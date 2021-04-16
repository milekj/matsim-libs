package org.mjanowski.master

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.apache.log4j.Logger
import org.matsim.api.core.v01.network.Network
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.mobsim.qsim.qnetsimengine.EventsMapper
import org.mjanowski.worker.{AssignNodes, WorkerCommand}
import org.mjanowski.{NetworkPartitioner, Partition}

import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala, SeqHasAsJava}

object SimMasterActor {

  val masterKey: ServiceKey[MasterCommand] = ServiceKey[MasterCommand]("master")
  private var partitions: mutable.Map[Int, Partition] = _
  private val workers = mutable.Map[Int, ActorRef[WorkerCommand]]()
  private var workersNumber: Int = _
  private var eventsManager: MasterEventsManager = _

  def apply(workersNumber: Int, network: Network, masterSim: MasterSim): Behavior[MasterCommand] = {
    SimMasterActor.workersNumber = workersNumber
    SimMasterActor.eventsManager = masterSim.getEventsManager
    val partitioner = new NetworkPartitioner(network)
    partitions = partitioner.partition(workersNumber)
      .asScala
      .map({ case (id, p) => (Option(id).map(Int.unbox).get, p) })

    Behaviors.setup(context => {
      context.system.receptionist ! Receptionist.register(masterKey, context.self)

      Behaviors.receiveMessage {
        case RegisterWorker(sender) =>
          println("witam!")
          workers += (workers.size -> sender)
          if (workers.size == workersNumber) {
            val workersNodesIds = partitions.view.mapValues(_.getNodes.asScala)
              .mapValues(l => l.map(_.getId.toString).toSeq)
              .toMap
            workers.map({ case (id, ref) => (ref, AssignNodes(id, workersNodesIds, workers)) })
              .foreach({ case (ref, command) => ref ! command })
          }
          Behaviors.same

        case Events(events, sender) =>
//          println(events.size)

          //todo just checking for debug

          val times = events.map(e => e.getTime).distinct

          if (events.map(e => e.getTime).distinct.size > 1)
            Logger.getRootLogger.error("Multiple event times in a batch!!!" + times)


//          Logger.getRootLogger.info("Sender " + sender)
//          events.foreach(e => {Logger.getRootLogger.info(e)})
          eventsManager.processBatch(events.asJava)
//          events.map(e => EventsMapper.map(e))
//            .foreach(e => {
////              Logger.getRootLogger.info(e + "\n " + sender)
//              eventsManager.processEvent(e)
//            })
          Behaviors.same

        case FinishEventsProcessing() =>
          eventsManager.finishProcessing();
          Behaviors.same

        case AfterMobsim() =>
          masterSim.afterMobsim()
          Behaviors.same

        case AfterSimStep(now) =>
//          Logger.getRootLogger.info("After simstep " + now)
          eventsManager.workerAfterSimStep(now);
          Behaviors.same

      }
    })
  }
}
