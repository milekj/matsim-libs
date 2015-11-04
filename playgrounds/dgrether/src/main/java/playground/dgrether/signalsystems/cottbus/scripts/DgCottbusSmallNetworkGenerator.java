/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusSmallNetworkGenerator
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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.FeatureNetworkLinkStartEndCoordFilter;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;
import playground.dgrether.utils.DgNet2Shape;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;


public class DgCottbusSmallNetworkGenerator {

	private static final Logger log = Logger.getLogger(DgCottbusSmallNetworkGenerator.class);

	private GeometryFactory geoFac = new GeometryFactory();

	private CoordinateReferenceSystem networkSrs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);

	private Envelope boundingBox;

	private Network shrinkedNetwork;

	public Network createSmallNetwork(Network net, SignalSystemsData signalSystemsData, String outputDirectory, double offset) {
		//	Tuple<CoordinateReferenceSystem, Feature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature(DgPaths.REPOS
		//	 + "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
		//Feature cottbusFeature = cottbusFeatureTuple.getSecond();
		//CoordinateReferenceSystem cottbusFeatureCrs = cottbusFeatureTuple.getFirst();


		//get all signalized link ids
		Map<Id, Set<Id>> signalizedLinkIdsBySystemIdMap = DgSignalsUtils.calculateSignalizedLinksPerSystem(signalSystemsData); 
		Set<Id> signalizedLinkIds = new HashSet<Id>();
		for (Set<Id> set : signalizedLinkIdsBySystemIdMap.values()){
			signalizedLinkIds.addAll(set);
		}
		SimpleFeature boundingboxFeature = calcBoundingBox(net, signalizedLinkIds, offset);

		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		filterManager.addLinkFilter(new FeatureNetworkLinkStartEndCoordFilter(networkSrs, boundingboxFeature, networkSrs));
		Network newNetwork = filterManager.applyFilters();

		//NetworkCleaner netCleaner = new NetworkCleaner();
		//netCleaner.run(newNetwork);

		String output = outputDirectory + "network_small";
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		new DgNet2Shape().write(newNetwork, output + ".shp", crs);

		Collection<SimpleFeature> boundingBoxCollection = new ArrayList<SimpleFeature>();
		boundingBoxCollection.add(boundingboxFeature);
		ShapeFileWriter.writeGeometries(boundingBoxCollection, outputDirectory + "bounding_box.shp");


		this.shrinkedNetwork = newNetwork;
		return newNetwork;		
	}

	public Network createSmallNetwork(String networkFile, String outputDirectory, String signalSystemsFile, double offset){
		Config c1 = ConfigUtils.createConfig();
		c1.network().setInputFile(networkFile);
		c1.scenario().setUseSignalSystems(true);
		c1.signalSystems().setSignalSystemFile(signalSystemsFile);
		Scenario scenario = ScenarioUtils.loadScenario(c1);
		SignalsData signalsdata = scenario.getScenarioElement(SignalsData.class);
		Network net = scenario.getNetwork();
		return this.createSmallNetwork(net, signalsdata.getSignalSystemsData(), outputDirectory, offset);

	}

	public Network getShrinkedNetwork(){
		return this.shrinkedNetwork;
	}

	public CoordinateReferenceSystem getCrs(){
		return this.networkSrs;
	}

	public Envelope getBoundingBox() {
		return this.boundingBox;
	}

	private SimpleFeature calcBoundingBox(Network net, Set<Id> signalizedLinkIds, double offset) {
		Link l = null;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Id linkId : signalizedLinkIds){
			l = net.getLinks().get(linkId);
			if (l.getCoord().getX() < minX) {
				minX = l.getCoord().getX();
			}
			if (l.getCoord().getX() > maxX) {
				maxX = l.getCoord().getX();
			}
			if (l.getCoord().getY() > maxY) {
				maxY = l.getCoord().getY();
			}
			if (l.getCoord().getY() < minY) {
				minY = l.getCoord().getY();
			}
		}

		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY);

		minX = minX - offset;
		minY = minY - offset;
		maxX = maxX + offset;
		maxY = maxY + offset;
		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY + " offset used: " + offset);

		Coordinate[] coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(minX, minY);
		coordinates[1] = new Coordinate(minX, maxY);
		coordinates[2] = new Coordinate(maxX, maxY);
		coordinates[3] = new Coordinate(maxX, minY);
		coordinates[4] = coordinates[0];


		this.boundingBox = new Envelope(coordinates[0], coordinates[2]);
		
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().
				setCrs(networkSrs).
				setName("link").
				create();
		return factory.createPolygon(coordinates);
	}

	public static void main(String[] args){
		new DgCottbusSmallNetworkGenerator().createSmallNetwork(args[0], args[1], args[2], Double.valueOf(args[3]));
	}

}
