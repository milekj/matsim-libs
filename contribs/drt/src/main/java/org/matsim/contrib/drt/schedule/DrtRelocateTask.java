package org.matsim.contrib.drt.schedule;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;

public class DrtRelocateTask extends DriveTask {
	public static final DrtTaskType TYPE = new DrtTaskType(DrtTaskBaseType.RELOCATE);
	
	public DrtRelocateTask(VrpPathWithTravelData path) {
		super(TYPE, path);
	}

}
