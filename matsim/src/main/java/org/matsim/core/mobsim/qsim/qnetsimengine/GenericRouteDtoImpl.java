package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteUtils;

public class GenericRouteDtoImpl extends AbstractRouteDtoImpl {

    private String routeDescription;

    public GenericRouteDtoImpl(double dist, Double travTime, Id<Link> startLinkId, Id<Link> endLinkId, String routeDescription) {
        super(dist, travTime, startLinkId, endLinkId);
        this.routeDescription = routeDescription;
    }

    public GenericRouteDtoImpl() {
    }

    @Override
    public Route toRoute() {
        Route genericRoute = RouteUtils.createGenericRouteImpl(getStartLinkId(), getEndLinkId());
        genericRoute.setDistance(getDist());
        genericRoute.setTravelTime(getTravTime());
        genericRoute.setRouteDescription(routeDescription);
        return genericRoute;
    }

}
