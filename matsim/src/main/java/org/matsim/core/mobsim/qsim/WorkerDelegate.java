package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.AcceptedVehiclesDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.DepartVehicleDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.MoveVehicleDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WorkerDelegate {

    void waitForUpdates();

    void startIteration();

    void terminateSystem();

    List<AcceptedVehiclesDto> update(Integer workerId, List<MoveVehicleDto> v, double timeOfDay);

    void accepted(Integer workerId, Map<Id<Node>, Collection<List<AcceptedVehiclesDto>>> accepted);

    void beforeIteration();

    void initializeForNextIteration();

    boolean mobsimFinished();

    void sendFinished(boolean finished);

    void movingNodesFinished(boolean workerFinished);

    void readyForNextStep(boolean finished);

    void initializeForNextStep();

    void sendReadyForNextStep();

    void waitUntilReadyForNextStep();

    boolean shouldFinish();

    void sendEvents(List<EventDto> eventDtos);

    void sendAfterMobsim();

    void sendAfterSimStep(double time);

    void readyForNextIteration();

    void waitUntilReadyForIteration();

    void checkIfShouldFinish();

    void sendVehicleDeparture(Integer toNodeWorkerId, DepartVehicleDto moveVehicleDto);
}
