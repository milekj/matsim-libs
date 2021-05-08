package org.mjanowski.worker;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.WorkerDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.AcceptedVehiclesDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.MoveVehicleDto;
import org.mjanowski.MySimConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkerDelegateImpl implements WorkerDelegate {

    private WorkerMain workerMain;
    private WorkerSim workerSim;
    private Scenario sc;
    private CountDownLatch initialized = new CountDownLatch(1);
    private CountDownLatch movingNodesFinishedLatch;
    private CountDownLatch readyForIteration = new CountDownLatch(0);
    private boolean finished = false;
    private AtomicBoolean allFinished = new AtomicBoolean(true);
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
    public List<AcceptedVehiclesDto> update(Integer workerId, List<MoveVehicleDto> moveVehicleDtos, double timeOfDay) {
        return workerMain.update(workerId, moveVehicleDtos, timeOfDay);
    }

    @Override
    public void accepted(Integer workerId, Map<Id<Node>, Collection<List<AcceptedVehiclesDto>>> accepted) {
        workerMain.accepted(workerId, accepted);
    }

    @Override
    public void beforeIteration() {
        if (movingNodesFinishedLatch == null) {
            movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
            initialized.countDown();
        }
    }

    @Override
    public void initializeForNextIteration() {
        finished = false;
        allFinished = new AtomicBoolean(true);
        canStartNextStep = null;
        movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
    }

    @Override
    public boolean mobsimFinished() {
        return !workerSim.getAgentCounter().isLiving() || workerSim.getStopTime() <= workerSim.getSimTimer().getTimeOfDay();
    }

    @Override
    public void sendFinished(boolean finished) {
        this.finished = finished;
        //todo potrzebne tutaj usprawnienie w przypadku gdy długo czekamy i nie ma poruszających się pojazdów?
//        Logger.getRootLogger().info("agents: " + workerSim.getAgentCounter().getLiving());
//        Logger.getRootLogger().info("stop time: " + workerSim.getStopTime());
//        Logger.getRootLogger().info("now: " + workerSim.getSimTimer().getTimeOfDay());
        allFinished = new AtomicBoolean(this.finished);
        canStartNextStep = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
        workerMain.sendFinished();
    }

    @Override
    public void movingNodesFinished() {
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
    public void readyForNextStep(boolean finished) {
        allFinished.compareAndSet(true, finished);
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
        movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerConnectionsNumber());
    }

    @Override
    public void sendReadyForNextStep() {
        workerMain.sendReadyForNextMoving(finished);
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
        return allFinished.get();
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

}
