package org.mjanowski.master

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, LogOptions}
import com.typesafe.config.ConfigFactory
import org.matsim.api.core.v01.network.Network
import org.matsim.core.config.Config
import org.matsim.core.mobsim.qsim.MasterDelegate
import org.matsim.core.mobsim.qsim.qnetsimengine.ReplanningDto
import org.mjanowski.MySimConfig
import org.mjanowski.worker.SendAfterSimStep

import scala.jdk.CollectionConverters.ListHasAsScala

class MasterMain(mySimConfig: MySimConfig, network: Network, masterSim: MasterSim, masterDelegate: MasterDelegate, config: Config) {

  val host = mySimConfig.getMasterAddress
  val port = mySimConfig.getMasterPort
  val workersNumber = mySimConfig.getWorkersNumber
  val addressConfig =
    s"""akka.remote.artery.canonical.hostname=${host}
          akka.remote.artery.canonical.port=${port}
          akka.cluster.seed-nodes = ["akka://system@${host}:${port}"]"""
  val akkaConfig = ConfigFactory.parseString(addressConfig)
    .withFallback(ConfigFactory.load())
  val actorSystem = ActorSystem(
    SimMasterActor(workersNumber, network, masterDelegate, config),
    "system",
    akkaConfig)

//val actorSystem = ActorSystem(
//  Behaviors.logMessages(LogOptions().withLevel(Level.INFO), SimMasterActor(workersNumber, network, masterSim)),
//  "system",
//  akkaConfig)

  def sendReplanning(replanningDtos: java.util.List[ReplanningDto], last: Boolean) : Unit = {
    actorSystem ! SendReplanning(replanningDtos.asScala.toSeq, last)
  }

  def sendWorkerAssignments(): Unit = {
    actorSystem ! SendWorkerAssignments()
  }

  def terminateSystem(): Unit = {
    actorSystem.terminate()
  }

}
