/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.*;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.Mobsim;

import javax.inject.Inject;
import java.util.Set;

class WorkerNewController extends AbstractController implements ControlerI {


	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(WorkerNewController.class);

	private final Config config;
	private final PrepareForSim prepareForSim;
	private final PrepareForMobsim prepareForMobsim;
	private final Provider<Mobsim> mobsimProvider;
	private final TerminationCriterion terminationCriterion;
	private final Set<ControlerListener> controlerListenersDeclaredByModules;
	private final ControlerConfigGroup controlerConfigGroup;
	private final OutputDirectoryHierarchy outputDirectoryHierarchy;

	@Inject
	WorkerNewController(Config config, ControlerListenerManagerImpl controlerListenerManager, MatsimServices matsimServices,
						IterationStopWatch stopWatch, PrepareForSim prepareForSim,
						Provider<Mobsim> mobsimProvider,
						TerminationCriterion terminationCriterion,
						Set<ControlerListener> controlerListenersDeclaredByModules, ControlerConfigGroup controlerConfigGroup,
						OutputDirectoryHierarchy outputDirectoryHierarchy
			, PrepareForMobsim prepareForMobsim
	) {
		super(controlerListenerManager, stopWatch, matsimServices);
		this.config = config;
		this.prepareForMobsim = prepareForMobsim;
		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		this.prepareForSim = prepareForSim;
		this.mobsimProvider = mobsimProvider;
		this.terminationCriterion = terminationCriterion;
		this.controlerListenersDeclaredByModules = controlerListenersDeclaredByModules;
		this.controlerConfigGroup = controlerConfigGroup;
		this.outputDirectoryHierarchy = outputDirectoryHierarchy;
	}

	@Override
	public final void run() {
		super.setupOutputDirectory(outputDirectoryHierarchy);
		super.run(this.config);
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	@Override
	protected final void loadCoreListeners() {

		for (ControlerListener controlerListener : this.controlerListenersDeclaredByModules) {
			this.addControlerListener(controlerListener);
		}
	}

	@Override
	protected final void prepareForSim() {
		this.prepareForSim.run();
	}

	@Override
	protected final void prepareForMobsim() {
		this.prepareForMobsim.run() ;
//		this.prepareForSim.run() ;
	}

	@Override
	protected final void runMobSim() {
		this.mobsimProvider.get().run();
	}

	@Override
	protected final boolean continueIterations(int it) {
		return terminationCriterion.continueIterations(it);
	}


}
