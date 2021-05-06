/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReplanner.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.mobsim.qsim.MasterDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.ReplanningDto;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;

import javax.inject.Provider;
import java.util.List;

final class MasterPlansReplanningImpl implements PlansReplanning, ReplanningListener {

	private final Provider<ReplanningContext> replanningContextProvider;
	private MasterDelegate masterDelegate;
	private Population population;
	private StrategyManager strategyManager;

	@Inject
    MasterPlansReplanningImpl(StrategyManager strategyManager,
                              Population pop,
                              Provider<ReplanningContext> replanningContextProvider,
                              MasterDelegate masterDelegate) {
		this.population = pop;
		this.strategyManager = strategyManager;
		this.replanningContextProvider = replanningContextProvider;
		this.masterDelegate = masterDelegate;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		List<ReplanningDto> replanningDtos = strategyManager.run(population, event.getIteration(), replanningContextProvider.get());
		masterDelegate.sendReplanning(replanningDtos);
	}

}
