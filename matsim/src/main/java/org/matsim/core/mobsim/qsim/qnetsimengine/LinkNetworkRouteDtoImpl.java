package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

public class LinkNetworkRouteDtoImpl extends AbstractRouteDtoImpl implements RouteDto {

    private List<Id<Link>> route;
    private double travelCost;
    private Id<Vehicle> vehicleId;

    public LinkNetworkRouteDtoImpl(double dist, Double travTime, Id<Link> startLinkId, Id<Link> endLinkId, List<Id<Link>> route, double travelCost, Id<Vehicle> vehicleId) {
        super(dist, travTime, startLinkId, endLinkId);
        this.route = route;
        this.travelCost = travelCost;
        this.vehicleId = vehicleId;
    }

    public LinkNetworkRouteDtoImpl() {
    }

    public double getTravelCost() {
        return travelCost;
    }

    public void setTravelCost(double travelCost) {
        this.travelCost = travelCost;
    }

    @JsonIgnore
    public List<Id<Link>> getRoute() {
        return route;
    }

    @JsonIgnore
    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    @JsonGetter("route")
    public List<String> getRouteAsStrings() {
        return route.stream().map(Id::toString).collect(Collectors.toList());
    }

    @JsonSetter
    public void setRoute(List<String> route) {
        this.route = route.stream().map(id -> Id.createLinkId(id)).collect(Collectors.toList());
    }

    @JsonGetter("vehicleId")
    public String getVehicleIdAsString() {
        return vehicleId != null ? vehicleId.toString() : null;
    }

    @JsonSetter
    public void setVehicleId(String vehicleId) {
        this.vehicleId = EventsMapper.idOrNull(vehicleId, Vehicle.class);
    }

    @Override
    public Route toRoute() {
        NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(getStartLinkId(), this.route, getEndLinkId());
        route.setTravelCost(travelCost);
        route.setDistance(getDist());
        route.setVehicleId(vehicleId);
        route.setTravelCost(getTravelCost());
        return route;
    }
}
