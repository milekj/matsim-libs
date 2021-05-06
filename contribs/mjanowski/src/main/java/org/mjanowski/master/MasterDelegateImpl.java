package org.mjanowski.master;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.MasterDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.ReplanningDto;
import org.mjanowski.MySimConfig;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MasterDelegateImpl implements MasterDelegate {

    private MasterMain masterMain;
    private MasterSim masterSim;

    @Inject
    public MasterDelegateImpl(Scenario scenario, Mobsim mobsim) {
        MasterSim masterSim = (MasterSim) mobsim;
        MySimConfig mySimConfig = (MySimConfig) scenario.getConfig().getModules().get("mySimConfig");
        this.masterSim = masterSim;
        masterSim.setMasterDelegate(this);
        masterSim.getEventsManager().initProcessing();
        masterMain = new MasterMain(mySimConfig, scenario.getNetwork(), masterSim, this);
    }

    @Override
    public void afterMobsim() {
        masterSim.afterMobsim();
    }

    @Override
    public void workerAfterSimStep(double step) {
        masterSim.getEventsManager().workerAfterSimStep(step);
    }

    @Override
    public void processBatch(List<EventDto> eventsDtos) {
        masterSim.getEventsManager().processBatch(eventsDtos);
    }

    @Override
    public void sendReplanning(List<ReplanningDto> replanningDtos) {
        Lists.partition(replanningDtos, 100)
                .forEach(r -> masterMain.sendReplanning(r, false));
        masterMain.sendReplanning(Collections.emptyList(), true);
    }

    @Override
    public void terminateSystem() {
        System.out.println("terminateing actor system");
        masterMain.terminateSystem();
    }


}
