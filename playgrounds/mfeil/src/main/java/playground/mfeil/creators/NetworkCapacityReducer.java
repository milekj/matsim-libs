/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansGeneral.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.creators;



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;


/**
 * This class reduces the freespeed of all links of a network by a certain factor
 * @author mfeil
 */
public class NetworkCapacityReducer {

	private static final Logger log = Logger.getLogger(NetworkCapacityReducer.class);
	private NetworkImpl network;

	public NetworkCapacityReducer(NetworkImpl network){
		this.network = network;
	}

	private void run (double factor, double absoluteDelta, String output){
		for (Link link : this.network.getLinks().values()) {
	//		link.setFreespeed(link.getFreespeed(0)*factor);
			if (link.getFreespeed()+absoluteDelta>0) link.setFreespeed(link.getFreespeed()+absoluteDelta);
			else {
				log.warn("Cannot reduce speed of link "+link.getId()+" ("+link.getFreespeed()+") by delta of "+absoluteDelta+". Reducing speed by 10% instead.");
				link.setFreespeed(link.getFreespeed()*0.9);
			}
		}
		new NetworkWriter(this.network).write(output);
	}


	public static void main(final String [] args) {
		// Scenario files
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";

		// Output file
		final String outputFile = "/home/baug/mfeil/data/Zurich10/network_-10.xml";

		// Settings
		final double factor = 0.7;
		final double absoluteDelta = -2.7778; // in m/s := -10km/h



		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);

		NetworkCapacityReducer ncr = new NetworkCapacityReducer(scenarioMATSim.getNetwork());
		ncr.run(factor, absoluteDelta, outputFile);

		log.info("Reduction of network capacity finished.");
	}

}

