package org.mjanowski;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.*;
import jnr.ffi.annotations.In;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.*;
import org.graphstream.ui.view.Viewer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.prepare.ReduceScenario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetworkPartitioner {

    private final Network network;
    private final Config config;

    public NetworkPartitioner(Network network, Config config) {
        this.network = network;
        this.config = config;
    }

    public Map<Integer, Partition> partition(int partitionsNumber) {

        URL partitionFileUrl = ConfigGroup.getInputFileURL(this.config.getContext(), "part.results.part." + partitionsNumber);
        String partitionFilePath = partitionFileUrl.getFile();
        if (Files.exists(Paths.get(partitionFilePath))) {

            try {
                Map<Id<Node>, ? extends Node> nodes = network.getNodes();
                URL intIdsFileUrl = ConfigGroup.getInputFileURL(this.config.getContext(), "intIds");
                FileReader idsReader = new FileReader(intIdsFileUrl.getFile());
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Integer> readIntIds = objectMapper.readValue(idsReader, new TypeReference<>() {});

                HashBiMap<String, Integer> intIds = HashBiMap.create(readIntIds);
                BiMap<Integer, String> stringIds = intIds.inverse();
                BufferedReader reader = new BufferedReader(new FileReader(partitionFilePath));


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

                Map<Integer, Partition> result = nodePartitions.asMap().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                            List<Node> partitionNodes = e.getValue().stream().map(Id::createNodeId).map(nodes::get).collect(Collectors.toList());
                            return new Partition(0, partitionNodes);
                        }));
                saveGraph(result);
                return result;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

        }


        Map<Id<Node>, ? extends Node> nodes = network.getNodes();
        int nodesNumber = nodes.size();
        int nodesPerPartition = nodesNumber / partitionsNumber;
        int reminderNodesNumber = nodesNumber % partitionsNumber;
        Map<Integer, Partition> partitions = IntStream.range(0, partitionsNumber)
                .boxed()
                .collect(Collectors.toMap(Function.identity(),
                        k -> k < reminderNodesNumber ? new Partition(nodesPerPartition + 1) : new Partition(nodesPerPartition)));

        TreeSet<Id<Node>> unvisitedNodesIds = nodes.values()
                .stream()
                .sorted(Comparator.comparingLong((Node n) -> n.getInLinks().size() + n.getOutLinks().size()).reversed())
                .map(Node::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        LinkedList<Id<Node>> nodesToVisitIds = new LinkedList<>();

        Iterator<Partition> partitionsIterator = partitions.values().iterator();
        Partition currentPartition = partitionsIterator.next();
        int nodesCounter = 0;
        while (unvisitedNodesIds.size() > 0) {
            Id<Node> sourceNodeId = unvisitedNodesIds.first();
            Node source = nodes.get(sourceNodeId);
            nodesToVisitIds.add(source.getId());

            while (nodesToVisitIds.size() > 0) {
                Id<Node> firstNodeId = nodesToVisitIds.pollLast();
                if (!unvisitedNodesIds.contains(firstNodeId))
                    continue;
                Node first = nodes.get(firstNodeId);
                Collection<? extends Link> outLinks = source.getOutLinks().values();
                List<Id<Node>> nextNodesIds = outLinks.stream()
                        .map(Link::getToNode)
                        .map(Node::getId)
                        .filter(unvisitedNodesIds::contains)
                        .collect(Collectors.toList());
                nodesToVisitIds.addAll(nextNodesIds);
                unvisitedNodesIds.remove(firstNodeId);

                if (nodesCounter == currentPartition.getMaxSize()) {
                    nodesCounter = 0;
                    currentPartition = partitionsIterator.next();
                }
                currentPartition.addNode(first);
                nodesCounter++;
            }
        }

        saveGraph(partitions);

        return partitions;
    }

    private void saveGraph(Map<Integer, Partition> partitions) {
        List<String> colours = Arrays.asList("#e6194B", "#3cb44b", "#ffe119", "#4363d8", "#f58231", "#911eb4", "#42d4f4", "#f032e6",
        "#bfef45", "#fabed4", "#469990", "#dcbeff", "#9A6324", "#fffac8", "#800000", "#aaffc3", "#808000",
        "#ffd8b1", "#000075", "#a9a9a9", "#000000");

        Map<Integer, String> coloursMap = IntStream.range(0, colours.size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), colours::get));

        SingleGraph displayGraph = new SingleGraph("");
        for (int i = 0; i < partitions.values().size(); i++) {
            Partition partition = partitions.get(i);
            for (Node node : partition.getNodes()) {
                Id<Node> nodeId = node.getId();
                org.graphstream.graph.Node graphNode = displayGraph.addNode(nodeId.toString());
                Coord coord = network.getNodes().get(nodeId).getCoord();
                graphNode.setAttribute("xy", coord.getX(), coord.getY());
                String colour = colours.get(i);
                graphNode.setAttribute("ui.style", String.format("fill-color: %s;", colour));
            }
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

        try {
        FileSink fs = new FileSinkImages(FileSinkImages.OutputType.PNG, FileSinkImages.Resolutions.UHD_4K);
        fs.writeAll(displayGraph, "graph.png");
    } catch (IOException e) {
        e.printStackTrace();
    }
    }

    public Map<Integer, Collection<String>> partitionsToWorkersIds(Map<Integer, Partition> partitions) {
        return partitions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getNodes().stream().map(n -> n.getId().toString()).collect(Collectors.toList())));
    }


    public Map<Integer, Collection<Integer>> getWorkersConnections(Map<Integer, Partition> partitions) {
        Map<Id<Node>, Integer> nodesWorkersId = partitions.entrySet()
                .stream()
                .flatMap(e -> e.getValue().getNodes().stream().map(n -> new AbstractMap.SimpleEntry<>(n.getId(), e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        SetMultimap<Integer, Integer> nodesConnections = HashMultimap.create(partitions.size(), partitions.size());
        network.getNodes().values()
                .forEach(n -> {
                    Integer fromWorkerId = nodesWorkersId.get(n.getId());
                    List<Integer> toWorkerIds = Stream.concat(n.getOutLinks().values().stream(),
                                n.getInLinks().values().stream())
                            .map(Link::getToNode)
                            .map(Node::getId)
                            .map(nodesWorkersId::get)
                            .collect(Collectors.toList());
                    nodesConnections.putAll(fromWorkerId, toWorkerIds);
                });
        nodesConnections.keySet().forEach((k -> nodesConnections.remove(k, k)));
        return nodesConnections.asMap();
    }
}
