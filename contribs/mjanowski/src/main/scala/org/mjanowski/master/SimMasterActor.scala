package org.mjanowski.master

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.matsim.api.core.v01.network.Network
import org.matsim.core.config.Config
import org.matsim.core.mobsim.qsim.MasterDelegate
import org.mjanowski.worker.{AssignNodes, Replanning, WorkerCommand}
import org.mjanowski.{NetworkPartitioner, Partition, worker}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala, SeqHasAsJava}

object SimMasterActor {

  val masterKey: ServiceKey[MasterCommand] = ServiceKey[MasterCommand]("master")

  private var partitions: mutable.Map[Integer, Partition] = _
  private var workersIds: mutable.Map[Integer, java.util.Collection[String]] = _
  private var connections: mutable.Map[Integer, java.util.Collection[Integer]] = _
  private val workers = mutable.Map[Int, ActorRef[WorkerCommand]]()
  private var workersNumber: Int = _
  private var masterDelegate: MasterDelegate = _
  private val logger = LoggerFactory.getLogger("debugFile")

  def apply(workersNumber: Int, network: Network, masterDelegate: MasterDelegate, config: Config): Behavior[MasterCommand] = {
    SimMasterActor.workersNumber = workersNumber
    SimMasterActor.masterDelegate = masterDelegate
    val partitioner = new NetworkPartitioner(network, config)
    val javaPartitions  = partitioner.partition(workersNumber)
    partitions = javaPartitions.asScala
    workersIds = partitioner.partitionsToWorkersIds(javaPartitions).asScala
    connections = partitioner.getWorkersConnections(javaPartitions).asScala

    Behaviors.setup(context => {
      context.system.receptionist ! Receptionist.register(masterKey, context.self)

      Behaviors.receiveMessage {
        case RegisterWorker(sender) =>
          println("witam!")
          workers += (workers.size -> sender)
          masterDelegate.workerRegistered()
          Behaviors.same

        case SendWorkerAssignments() =>
          val workersNodesIds = partitions.view.mapValues(_.getNodes.asScala)
            .map({ case (wid, nodes) => (Int.unbox(wid), nodes.map(_.getId.toString))})
            .toMap
          workersNodesIds.foreach({ case (wid, nodes) => {
            val batches = nodes.grouped(10000)
            batches.foreach(batch => workers.values.foreach(ref => ref ! AssignNodes(0, Map(wid -> batch.toSeq), null, null, false)))
          }})
          workers.map({ case (id, ref) => (ref, AssignNodes(id, null, workers, connections(id), true)) })
            .foreach({ case (ref, command) => ref ! command })
          Behaviors.same


        case Events(events, sender) =>
//          println(events.size)

          //todo just checking for debug

//          val times = events.map(e => e.getTime).distinct

//          if (events.map(e => e.getTime).distinct.size > 1)
//            Logger.getRootLogger.error("Multiple event times in a batch!!!" + times)


//          Logger.getRootLogger.info("Sender " + sender)
//          events.foreach(e => {Logger.getRootLogger.info(e)})
//            logger.debug(events.toString())
          SimMasterActor.masterDelegate.processBatch(events)
//          events.map(e => EventsMapper.map(e))
//            .foreach(e => {
////              Logger.getRootLogger.info(e + "\n " + sender)
//              eventsManager.processEvent(e)
//            })
          Behaviors.same

        case AfterMobsim() =>
          println("Received after mobsim")
          SimMasterActor.masterDelegate.afterMobsim()
          Behaviors.same

        case AfterSimStep(now) =>
//          Logger.getRootLogger.info("After simstep " + now)
          SimMasterActor.masterDelegate.workerAfterSimStep(now);
          Behaviors.same

        case SendReplanning(replanningDtos, last) =>
          workers.values.foreach(w => w ! Replanning(replanningDtos, last))
          Behaviors.same

        case TerminateSystem() =>
          workers.values.foreach(w => w ! worker.TerminateSystem())
          Behaviors.stopped

      }
    })
  }
}
