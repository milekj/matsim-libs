/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtRelocateTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ExtensiveInsertionSearch implements DrtInsertionSearch<PathData> {
	private final ExtensiveInsertionSearchParams insertionParams;

	// step 1: initial filtering out feasible insertions
	private final InsertionCostCalculator<Double> admissibleCostCalculator;
	private final DetourTimesProvider admissibleDetourTimesProvider;

	// step 2: finding best insertion
	private final ForkJoinPool forkJoinPool;
	private final DetourPathCalculator detourPathCalculator;
	private final BestInsertionFinder<PathData> bestInsertionFinder;

	// Zonal based matching related parameters
	private final DrtZonalSystem zonalSystem;

	public ExtensiveInsertionSearch(DetourPathCalculator detourPathCalculator, DrtConfigGroup drtCfg, MobsimTimer timer,
			ForkJoinPool forkJoinPool, InsertionCostCalculator.PenaltyCalculator penaltyCalculator,
			DrtZonalSystem zonalSystem) {
		this.detourPathCalculator = detourPathCalculator;
		this.forkJoinPool = forkJoinPool;

		insertionParams = (ExtensiveInsertionSearchParams) drtCfg.getDrtInsertionSearchParams();
		admissibleCostCalculator = new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, Double::doubleValue);

		// TODO use more sophisticated DetourTimeEstimator
		double admissibleBeelineSpeed = insertionParams.getAdmissibleBeelineSpeedFactor()
				* drtCfg.getEstimatedDrtSpeed() / drtCfg.getEstimatedBeelineDistanceFactor();

		admissibleDetourTimesProvider = new DetourTimesProvider(
				DetourTimeEstimator.createNodeToNodeBeelineTimeEstimator(admissibleBeelineSpeed));

		bestInsertionFinder = new BestInsertionFinder<>(
				new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, PathData::getTravelTime));
		this.zonalSystem = zonalSystem;
	}

	@Override
	public Optional<InsertionWithDetourData<PathData>> findBestInsertion(DrtRequest drtRequest,
			Collection<Entry> vEntries) {
		InsertionGenerator insertionGenerator = new InsertionGenerator();
		DetourData<Double> admissibleTimeData = admissibleDetourTimesProvider.getDetourData(drtRequest);
		KNearestInsertionsAtEndFilter kNearestInsertionsAtEndFilter = new KNearestInsertionsAtEndFilter(
				insertionParams.getNearestInsertionsAtEndLimit(), insertionParams.getAdmissibleBeelineSpeedFactor());

		// If zone based matching is turned on, we need to filter out some of the
		// vehicles when exploring insertion
		Collection<Entry> filteredVEntries = new ArrayList<>();
		if (insertionParams.getZoneBasedMacthing()) {
			for (Entry entry : vEntries) {
				Task currentTask = entry.vehicle.getSchedule().getCurrentTask();
				if (currentTask instanceof DrtStayTask) {
					// For idling vehicle, we will consider it, if it is in the same zone of the
					// request
					Link currentLink = ((DrtStayTask) currentTask).getLink();
					if (CheckIfTwoLinksInSameZone(currentLink, drtRequest.getFromLink())) {
						filteredVEntries.add(entry);
					}
				} else if (currentTask instanceof DrtRelocateTask) {
					// For rebalancing vehicle, we will consider it, if it is rebalancing to the
					// same zone of the request
					Link destinationLink = ((DrtRelocateTask) currentTask).getPath().getToLink();
					if (CheckIfTwoLinksInSameZone(destinationLink, drtRequest.getFromLink())) {
						filteredVEntries.add(entry);
					}

				} else {
					// For other vehicle (drive with passenger, pick up drive, picking up and
					// dropping off), we will always consider (as shared ride)
					filteredVEntries.add(entry);
				}
			}
		} else {
			filteredVEntries.addAll(vEntries);
		}

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is
		// sequential.
		List<Insertion> filteredInsertions = forkJoinPool.submit(() -> filteredVEntries.parallelStream()
				// generate feasible insertions (wrt occupancy limits)
				.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
				// map insertions to insertions with admissible detour times (i.e. admissible
				// beeline speed factor)
				.map(admissibleTimeData::createInsertionWithDetourData)
				// optimistic pre-filtering wrt admissible cost function
				.filter(insertion -> admissibleCostCalculator.calculate(drtRequest,
						insertion) < InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)
				// skip insertions at schedule ends (a subset of most promising
				// "insertionsAtEnd" will be added later)
				.filter(kNearestInsertionsAtEndFilter::filter)
				// forget (admissible) detour times
				.map(InsertionWithDetourData::getInsertion).collect(Collectors.toList())).join();
		filteredInsertions.addAll(kNearestInsertionsAtEndFilter.getNearestInsertionsAtEnd());

		DetourData<PathData> pathData = detourPathCalculator.calculatePaths(drtRequest, filteredInsertions);
		// TODO could use a parallel stream within forkJoinPool, however the idea is to
		// have as few filteredInsertions
		// as possible, and then using a parallel stream does not make sense.
		return bestInsertionFinder.findBestInsertion(drtRequest,
				filteredInsertions.stream().map(pathData::createInsertionWithDetourData));
	}

	private boolean CheckIfTwoLinksInSameZone(Link link1, Link link2) {
		if (zonalSystem.getZoneForLinkId(link1.getId()).equals(zonalSystem.getZoneForLinkId(link2.getId()))) {
			return true;
		}
		return false;
	}
}
