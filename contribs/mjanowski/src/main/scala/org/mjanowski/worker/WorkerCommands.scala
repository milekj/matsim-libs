package org.mjanowski.worker

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import org.matsim.core.mobsim.qsim.qnetsimengine.{AcceptedVehiclesDto, DepartVehicleDto, EventDto, MoveVehicleDto, ReplanningDto}
import org.mjanowski.master.MySerializable
import java.util

import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.Node

import scala.collection.mutable

sealed trait WorkerCommand extends MySerializable

case class ListingResponse(listing: Receptionist.Listing) extends WorkerCommand


//TOOD seq -> iterable???

case class AssignNodes(workerId: Integer,
                        @JsonDeserialize(keyAs = classOf[Integer]) workersNodesIds: Map[Int, Seq[String]],
                        @JsonDeserialize(keyAs = classOf[Integer]) workersRefs: mutable.Map[Int, ActorRef[WorkerCommand]],
                        @JsonDeserialize(keyAs = classOf[Integer]) workersConnections: util.Collection[Integer],
                        last: Boolean) extends WorkerCommand

case class StartIteration() extends WorkerCommand

case class SendUpdate(workerId: Int, seq: java.util.List[MoveVehicleDto], stuck: Boolean, replyTo: ActorRef[WorkerCommand]) extends WorkerCommand

case class Update(workerId: Int, moveVehicleDtos: java.util.List[MoveVehicleDto], stuck: Boolean, replyTo: ActorRef[WorkerCommand]) extends WorkerCommand

case class SendVehicleDeparture(workerId: Int, departVehicleDto: DepartVehicleDto) extends WorkerCommand

case class VehicleDeparture(departVehicleDto: DepartVehicleDto) extends WorkerCommand

case class Accepted(accepted: util.List[AcceptedVehiclesDto]) extends WorkerCommand

case class SendMovingNodesFinished(readyToFinish: Boolean) extends WorkerCommand

case class MovingNodesFinished(readyToFinish: Boolean) extends WorkerCommand

case class SendReadyForNextStep(readyToFinishWithNeighbours: Boolean) extends WorkerCommand

case class ReadyForNextStep(readyToFinishWithNeighbours: Boolean) extends WorkerCommand

case class SendEvents(events: java.util.List[EventDto]) extends WorkerCommand

case class SendAfterMobsim() extends WorkerCommand

case class SendAfterSimStep(now: Double) extends WorkerCommand

case class Replanning(replanningDtos: java.util.List[ReplanningDto], last: Boolean) extends WorkerCommand

case class TerminateSystem() extends WorkerCommand