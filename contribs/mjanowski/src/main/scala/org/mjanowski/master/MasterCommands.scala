package org.mjanowski.master

import akka.actor.typed.ActorRef
import org.matsim.core.mobsim.qsim.qnetsimengine.{EventDto, ReplanningDto}
import org.mjanowski.worker.WorkerCommand

trait MySerializable
sealed trait MasterCommand extends MySerializable

case class RegisterWorker(replyTo: ActorRef[WorkerCommand]) extends MasterCommand

case class Events(events: java.util.List[EventDto], sender: ActorRef[WorkerCommand]) extends MasterCommand

case class AfterMobsim() extends MasterCommand

case class AfterSimStep(now: Double) extends MasterCommand

case class SendReplanning(replanningDtos: java.util.List[ReplanningDto], last: Boolean) extends MasterCommand

case class SendWorkerAssignments() extends MasterCommand

case class TerminateSystem() extends MasterCommand