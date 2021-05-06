/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.qnetsimengine.PlanDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.ReplanningDto;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author nagel
 *
 */
public class GenericPlanStrategyImpl<T extends BasicPlan, I> implements GenericPlanStrategy<T, I> {

	private PlanSelector<T, I> planSelector = null;
	private GenericPlanStrategyModule<T> firstModule = null;
	private final ArrayList<GenericPlanStrategyModule<T>> modules = new ArrayList<>();
	private final ArrayList<T> plans = new ArrayList<>();
	private final List<ReplanningDto> selectedPlansReplanningDtos = new LinkedList<>();
	private long counter = 0;
	private ReplanningContext replanningContext;
	private final static Logger log = Logger.getLogger(PlanStrategyImpl.class);

	/**
	 * Creates a new strategy using the specified planSelector.
	 *
	 */
	public GenericPlanStrategyImpl(final PlanSelector<T, I> planSelector) {
		this.planSelector = planSelector;
	}

	public void addStrategyModule(final GenericPlanStrategyModule<T> module) {
		if (this.firstModule == null) {
			this.firstModule = module;
		} else {
			this.modules.add(module);
		}
	}
	
	public int getNumberOfStrategyModules() {
		if (this.firstModule == null) {
			return 0;
		}
		return this.modules.size() + 1; // we also have to count "firstModule", thus +1
	}
	
	@Override
	public void run(final HasPlansAndId<T, I> person) {
		this.counter++;
		
		// if there is at least one unscored plan, find that one:
		T plan = new RandomUnscoredPlanSelector<T, I>().selectPlan(person) ;
		
		// otherwise, find one according to selector (often defined in PlanStrategy ctor):
		if (plan == null) {
			plan = this.planSelector.selectPlan(person);
		}
		
		// "select" that plan:
		if ( plan != null ) {
			person.setSelectedPlan(plan);
		}
		else {
			log.error( planSelector+" returned no plan: not changing selected plan for person "+person );
		}

		// if there is a "module" (i.e. "innovation"):
		if (this.firstModule != null) {
			
			// set the working plan to a copy of the selected plan:
			plan = person.createCopyOfSelectedPlanAndMakeSelected();
			
			// add new plan to container that contains the plans that are handled by this PlanStrategy:
			this.plans.add(plan);

			// start working on this new plan:
			this.firstModule.handlePlan(plan);
		} else {
			selectedPlansReplanningDtos.add(ReplanningDto.withSelectedPlan((Id<Person>) person.getId(), person.getSelectedPlanIndex(plan)));
		}

	}

	@Override
	public void init(ReplanningContext replanningContext0) {
		this.replanningContext = replanningContext0;
		if (this.firstModule != null) {
			this.firstModule.prepareReplanning(replanningContext0);
		}
	}

	@Override
	public void finish() {
		if (this.firstModule != null) {
			// finish the first module
				this.firstModule.finishReplanning();
			// now work through the others
			for (GenericPlanStrategyModule<T> module : this.modules) {
				module.prepareReplanning(replanningContext);
				for (T plan : this.plans) {
					module.handlePlan(plan);
				}
				module.finishReplanning();
			}
		}
		this.plans.clear();
		log.info("Plan-Strategy finished, " + this.counter + " plans handled. Strategy: " + this.toString());
		this.counter = 0;
	}

	@Override
	public List<ReplanningDto> finishAndReturn() {
		if (this.firstModule != null) {
			// finish the first module
			this.firstModule.finishReplanning();
			// now work through the others
			for (GenericPlanStrategyModule<T> module : this.modules) {
				module.prepareReplanning(replanningContext);
				for (T plan : this.plans) {
					module.handlePlan(plan);
				}
				module.finishReplanning();
			}
		}
		Stream<ReplanningDto> newPlanReplanningDtos = plans.stream()
				.map(p -> {
					PlanDto planDto = p.toDto();
					Id<Person> personId = planDto.getPersonId();
					return ReplanningDto.withNewPlan(personId, planDto);
				});
		List<ReplanningDto> result = Stream.concat(newPlanReplanningDtos, selectedPlansReplanningDtos.stream())
				.collect(Collectors.toList());
		this.plans.clear();
		log.info("Plan-Strategy finished, " + this.counter + " plans handled. Strategy: " + this.toString());
		this.counter = 0;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder name = new StringBuilder(20);
		name.append(this.planSelector.getClass().getSimpleName());
		if (this.firstModule != null) {
			name.append('_');
			name.append(this.firstModule.getClass().getSimpleName());
			for (Object module : this.modules) {
				name.append('_');
				name.append(module.getClass().getSimpleName());
			}
		}
		return name.toString();
	}

	public PlanSelector<T, I> getPlanSelector() {
		return planSelector;
	}
	

}
