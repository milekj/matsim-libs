package org.mjanowski;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.prepare.ReduceScenario;
import org.matsim.run.RunLosAngelesScenario;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Metis {

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.loadConfig("examples/scenarios/los-angeles/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
//        Network network = NetworkUtils.createNetwork(config);

//        ReduceScenario.main(scenario, config);

        Map<String, Integer> intIds = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(1);
        Map<Id<Node>, ? extends Node> nodes = network.getNodes();
        nodes.values()
                .stream()
                .map(n -> n.getId().toString())
                .forEach(sid -> {
                    intIds.put(sid, counter.getAndIncrement());
                });


        Map<Id<Link>, ? extends Link> links = network.getLinks();
        int linksNumber = links.values().stream()
                .filter(l -> !l.getFromNode().equals(l.getToNode()))
                .map(l -> Set.of(l.getFromNode().getId().toString(), l.getToNode().getId().toString()))
                .collect(Collectors.toSet())
                .size();

        Map<Integer, Set<Integer>> results = new TreeMap<>();
        for (Node node : nodes.values()) {
            String nodeStringId = node.getId().toString();
            Stream<String> inLinksStream = node.getInLinks().values()
                    .stream()
                    .map(l -> l.getFromNode().getId().toString());
            Stream<String> outLinksStream = node.getOutLinks().values()
                    .stream()
                    .map(l -> l.getToNode().getId().toString());
            Set<Integer> edges = Streams.concat(inLinksStream, outLinksStream)
                    .map(intIds::get)
                    .collect(Collectors.toSet());
            Integer nodeIntId = intIds.get(nodeStringId);
            edges.remove(nodeIntId);
            results.put(nodeIntId, edges);
        }

        HashMap<Integer, Long> nodesWeights = new HashMap<>(nodes.size());
        HashMap<Set<Integer>, Long> edgesWeights = new HashMap<>();

        scenario.getPopulation().getPersons()
                .values()
                .stream()
                .map(Person::getPlans)
                .flatMap(Collection::stream)
                .map(Plan::getPlanElements)
                .flatMap(Collection::stream)
                .filter(p -> p instanceof Leg)
                .map(p -> (Leg) p)
                .map(Leg::getRoute)
                .filter(r -> r instanceof NetworkRoute)
                .map(r -> (NetworkRoute) r)
                .flatMap(r -> {
                    List<Integer> route = new ArrayList<>();
                    Id<Node> fromNodeId = getFromNodeId(links, r.getStartLinkId());
                    route.add(intIds.get(fromNodeId.toString()));
                    r.getLinkIds()
                            .stream()
                            .map(lid -> getFromNodeId(links, lid))
                            .map(nid -> intIds.get(nid.toString()))
                            .forEach(route::add);
                    Id<Node> finalFromNodeId = getFromNodeId(links, r.getEndLinkId());
                    route.add(intIds.get(finalFromNodeId.toString()));
                    Id<Node> finalToNodeId = getToNodeId(links, r.getEndLinkId());
                    route.add(intIds.get(finalToNodeId.toString()));

                    for (int i = 0; i < route.size() - 1; i++) {
                        int node1IntId = route.get(i);
                        int node2IntId = route.get(i + 1);
                        if (node1IntId == node2IntId)
                            continue;
                        Set<Integer> key = Set.of(node1IntId, node2IntId);
                        edgesWeights.compute(key, (k, v) -> v != null ? v + 1 : 2);
                    }


                    return route.stream();
                })
                .forEach(nid -> nodesWeights.compute(nid, (k, v) -> v != null ? v + 1 : 1));

        boolean useEdgeWeights = false;


        try(PrintWriter writer = new PrintWriter("part.results")) {

            if (useEdgeWeights) {
                writer.format("%d %d 011 %n", nodes.size(), linksNumber);
                results.forEach((nid, edge) -> {
                    writer.write(nodesWeights.getOrDefault(nid, 0L).toString());
                    edge.forEach(id -> writer.format(" %d %d ", id, edgesWeights.getOrDefault(Set.of(nid, id), 1L)));
                    writer.println();
                });
            } else {
                writer.format("%d %d 010 %n", nodes.size(), linksNumber);
                results.forEach((nid, connected) -> {
                    writer.write(nodesWeights.getOrDefault(nid, 0L).toString());
                    connected.forEach(id -> writer.format(" %d ", id));
                    writer.println();
                });
            }

        }


        FileWriter idsWriter = new FileWriter("intIds");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(idsWriter, intIds);
    }

    private static Id<Node> getFromNodeId(Map<Id<Link>, ? extends Link> linksMap, Id<Link> linkId) {
        return linksMap.get(linkId).getFromNode().getId();
    }

    private static Id<Node> getToNodeId(Map<Id<Link>, ? extends Link> linksMap, Id<Link> linkId) {
        return linksMap.get(linkId).getToNode().getId();
    }
}
