/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package miniscenario;

import java.io.File;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import preprocess.AgentMemories;
import preprocess.AgentMemory;
import preprocess.DecisionModel;
import preprocess.DecisionModels;

public class CreatePopulation {
	private ScenarioImpl scenario = null;	
	private Config config;
	private final static Logger log = Logger.getLogger(CreatePopulation.class);	
	private DecisionModels decisionModels = new DecisionModels();
	private AgentMemories memories = new AgentMemories();
	
	private TreeMap<Id, ActivityFacility> homeLocations = new TreeMap<Id, ActivityFacility>();
	private TreeMap<Id, ActivityFacility> workLocations = new TreeMap<Id, ActivityFacility>();
	
	private DecisionModelCreator decisionModelCreator = new DecisionModelCreator();
	private Random random = new Random(37835409);
	
	
				
	public void createPopulation(ScenarioImpl scenario, Config config) {		
		this.scenario = scenario;
		this.config = config;		
		this.createPersons();
						
		for (String day : CreateNetwork.days) {
			for (Person p : this.scenario.getPopulation().getPersons().values()) {	
				DecisionModel decisionModel = this.decisionModelCreator.createDecisionModelForAgent((PersonImpl)p, this.memories.getMemory(p.getId()));
				this.decisionModels.addDecisionModelForAgent(decisionModel, p.getId());
				this.createDemandForPerson((PersonImpl)p, day);	
			}
		}
		for (String day : CreateNetwork.days) {
			// choose random plan for day
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				// set plan and remove it from memory
				Plan plan = this.memories.getMemory(person.getId()).getRandomPlanAndRemove(day, this.random);
				((PersonImpl)person).addPlan(plan);
				((PersonImpl)person).setSelectedPlan(plan);
				((PersonImpl)person).createDesires("desired activity durations");
            	((PersonImpl)person).getDesires().putActivityDuration("work", 8*3600);
            	((PersonImpl)person).getDesires().putActivityDuration("home", 8*3600);
            	((PersonImpl)person).getDesires().putActivityDuration("shop_retail", 0.5*3600);
            	((PersonImpl)person).getDesires().putActivityDuration("shop_service", 1*3600);
            	((PersonImpl)person).getDesires().putActivityDuration("leisure_sports_fun", 1*3600);
            	((PersonImpl)person).getDesires().putActivityDuration("leisure_gastro_culture", 2*3600);

			}
			String outPath = config.findParam(CreateNetwork.AGENT_INTERACTION_PREPROCESS, "outPath") + day;
			new File(outPath).mkdirs();
			this.write(outPath);
			
			// clear plan for day from persons
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				((PersonImpl)person).getPlans().clear();
			}
		}
	}
	
	private void initZone(Zone zone) {
		// fill zones
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {	
			if (zone.inZone(facility.getCoord())) {
				zone.addFacility(facility);
			}
		}
	}
	
	private void createPersons() {
		double sideLength = Double.parseDouble(config.findParam(CreateNetwork.AGENT_INTERACTION_PREPROCESS, "sideLength"));
		Zone centerZone = new Zone("centerZone", (Coord) new CoordImpl(sideLength / 2.0 - 500.0, sideLength / 2.0 + 500.0), 2000.0, 2000.0);	
		this.initZone(centerZone);
		
		Zone topLeftZone =  new Zone("topLeftZone", (Coord) new CoordImpl(0.0, sideLength), 2000.0, 2000.0); 
		this.initZone(topLeftZone);
		Zone bottomLeftZone =  new Zone("bottomLeftZone", (Coord) new CoordImpl(0.0, 2000.0), 2000.0, 2000.0); 
		this.initZone(bottomLeftZone);
		Zone bottomRightZone =  new Zone("bottomRightZone", (Coord) new CoordImpl(sideLength - 2000.0, 2000.0), 2000.0, 2000.0);
		this.initZone(bottomRightZone);		
		Zone topRightZone = new Zone("topRightZone", (Coord) new CoordImpl(sideLength - 2000.0, sideLength), 2000.0, 2000.0);
		this.initZone(topRightZone);
//		Zone bottomRightLargeZone = new Zone("bottomRightLargeZone", (Coord) new CoordImpl(sideLength - 2000.0, 2000.0), 2000.0, 2000.0);
//		this.initZone(bottomRightLargeZone);
		
		int personCnt = this.addPersons(centerZone, centerZone, 0);
		personCnt += this.addPersons(topLeftZone, centerZone, personCnt);
		personCnt += this.addPersons(bottomLeftZone, topRightZone, personCnt);
		personCnt += this.addPersons(bottomRightZone, topRightZone, personCnt);
		personCnt += this.addPersons(topRightZone, topRightZone, personCnt);
		
		log.info("Created " + personCnt + " persons");
	}
			
	private int addPersons(Zone origin, Zone destination, int offset) {				
		int personsPerLocation = Integer.parseInt(config.findParam(CreateNetwork.AGENT_INTERACTION_PREPROCESS, "personsPerZone"));
		int personCnt = 0;
		for (int j = 0; j < personsPerLocation; j++) {
			PersonImpl p = new PersonImpl(new IdImpl(personCnt + offset));			
			personCnt++;
			this.scenario.getPopulation().addPerson(p);	
			//TreeMap<Id, ActivityFacility> facilitiesHome = this.scenario.getActivityFacilities().getFacilitiesForActivityType("home");
			ActivityFacility home = origin.getRandomLocationInZone(this.random);
			while (!home.getActivityOptions().containsKey("home")){//||home.getActivityOptions().equals("shop_service")||home.getActivityOptions().equals("leisure_sports_fun")||home.getActivityOptions().equals("leisure_gastro_culture")||home.getActivityOptions().equals("work")){
				home = origin.getRandomLocationInZone(this.random);
			}
			this.homeLocations.put(p.getId(), home);
						
			ActivityFacility work = destination.getRandomLocationInZone(this.random);
			this.workLocations.put(p.getId(), work);
			
			// assign daily income according to one-sided Gauss distribution. 
			// TODO: check Gini coefficient, Lorenz distribution
			//double r = Math.abs(random.nextGaussian() * CreateNetwork.stdDev + CreateNetwork.mean);						
			
//			if (Boolean.parseBoolean(config.findParam(CreateNetwork.AGENT_INTERACTION_PREPROCESS, "richpoor"))) {
//				if (origin.getName().equals("centerZone")) { // poor area
//					r *= 0.8;
//				} 
//				else if (origin.getName().equals("bottomLeftZone")) { // rich area
//					r *= 1.0 / 0.8;
//				}
//			}
			
//			String votFactor = Double.toString(r);
//			this.votFactors.putAttribute(p.getId().toString(), "income", votFactor);
			
			this.memories.addMemory(p.getId(), new AgentMemory());
			this.memories.getMemory(p.getId()).setHomeZone(origin);
		}
		return personCnt;
	}
	
	private void createDemandForPerson(PersonImpl person, String day) {
		PlanImpl plan = new PlanImpl();
		ActivityFacility home = this.homeLocations.get(person.getId());
		
		ActivityImpl act = new ActivityImpl("home", home.getCoord());
		act.setFacilityId(home.getId());
		
		double endTime = Math.max(0.0, this.random.nextGaussian() * 2.0 * 3600.0 + 7.0 * 3600.0);
		act.setEndTime(endTime);
		plan.addActivity(act);
				
		endTime = this.addWorkingAct(plan, person, day, endTime);
		endTime = this.addOtherActs(plan, person, day, endTime);
		
		this.finishPlan(plan, person);		
		this.memories.getMemory(person.getId()).addPlan(plan, day);
	}
	
	private double addOtherActs(PlanImpl plan, PersonImpl person, String day, double endTime) {
		int offset = this.random.nextInt();
		Random rand = new Random(4711+offset);
		DecisionModel decisionModel = this.decisionModels.getDecisionModelForAgent(person.getId());
//		boolean checkShopping = false;
//		boolean checkLeisure = false;
//		
//		if (this.random.nextBoolean()) {
//			checkShopping = true;
//		}
//		else {
//			checkLeisure = true;
//		}
//		if (this.random.nextBoolean()) {
//			checkLeisure = true;
//		}
//		else {
//			checkShopping = true;
//		}
				
		if (rand.nextDouble()<0.4 //&& checkShopping
				) {
//		if (checkShopping) {
			
			plan.addLeg(new LegImpl("car"));
			TreeMap<Id, ActivityFacility> facilitiesShopRetail = this.scenario.getActivityFacilities().getFacilitiesForActivityType("shop_retail");
			ActivityFacility facility = (ActivityFacility) facilitiesShopRetail.values().toArray()[random.nextInt(facilitiesShopRetail.size())];
			ActivityImpl act = new ActivityImpl("shop_retail", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 2.0 * 3600.0 + 3.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
		}
		// check leisure
		if (rand.nextDouble()<0.05 //&& checkLeisure
				) {
//		if (checkLeisure) {
			plan.addLeg(new LegImpl("car"));
			TreeMap<Id, ActivityFacility> facilitiesShopService = this.scenario.getActivityFacilities().getFacilitiesForActivityType("shop_service");
			ActivityFacility facility = (ActivityFacility) facilitiesShopService.values().toArray()[random.nextInt(facilitiesShopService.size())];
			ActivityImpl act = new ActivityImpl("shop_service", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 2.0 * 3600.0 + 3.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
		}
		if (rand.nextDouble()<0.3 //&& checkLeisure
				) {
			plan.addLeg(new LegImpl("car"));
			TreeMap<Id, ActivityFacility> facilitiesSportsFun = this.scenario.getActivityFacilities().getFacilitiesForActivityType("leisure_sports_fun");
			ActivityFacility facility = (ActivityFacility) facilitiesSportsFun.values().toArray()[random.nextInt(facilitiesSportsFun.size())];
			ActivityImpl act = new ActivityImpl("leisure_sports_fun", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 2.0 * 3600.0 + 3.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
		}
		if (rand.nextDouble()<0.3 //&& checkLeisure
				) {
			plan.addLeg(new LegImpl("car"));
			TreeMap<Id, ActivityFacility> facilitiesGastroCulture = this.scenario.getActivityFacilities().getFacilitiesForActivityType("leisure_gastro_culture");
			ActivityFacility facility = (ActivityFacility) facilitiesGastroCulture.values().toArray()[random.nextInt(facilitiesGastroCulture.size())];
			ActivityImpl act = new ActivityImpl("leisure_gastro_culture", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 2.0 * 3600.0 + 3.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
		}
		return endTime;
	}
	
	private double addWorkingAct(PlanImpl plan, PersonImpl person, String day, double endTime) {
		DecisionModel decisionModel = this.decisionModels.getDecisionModelForAgent(person.getId());
		
		if (decisionModel.doesAct("work", day)) {
			plan.addLeg(new LegImpl("car"));
			ActivityFacility facility = this.workLocations.get(person.getId());
			ActivityImpl workAct = new ActivityImpl("work", facility.getCoord());
			endTime += Math.max(0.5 * 3600.0, this.random.nextGaussian() * 1.5 * 3600.0 + 8.0 * 3600.0);
			workAct.setEndTime(endTime);
			workAct.setFacilityId(facility.getId());
			plan.addActivity(workAct);
		}
		return endTime;
	}
	
	private void finishPlan(PlanImpl plan, PersonImpl person) {			
		plan.addLeg(new LegImpl("car"));
		ActivityFacility home = this.homeLocations.get(person.getId());
		ActivityImpl homeAct = new ActivityImpl("home", home.getCoord());
		homeAct.setFacilityId(home.getId());
		plan.addActivity(homeAct);
	}
							
	public void write(String path) {
		log.info("Writing plans ...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(path + "/plans.xml");
	}
}
