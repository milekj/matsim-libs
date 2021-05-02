package org.mjanowski.master

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.apache.log4j.Logger
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.{Network, Node}
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.mobsim.qsim.MasterDelegate
import org.matsim.core.mobsim.qsim.qnetsimengine.EventsMapper
import org.mjanowski.worker.{AssignNodes, Replanning, WorkerCommand}
import org.mjanowski.{NetworkPartitioner, Partition}

import java.util.AbstractMap.SimpleEntry
import java.util.stream.Collectors
import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala, SeqHasAsJava}

object SimMasterActor {

  val masterKey: ServiceKey[MasterCommand] = ServiceKey[MasterCommand]("master")
  private var partitions: java.util.Map[Integer, Partition] = _
  private var workersIds: java.util.Map[Integer, java.util.Collection[String]] = _
  private var connections: java.util.Map[Integer, java.util.Collection[Integer]] = _
  private val workers = mutable.Map[Int, ActorRef[WorkerCommand]]()
  private var workersNumber: Int = _
  private var masterDelegate: MasterDelegate = _

  def apply(workersNumber: Int, network: Network, masterDelegate: MasterDelegate): Behavior[MasterCommand] = {
    SimMasterActor.workersNumber = workersNumber
    SimMasterActor.masterDelegate = masterDelegate
    val partitioner = new NetworkPartitioner(network)
    partitions = partitioner.partition(workersNumber)
    workersIds = partitioner.partitionsToWorkersIds(partitions)
    connections = partitioner.getWorkersConnections(partitions)

    Behaviors.setup(context => {
      context.system.receptionist ! Receptionist.register(masterKey, context.self)

      Behaviors.receiveMessage {
        case RegisterWorker(sender) =>
          println("witam!")
          workers += (workers.size -> sender)
          if (workers.size == workersNumber) {
            workers.map({ case (id, ref) => (ref, AssignNodes(id,
              workersIds,
              workers.toMap,
              connections.get(id))) })
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
          SimMasterActor.masterDelegate.processBatch(events.asJava)
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

      }
    })
  }
}
