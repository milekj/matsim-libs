package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.List;

public class ExperimentalTransitRouteDto extends AbstractRouteDtoImpl {

    private String routeDescription;

    public ExperimentalTransitRouteDto(double dist, Double travTime, Id<Link> startLinkId, Id<Link> endLinkId, String routeDescription) {
        super(dist, travTime, startLinkId, endLinkId);
        this.routeDescription = routeDescription;
    }

    public ExperimentalTransitRouteDto() {
    }

    public String getRouteDescription() {
        return routeDescription;
    }

    public void setRouteDescription(String routeDescription) {
        this.routeDescription = routeDescription;
    }

    @Override
    public ExperimentalTransitRoute toRoute() {
        ExperimentalTransitRoute route = new ExperimentalTransitRoute(getStartLinkId(), getEndLinkId());
        route.setDistance(getDist());
        route.setTravelTime(getTravTime());
        route.setRouteDescription(routeDescription);
        return route;
    }

}