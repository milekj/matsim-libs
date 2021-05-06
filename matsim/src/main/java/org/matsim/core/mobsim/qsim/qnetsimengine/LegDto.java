package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;

public class LegDto implements PlanElementDto {

    private RouteDto route;
    private double depTime;
    private double travTime;
    private String mode;

    public LegDto(RouteDto route, double depTime, double travTime, String mode) {
        this.route = route;
        this.depTime = depTime;
        this.travTime = travTime;
        this.mode = mode;
    }

    public LegDto() {
    }

    public RouteDto getRoute() {
        return route;
    }

    public void setRoute(RouteDto route) {
        this.route = route;
    }

    public double getDepTime() {
        return depTime;
    }

    public void setDepTime(double depTime) {
        this.depTime = depTime;
    }

    public double getTravTime() {
        return travTime;
    }

    public void setTravTime(double travTime) {
        this.travTime = travTime;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public Leg toPlanElement() {
        Leg leg = PopulationUtils.createLeg(mode);
        leg.setDepartureTime(depTime);
        leg.setTravelTime(travTime);
        leg.setRoute(route.toRoute());
        return leg;
    }
}
