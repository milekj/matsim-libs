/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
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

package org.matsim.core.scoring;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * This is a scoring function which is configurable with sum terms for Activities, Legs, money spent, and becoming stuck.
 * You can create an instance of this class from your own ScoringFunctionFactory and add different sum terms to it,
 * for example depending on a configuration option.
 * 
 * The sum terms are instances which implement BasicScoring as well as one of ActivityScoring, LegScoring, MoneyScoring or
 * AgentStuckScoring. They are notified about these events, and after finish() is called on them, they must be able to answer
 * getScore(). Instances of BasicScoring, like ScoringFunctions, must be able to reset() themselves between iterations, though 
 * this is currently not used (fresh instances are created).
 * 
 * Note that the startActivity() and endActivity() as well as starLeg() and endLeg() are redundant. The start time as well as
 * the end time is contained in the Activity and the Leg. This is for historical reasons, when the Activity and the Leg which 
 * were passed were instances from the Plan.
 * 
 * It is by no means necessary to use this class for a ScoringFunction. You are encouraged to just implement ScoringFunction.
 * 
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public class ScoringFunctionAccumulator implements ScoringFunction {
	
	public interface BasicScoring {
		public void finish();
		public double getScore();
		public void reset();
	}
	
	public interface ActivityScoring {
		void startActivity(final double time, final Activity act);
		void endActivity(final double time, final Activity act);
	}

	public interface LegScoring {
		void startLeg(final double time, final Leg leg);
		void endLeg(final double time);
	}
	
	public interface MoneyScoring {
		void addMoney(final double amount);
	}

	public interface AgentStuckScoring {
		void agentStuck(final double time);
	}
	
	private static Logger log = Logger.getLogger(ScoringFunctionAccumulator.class);

	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private ArrayList<LegScoring> legScoringFunctions = new ArrayList<LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();

	public final void handleActivity(Activity activity) {
        if (activity.getStartTime() != Time.UNDEFINED_TIME) {
            startActivity(activity.getStartTime(), activity);
        }
        if (activity.getEndTime() != Time.UNDEFINED_TIME) {
            endActivity(activity.getEndTime(), activity);
        }
    }

    public final void handleLeg(Leg leg) {
        startLeg(leg.getDepartureTime(), leg);
        endLeg(leg.getDepartureTime() + leg.getTravelTime());
    }
	
	@Override
	public void addMoney(double amount) {
		for (MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	@Override
	public void agentStuck(double time) {
		for (AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	public void startActivity(double time, Activity act) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.startActivity(time, act);
		}
	}

	public void endActivity(double time, Activity act) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.endActivity(time, act);
		}
	}

	public void startLeg(double time, Leg leg) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.startLeg(time, leg);
		}
	}

	public void endLeg(double time) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.endLeg(time);
		}
	}

	public void finish() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	@Override
	public double getScore() {
		double score = 0.0;
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
            double contribution = basicScoringFunction.getScore();
			if (log.isTraceEnabled()) {
				log.trace("Contribution of scoring function: " + basicScoringFunction.getClass().getName() + " is: " + contribution);
			}
            score += contribution;
		}
		return score;
	}

	@Override
	public void reset() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.reset();
		}
	}

	/**
	 * add the scoring function the list of functions, it implemented the
	 * interfaces.
	 * 
	 * @param scoringFunction
	 */
	public void addScoringFunction(BasicScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof LegScoring) {
			legScoringFunctions.add((LegScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}

	}

	public ArrayList<ActivityScoring> getActivityScoringFunctions() {
		return activityScoringFunctions;
	}

}
