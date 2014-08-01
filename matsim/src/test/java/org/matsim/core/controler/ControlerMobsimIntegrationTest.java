/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ControlerMobsimIntegrationTest {

	private final static Logger log = Logger.getLogger(ControlerMobsimIntegrationTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunMobsim_qsim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("qsim");
		cfg.controler().setWritePlansInterval(0);
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
		Assert.assertTrue(c.sim instanceof QSim);
	}

	@Test
	public void testRunMobsim_qsim_parallel() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("qsim");
		cfg.controler().setWritePlansInterval(0);
		cfg.qsim().setNumberOfThreads(3);
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
		Assert.assertTrue(c.sim instanceof QSim);
	}

	@Test
	public void testRunMobsim_queueSimulation() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("queueSimulation");
		cfg.controler().setWritePlansInterval(0);
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
		Assert.assertTrue(c.sim instanceof QueueSimulation);
	}

	@Test
	public void testRunMobsim_jdeqsim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim(MobsimType.JDEQSim.toString());
		cfg.controler().setWritePlansInterval(0);
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
		Assert.assertTrue("sim is of wrong type " + c.sim.getClass().getCanonicalName(), c.sim instanceof JDEQSimulation);
	}

	@Test
	public void testRunMobsim_customMobsim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("counting");
		cfg.controler().setWritePlansInterval(0);
		Controler c = new Controler(cfg);
		CountingMobsimFactory mf = new CountingMobsimFactory();
		c.addMobsimFactory("counting", mf);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
		Assert.assertEquals(1, mf.callCount);
	}

	@Test
	public void testRunMobsim_missingMobsimFactory() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("counting");
		cfg.controler().setWritePlansInterval(0);
		Controler c = new Controler(cfg);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
        Assert.assertNotNull("expected exception, but there was none.", c.uncaughtException);
	}

	private static class FakeControler extends Controler {
		/*package*/ Mobsim sim = null;
		public FakeControler(final Config cfg) {
			super(cfg);
		}
		@Override
		Mobsim getNewMobsim() {
			// remember the created sim, but return a dummy
			this.sim = super.getNewMobsim();
			return new FakeSimulation();
		}
	}

	private static class CountingMobsimFactory implements MobsimFactory {

		/*package*/ int callCount = 0;

		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			this.callCount++;
			return new FakeSimulation();
		}
	}

	private static class FakeSimulation implements Mobsim {
		@Override
		public void run() {
		}
	}
}
