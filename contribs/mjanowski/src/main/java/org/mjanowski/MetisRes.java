package org.mjanowski;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.ListUtils;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkSVG2;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.prepare.ReduceScenario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MetisRes {

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.loadConfig("examples/scenarios/los-angeles/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

//        ReduceScenario.main(scenario, config);

        Map<Id<Node>, ? extends Node> nodes = network.getNodes();

        FileReader idsReader = new FileReader("intIds");
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Integer> readIntIds = objectMapper.readValue(idsReader, new TypeReference<>() {});

        HashBiMap<String, Integer> intIds = HashBiMap.create(readIntIds);
        BiMap<Integer, String> stringIds = intIds.inverse();

        int partitionsNumber = 2;

        BufferedReader reader = new BufferedReader(new FileReader("part.results.part." + partitionsNumber));


        HashMultimap<Integer, String> nodePartitions = HashMultimap.create();
        int index = 1;
        String line = reader.readLine();
        while (line != null) {
            int partitionNumber = Integer.parseInt(line);
            String nodeId = stringIds.get(index);
            nodePartitions.put(partitionNumber, nodeId);
            line = reader.readLine();
            index++;
        }


        List<String> colours = Arrays.asList("#e6194B", "#3cb44b", "#ffe119", "#4363d8", "#f58231", "#911eb4", "#42d4f4", "#f032e6",
                "#bfef45", "#fabed4", "#469990", "#dcbeff", "#9A6324", "#fffac8", "#800000", "#aaffc3", "#808000",
                "#ffd8b1", "#000075", "#a9a9a9", "#000000");

        Map<Integer, String> coloursMap = IntStream.range(0, colours.size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), colours::get));

        SingleGraph displayGraph = new SingleGraph("");

        for (Map.Entry<Integer, Collection<String>> entry : nodePartitions.asMap().entrySet()) {
            Integer partitionNumber = entry.getKey();
            entry.getValue().stream()
                    .map(Id::createNodeId)
                    .forEach(id -> {
                        org.graphstream.graph.Node graphNode = displayGraph.addNode(id.toString());
                        Coord coord = nodes.get(id).getCoord();
                        graphNode.setAttribute("xy", coord.getX(), coord.getY());
                        String colour = colours.get(partitionNumber);
                        graphNode.setAttribute("ui.style", String.format("fill-color: %s;", colour));
                    });
        }

        Map<Id<Link>, ? extends Link> links = network.getLinks();
        for (Link link : links.values()) {
            String fromNodeId = link.getFromNode().getId().toString();
            String toNodeId = link.getToNode().getId().toString();
            try {
                org.graphstream.graph.Edge edge = displayGraph.addEdge(link.getId().toString(), fromNodeId, toNodeId);
            } catch (Exception e) {
            }
        }

        FileSink fs = new FileSinkImages(FileSinkImages.OutputType.PNG, FileSinkImages.Resolutions.UHD_4K);
        fs.writeAll(displayGraph, "graph.png");

    }
}
