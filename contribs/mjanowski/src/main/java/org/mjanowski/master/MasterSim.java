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

package org.mjanowski.master;

import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.MasterDelegate;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.mjanowski.MySimConfig;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CountDownLatch;

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
public final class MasterSim implements Netsim {

	final private static Logger log = Logger.getLogger(MasterSim.class);

	private final EventsManager events;
	private ControlerListenerManager controlerListenerManager;
	private final MobsimTimer simTimer;
	private final MasterSimListenerManager listenerManager;
	private final Scenario scenario;
	private MasterDelegate masterDelegate;
	private CountDownLatch iterationEndedLatch;

	/**
	 * Constructs an instance of this simulation which does not do anything by itself, but accepts handlers for Activities and Legs.
	 * Use this constructor if you want to plug together your very own simulation, i.e. you are writing some of the simulation
	 * logic yourself.
	 *
	 * If you wish to use QSim as a product and run a simulation based on a Config file, rather use QSimFactory as your entry point.
	 *
	 */
	@Inject
	private MasterSim(final Scenario sc,
					  EventsManager events,
					  Injector childInjector,
					  ControlerListenerManager controlerListenerManager) {
		this.scenario = sc;
		this.events = events;
		this.controlerListenerManager = controlerListenerManager;
		this.listenerManager = new MasterSimListenerManager(this);
		this.simTimer = new MobsimTimer( sc.getConfig().qsim().getTimeStepSize());
		controlerListenerManager.addControlerListener(
				(ShutdownListener) event -> {
					masterDelegate.terminateSystem();
					events.finishProcessing();
				}
		);
		controlerListenerManager.addControlerListener(
				(ReplanningListener) event -> {
					events.initProcessing();
				}
		);
	}

	@Override
	public AgentCounter getAgentCounter() {
		return null;
	}

	// ============================================================================================================================
	// "run" method:

	public void run() {
		System.out.println("mastersim run");
//		getEventsManager().initProcessing();
		//???
		MySimConfig mySimConfig = (MySimConfig) scenario.getConfig().getModules().get("mySimConfig");
		iterationEndedLatch = new CountDownLatch(mySimConfig.getWorkersNumber());
		try {
			iterationEndedLatch.await();
			events.finishProcessing();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setMasterDelegate(MasterDelegate masterDelegate) {
		this.masterDelegate = masterDelegate;
	}

	@Override
	public MasterEventsManager getEventsManager() {
		return (MasterEventsManager) events;
	}

	@Override
	public NetsimNetwork getNetsimNetwork() {
		return null;
		//todo ???
	}

	@Override
	public Scenario getScenario() {
		return this.scenario;
	}

	@Override
	public MobsimTimer getSimTimer() {
		return this.simTimer;
	}

	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

	@Inject
	void addQueueSimulationListeners(Set<MobsimListener> listeners) {
		for (MobsimListener listener : listeners) {
			this.listenerManager.addQueueSimulationListener(listener);
		}
	}

	@Override
	public double getStopTime() {
		return 0;
	}

	public void afterMobsim() {
		System.out.println("Iteration ended latch countdown");
		iterationEndedLatch.countDown();
	}
}
