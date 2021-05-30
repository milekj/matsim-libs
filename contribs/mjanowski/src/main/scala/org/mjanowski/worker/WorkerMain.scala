package org.mjanowski.worker

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.Node
import org.matsim.core.mobsim.qsim.qnetsimengine.{AcceptedVehiclesDto, DepartVehicleDto, EventDto, MoveVehicleDto}
import org.mjanowski.MySimConfig
import java.util

import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.reflect.ClassTag

class WorkerMain(config: MySimConfig, workerSim: WorkerSim) {

  private val masterHost = config.getMasterAddress
  private val masterPort = config.getMasterPort
  private val workerHost = config.getWorkerAddress
  private val addressConfig =
    s"""akka.remote.artery.canonical.hostname=${workerHost}
          akka.remote.artery.canonical.port=0
          akka.cluster.seed-nodes = ["akka://system@${masterHost}:${masterPort}"]"""
  private val akkaConfig = ConfigFactory.parseString(addressConfig)
    .withFallback(ConfigFactory.load())
//  private val actorSystem = ActorSystem(Behaviors.logMessages(SimWorkerActor(workerSim)), "system", akkaConfig)
  private val actorSystem = ActorSystem(SimWorkerActor(workerSim), "system", akkaConfig)
  implicit val timeout: Timeout = 10.minutes
  implicit val scheduler = actorSystem.scheduler

  //  private val system2 = ActorSystem(DeadLetterActor(), "deadletter")
//  actorSystem.eventStream ! EventStream.Subscribe(system2)

  def startIteration(): Unit = {
    actorSystem ! StartIteration()
  }

  def update(workerId: Int, moveVehicleDtos: java.util.List[MoveVehicleDto], stuck: Boolean) = {
    //todo czy tutaj trzeba po jednym aktorze na każdy wątek?
    //czy one są w stanie przetwarzać te future'y równolegle?
    //może trzeba coś w konfiguracji dispachera akki
    val future: Future[Accepted] = actorSystem.ask(ref => SendUpdate(workerId, moveVehicleDtos, stuck, ref))
      .mapTo[Accepted]
    Await.ready(future, Duration.Inf)
    future.value.get.get.accepted
  }

  def sendVehicleDeparture(toNodeWorkerId: Int, departVehicleDto: DepartVehicleDto) = {
    actorSystem ! SendVehicleDeparture(toNodeWorkerId, departVehicleDto)
  }

  def sendFinishedMovingNodes(readyToFinish: Boolean): Unit = {
    actorSystem ! SendMovingNodesFinished(readyToFinish)
  }

  def sendReadyForNextMoving(readyToFinishWithNeighbours: Boolean): Unit = {
    actorSystem ! SendReadyForNextStep(readyToFinishWithNeighbours)
  }

  def terminateSystem(): Unit = {
    actorSystem.terminate()
  }

  def sendEvents(events : java.util.List[EventDto]): Unit = {
    actorSystem ! SendEvents(events)
  }

  def sendAfterMobsim(): Unit = {
    actorSystem ! SendAfterMobsim()
  }

  def sendAfterSimStep(now: Double): Unit = {
    actorSystem ! SendAfterSimStep(now)
  }

}
