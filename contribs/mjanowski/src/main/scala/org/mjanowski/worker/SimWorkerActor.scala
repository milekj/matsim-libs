package org.mjanowski.worker

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.mjanowski.master._

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala, SeqHasAsJava}

object SimWorkerActor {

  var master: ActorRef[MasterCommand] = _
  var workerSim: WorkerSim = _
  var workerRefs: collection.Map[Int, ActorRef[WorkerCommand]] = _
  var workerConnections: Iterable[ActorRef[WorkerCommand]] = _

  var workerId: Int = _
  var canStartIteration: Boolean = false
  var assigned: Boolean = false

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

        case AssignNodes(workerId, workersNodesIds, workersRefs, workerConnections) =>
          SimWorkerActor.workerRefs = workersRefs
          println("hura")
          println(workerId, workersRefs, workerConnections)
          this.workerId = workerId
          workerSim.setWorkerId(workerId)
          workerSim.setPartitions(workersNodesIds)
          workerSim.setConnections(workerConnections)
          this.workerConnections = workerConnections.asScala.map(wid => workersRefs(wid))

          println("assigned = true")
          assigned = true
          if (canStartIteration)
            workerSim.runIteration()
          Behaviors.same

        case StartIteration() =>
          println("canStartIteration = true")
          println(Thread.currentThread().getName());
          SimWorkerActor.canStartIteration = true
          if (assigned)
            workerSim.runIteration()
          Behaviors.same

        case SendUpdate(workerId, moveVehicleDtos, replyTo) =>

          SimWorkerActor.workerRefs(workerId) ! Update(workerId, moveVehicleDtos, replyTo)
          Behaviors.same

        case m : Update =>
          //czy tutaj musi być aktor? może future?
          context.spawn(UpdateActor(workerSim), "update" + System.nanoTime()) ! m
          Behaviors.same

        case SendMovingNodesFinished() =>
          workerConnections.foreach(w => w ! MovingNodesFinished())

          Behaviors.same

        case MovingNodesFinished() =>
          workerSim.movingNodesFinished()
          Behaviors.same

        case SendReadyForNextStep(finished) =>
          workerConnections.foreach(w => w ! ReadyForNextStep(finished))
          Behaviors.same

        case ReadyForNextStep(finished) =>
          workerSim.readyForNextStep(finished)
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
//          println("Received replanning " + last)
          SimWorkerActor.workerSim.handleReplanning(replanningDtos.asJava, last)
          Behaviors.same


      }
    })
  }


}
