/* *********************************************************************** *
 * project: org.matsim.*
 * QSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package org.mjanowski.worker;

import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.WorkerDelegate;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This has developed over the last couple of months/years towards an increasingly pluggable module.  The current (dec'2011)
 * approach consists of the following elements (and presumably more, developed by mzilske):<ul>
 * <li> QSim itself should have all basic functionality to execute a typical agent plan, i.e. activities and legs.  In this basic
 * version, all legs are teleported.
 * <li> In addition, there are "engines" that plug into QSim.  Those are time-step driven, as is QSim.  Many engines move
 * particles around, i.e. they execute the different modes.  Others are responsible for, e.g., time-variant networks or signals.
 * <li> A special engine is the netsim engine, which is the original "queue"
 * engine.  It is invoked by default, and it carries the "NetsimNetwork" for which there is a getter.
 * <li> Engines that move particles around need to be able to "end legs".
 * This used to be such that control went to the agents, which
 * reinserted themselves into QSim.  This has now been changed: The agents compute their next state, but the engines are
 * responsible for reinsertion into QSim.  For this, they obtain an "internal interface" during engine addition.  Naming
 * conventions will be adapted to this in the future.
 * <li> <i>A caveat is that drivers that move around other agents (such as TransitDriver, TaxicabDriver) need to become
 * "engines".</i>  Possibly, something that executes a leg is not really the same as an "engine", but this is what we have
 * for the time being.
 * <li> Engines that offer new modes also need to be registered as "DepartureHandler"s.
 *  * </ul>
 * Future plans include: pull the agent counter write methods back into QSim (no big deal, I hope); pull the actstart/end,
 * agent departure/arrival back into QSim+engines; somewhat separate the teleportation engine and the activities engine from the
 * framework part of QSim.
 * <p></p>
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public final class WorkerSim extends Thread implements VisMobsim, Netsim, ActivityEndRescheduler {

	final private static Logger log = Logger.getLogger(WorkerSim.class);
	private QSim qSim;
	private Population population;
	private WorkerDelegate workerDelegate;
	private Scenario scenario;

	private int workerId;
	private Map<Integer, Set<Id<Node>>> workerNodesIds;
	private Collection<Integer> workersConnections;

	@Inject
	private WorkerSim(final Scenario sc,
					  EventsManager events,
					  ControlerListenerManager controlerListenerManager,
					  Population population) {
		scenario = sc;
		this.population = population;
		controlerListenerManager.addControlerListener(
				(AfterMobsimListener) event -> {
					workerDelegate.initializeForNextIteration();
					workerDelegate.sendAfterMobsim();
				}
		);
		controlerListenerManager.addControlerListener(
				(ShutdownListener) event -> {
					workerDelegate.terminateSystem();
				}
		);
	}

	public void setQsim(QSim qsim) {
		this.qSim = qsim;
		this.qSim.setWorkerDelegate(workerDelegate);
	}

	public void setWorkerDelegate(WorkerDelegate workerDelegate) {
		this.workerDelegate = workerDelegate;
	}

	public void setWorkerId(int workerId) {
		this.workerId = workerId;
	}

	public void setPartitions(Map<Integer, Collection<String>> workerNodesStringIds) {
		this.workerNodesIds = workerNodesStringIds.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().map((Function<String, Id<Node>>) Id::createNodeId).collect(Collectors.toSet())));
	}

	public void setConnections(Collection<Integer> workersConnections) {
		this.workersConnections = workersConnections;
	}

	public Map<Integer, Set<Id<Node>>> getWorkerNodesIds() {
		return workerNodesIds;
	}

	public Collection<Integer> getWorkersConnections() {
		return workersConnections;
	}

	public int getWorkerConnectionsNumber() {
		return workersConnections.size();
	}

	public void runIteration() {
		qSim.setWorkerNodesIds(workerNodesIds);
		qSim.setWorkerId(workerId);
		workerDelegate.beforeIteration();
		new Thread(() -> {
			qSim.run();
			synchronized (this) {
				this.notify();
			}
		}).start();
	}

	@Override
	public void run() {
		workerDelegate.waitUntilReadyForIteration();
		workerDelegate.startIteration();
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//todo to gdzieś indziej...!!!!
		//		workerDelegate.terminateSystem();
	}

	public List<AcceptedVehiclesDto> acceptVehicles(int workerId, List<MoveVehicleDto> moveVehicleDtos) {
		//todo podział na wątki itd....
		return qSim.acceptVehicles(workerId, moveVehicleDtos);
	}

	public void movingNodesFinished() {
		workerDelegate.movingNodesFinished();
	}

	public void readyForNextStep(boolean finished) {
		workerDelegate.readyForNextStep(finished);
	}

	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		qSim.addQueueSimulationListeners(listener);
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		qSim.rescheduleActivityEnd(agent);
	}

	@Override
	public NetsimNetwork getNetsimNetwork() {
		return qSim.getNetsimNetwork();
	}

	@Override
	public EventsManager getEventsManager() {
		return qSim.getEventsManager();
	}

	@Override
	public AgentCounter getAgentCounter() {
		return qSim.getAgentCounter();
	}

	@Override
	public double getStopTime() {
		return qSim.getStopTime();
	}

	@Override
	public Scenario getScenario() {
		return qSim.getScenario();
	}

	@Override
	public MobsimTimer getSimTimer() {
		return qSim.getSimTimer();
	}

	@Override
	public VisNetwork getVisNetwork() {
		return qSim.getVisNetwork();
	}

	@Override
	public Map<Id<Person>, MobsimAgent> getAgents() {
		return qSim.getAgents();
	}

	@Override
	public VisData getNonNetworkAgentSnapshots() {
		return qSim.getNonNetworkAgentSnapshots();
	}

	public void handleReplanning(List<ReplanningDto> replanningDtos, boolean last) {
		Map<Id<Person>, ? extends Person> persons = population.getPersons();

		for (ReplanningDto replanningDto : replanningDtos) {
			Id<Person> personId = replanningDto.getPersonId();
			Person person = persons.get(personId);

			replanningDto.getPlanId().ifPresentOrElse(
					person::setSelectedPlan,
					() -> {
						PlanDto planDto = replanningDto.getNewPlan().get();
						Plan plan = PopulationUtils.createPlan();
						plan.setPerson(person);
						plan.setType(planDto.getType());
						plan.setScore(planDto.getScore());
						ArrayList<PlanElement> planElements = planDto.getActsLegs().stream()
								.map(PlanElementDto::toPlanElement)
								.collect(Collectors.toCollection(ArrayList::new));
						plan.setPlanElements(planElements);
						person.addPlan(plan);
						person.setSelectedPlan(plan);
					}
			);
		}
		if (last) {
			workerDelegate.readyForNextIteration();
		}
	}
}
