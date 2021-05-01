package org.matsim.core.mobsim.qsim;

import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.ReplanningDto;

import java.util.List;

public interface MasterDelegate {

    void afterMobsim();

    void workerAfterSimStep(double step);

    void processBatch(List<EventDto> eventsDtos);

    void sendReplanning(List<ReplanningDto> replanningDtos);

    void terminateSystem();
}
