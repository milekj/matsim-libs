package org.mjanowski.worker;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.WorkerDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.AcceptedVehiclesDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.DepartVehicleDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.MoveVehicleDto;
import org.mjanowski.MySimConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class WorkerDelegateImpl implements WorkerDelegate {

    private WorkerMain workerMain;
    private WorkerSim workerSim;
    private Scenario sc;
    private CountDownLatch initialized = new CountDownLatch(1);
    private CountDownLatch movingNodesFinishedLatch;
    private CountDownLatch readyForIteration = new CountDownLatch(0);
    private boolean readyToFinish = false;
    private int neighboursReadyToFinishCount = 0;
    private boolean readyToFinishWithNeighbours = false;
    private int neighboursReadyToFinishWithNeighboursCount = 0;
    private boolean finished = false;
    private CountDownLatch canStartNextStep;

    @Inject
    public WorkerDelegateImpl(Scenario scenario, WorkerSim workerSim) {
        this.workerSim = workerSim;
        MySimConfig mySimConfig = (MySimConfig) scenario.getConfig().getModules().get("mySimConfig");
        this.workerMain = new WorkerMain(mySimConfig, workerSim);
        workerSim.setWorkerDelegate(this);
    }

    @Override
    public void startIteration() {
        System.out.println("init for next");
        readyForIteration = new CountDownLatch(1);
        workerMain.startIteration();
    }

    @Override
    public void terminateSystem() {
        System.out.println("terminateing actor system");
        workerMain.terminateSystem();
    }

    @Override
    public List<AcceptedVehiclesDto> update(Integer workerId, List<MoveVehicleDto> moveVehicleDtos, boolean stuck) {
        return workerMain.update(workerId, moveVehicleDtos, stuck);
    }

    @Override
    public void beforeIteration() {
        if (movingNodesFinishedLatch == null) {
            movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
            initialized.countDown();
        }
        neighboursReadyToFinishCount = 0;
        neighboursReadyToFinishWithNeighboursCount = 0;
    }

    @Override
    public void initializeForNextIteration() {
        readyToFinish = false;
        readyToFinishWithNeighbours = false;
        finished = false;
        canStartNextStep = null;
        movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
        neighboursReadyToFinishCount = 0;
        neighboursReadyToFinishWithNeighboursCount = 0;
    }

    @Override
    public boolean mobsimFinished() {
        return !workerSim.getAgentCounter().isLiving() || workerSim.getStopTime() <= workerSim.getSimTimer().getTimeOfDay();
    }

    @Override
    public void sendFinished(boolean readyToFinish) {
        this.readyToFinish = readyToFinish;
        canStartNextStep = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
        workerMain.sendFinishedMovingNodes(readyToFinish);
    }

    @Override
    public void movingNodesFinished(boolean workerFinished) {
        if (workerFinished)
            neighboursReadyToFinishCount++;
        if (initialized.getCount() == 0)
            movingNodesFinishedLatch.countDown();
        else {
            new Thread(() -> {
                try {
                    initialized.await();
                    movingNodesFinishedLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void readyForNextStep(boolean readyToFinishWithNeighbours) {
        if (readyToFinishWithNeighbours)
            neighboursReadyToFinishWithNeighboursCount++;
        canStartNextStep.countDown();
    }

    @Override
    public void waitForUpdates() {
        try {
//            Logger.getRootLogger().info("start waiting for updates");
            movingNodesFinishedLatch.await();
//            Logger.getRootLogger().info("finished waiting for updates");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initializeForNextStep() {
        readyToFinishWithNeighbours = readyToFinish && neighboursReadyToFinishCount == workerSim.getWorkerConnectionsNumber();
        neighboursReadyToFinishCount = 0;
        movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
    }

    @Override
    public void sendReadyForNextStep() {
        workerMain.sendReadyForNextMoving(readyToFinishWithNeighbours);
    }

    @Override
    public void waitUntilReadyForNextStep() {
        try {
            canStartNextStep.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldFinish() {
        return finished;
    }

    @Override
    public void sendEvents(List<EventDto> eventDtos) {
        workerMain.sendEvents(eventDtos);
    }

    @Override
    public void sendAfterMobsim() {
        workerMain.sendAfterMobsim();
    }

    @Override
    public void sendAfterSimStep(double time) {
        workerMain.sendAfterSimStep(time);
    }

    @Override
    public void readyForNextIteration() {
        System.out.println("ready for iteration count down");
        readyForIteration.countDown();
    }

    @Override
    public void waitUntilReadyForIteration() {
        try {
            readyForIteration.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void checkIfShouldFinish() {
        this.finished = readyToFinish && neighboursReadyToFinishWithNeighboursCount == workerSim.getWorkerConnectionsNumber();
        neighboursReadyToFinishWithNeighboursCount = 0;
    }
    
    @Override
    public void sendVehicleDeparture(Integer toNodeWorkerId, DepartVehicleDto departVehicleDto) {
        workerMain.sendVehicleDeparture(toNodeWorkerId, departVehicleDto);
    }

}
