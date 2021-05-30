package org.mjanowski.worker

import java.util

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.mjanowski.master
import org.mjanowski.master.{AfterMobsim, AfterSimStep, Events, MasterCommand, RegisterWorker, SimMasterActor}
import org.mjanowski.worker

import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsJava, SetHasAsJava}

object SimWorkerActor {

  var master: ActorRef[MasterCommand] = _
  var workerSim: WorkerSim = _
  var workerRefs: collection.Map[Int, ActorRef[WorkerCommand]] = _
  var workerConnections: Iterable[ActorRef[WorkerCommand]] = _

  var workerId: Int = _
  var canStartIteration: Boolean = false
  var assigned: Boolean = false
  val workerNodesIds: mutable.MultiDict[Int, String] = mutable.MultiDict()

  def apply(workerSim: WorkerSim): Behavior[WorkerCommand] = {
    SimWorkerActor.workerSim = workerSim

    Behaviors.setup(context => {
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse)
      context.system.receptionist ! Receptionist.Subscribe(SimMasterActor.masterKey, listingResponseAdapter)

      Behaviors.receiveMessage {

        case ListingResponse(SimMasterActor.masterKey.Listing(listings)) =>
          listings.headOption.foreach(r => {
            master = r
            r ! RegisterWorker(context.self)
          })
          Behaviors.same

        case AssignNodes(workerId, workersNodesIds, workersRefs, workerConnections, last) =>
//          println(workerId, workersNodesIds, workersRefs)

          if (last) {
            SimWorkerActor.workerRefs = workersRefs
            val javaWorkersNodesId: Map[Integer, util.Collection[String]] =
              SimWorkerActor.workerNodesIds.sets.map({ case (k, v) => (Integer.valueOf(k), v.asJava) })
                .toMap
            println("hura")
            this.workerId = workerId
            workerSim.setWorkerId(workerId)
            workerSim.setPartitions(javaWorkersNodesId.asJava)
            workerSim.setConnections(workerConnections)
            this.workerConnections = workerConnections.asScala.map(wid => workersRefs(wid))

            println("assigned = true")
            println(Thread.currentThread().getName());
            assigned = true
            if (canStartIteration)
              workerSim.runIteration()
          } else {
            workersNodesIds.foreach({
              case (wid, nodes) =>
                nodes.foreach(n => SimWorkerActor.workerNodesIds.addOne((wid, n)))
            })
          }
          Behaviors.same

        //          SimWorkerActor.workerRefs = workersRefs
//          println("hura")
//          println(workerId, workersRefs, workerConnections)
//          this.workerId = workerId
//          workerSim.setWorkerId(workerId)
//          workerSim.setPartitions(workersNodesIds)
//          workerSim.setConnections(workerConnections)
//          this.workerConnections = workerConnections.asScala.map(wid => workersRefs(wid))
//
//          println("assigned = true")
//          assigned = true
//          if (canStartIteration)
//            workerSim.runIteration()
//          Behaviors.same

        case StartIteration() =>
          println("canStartIteration = true")
          println(Thread.currentThread().getName());
          SimWorkerActor.canStartIteration = true
          if (assigned)
            workerSim.runIteration()
          Behaviors.same

        case SendUpdate(workerId, moveVehicleDtos, stuck, replyTo) =>

          SimWorkerActor.workerRefs(workerId) ! Update(workerId, moveVehicleDtos, stuck, replyTo)
          Behaviors.same

        case m : Update =>
          //czy tutaj musi być aktor? może future?
          context.spawn(UpdateActor(workerSim), "update" + System.nanoTime()) ! m
          Behaviors.same

        case SendVehicleDeparture(workerId, departVehicleDto) =>

          SimWorkerActor.workerRefs(workerId) ! VehicleDeparture(departVehicleDto)
          Behaviors.same

        case VehicleDeparture(departVehicleDto) =>
          workerSim.handleVehicleDeparture(departVehicleDto);
          Behaviors.same

        case SendMovingNodesFinished(readyToFinish) =>
          workerConnections.foreach(w => w ! MovingNodesFinished(readyToFinish))

          Behaviors.same

        case MovingNodesFinished(readyToFinish) =>
          workerSim.movingNodesFinished(readyToFinish)
          Behaviors.same

        case SendReadyForNextStep(readyToFinishWithNeighbours) =>
          workerConnections.foreach(w => w ! ReadyForNextStep(readyToFinishWithNeighbours))
          Behaviors.same

        case ReadyForNextStep(readyToFinishWithNeighbours) =>
          workerSim.readyForNextStep(readyToFinishWithNeighbours)
          Behaviors.same

        case SendEvents(events) =>
          master ! Events(events, context.self)
          Behaviors.same

        case SendAfterMobsim() =>
          println("send after mobsim")
          master ! AfterMobsim()
          Behaviors.same

        case SendAfterSimStep(now) =>
          master ! AfterSimStep(now)
          Behaviors.same

        case Replanning(replanningDtos, last) =>
//          Logger.getRootLogger.info("Received replanning " + last)
          SimWorkerActor.workerSim.handleReplanning(replanningDtos, last)
          Behaviors.same

        case TerminateSystem() =>
          context.system.terminate()
          Behaviors.stopped


      }
    })
  }


}
