package org.mjanowski.worker

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import org.matsim.core.mobsim.qsim.qnetsimengine.{AcceptedVehiclesDto, EventDto, MoveVehicleDto, ReplanningDto}
import org.mjanowski.master.MySerializable

import java.util

sealed trait WorkerCommand extends MySerializable

case class ListingResponse(listing: Receptionist.Listing) extends WorkerCommand


//TOOD seq -> iterable???

case class AssignNodes(workerId: Integer,
                        @JsonDeserialize(keyAs = classOf[Integer]) workersNodesIds: java.util.Map[Integer, util.Collection[String]],
                        @JsonDeserialize(keyAs = classOf[Integer]) workersRefs: Map[Int, ActorRef[WorkerCommand]],
                        @JsonDeserialize(keyAs = classOf[Integer]) workersConnections: util.Collection[Integer]) extends WorkerCommand

case class StartIteration() extends WorkerCommand

case class SendUpdate(workerId: Int, seq: Seq[MoveVehicleDto], replyTo: ActorRef[WorkerCommand]) extends WorkerCommand

case class Update(workerId: Int, moveVehicleDtos: Seq[MoveVehicleDto], replyTo: ActorRef[WorkerCommand]) extends WorkerCommand

case class SendAccepted(workerId: Int, accepted: Map[String, util.Collection[util.List[AcceptedVehiclesDto]]]) extends WorkerCommand

case class Accepted(accepted: util.List[AcceptedVehiclesDto]) extends WorkerCommand

case class SendMovingNodesFinished() extends WorkerCommand

case class MovingNodesFinished() extends WorkerCommand

case class SendReadyForNextStep(finished: Boolean) extends WorkerCommand

case class ReadyForNextStep(finished: Boolean) extends WorkerCommand

case class SendEvents(events: Seq[EventDto]) extends WorkerCommand

case class SendAfterMobsim() extends WorkerCommand

case class SendAfterSimStep(now: Double) extends WorkerCommand

case class Replanning(replanningDtos: Seq[ReplanningDto], last: Boolean) extends WorkerCommand