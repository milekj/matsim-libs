package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;

public abstract class AbstractRouteDtoImpl implements RouteDto {

    private double dist;
    private Double travTime;
    private Id<Link> startLinkId;
    private Id<Link> endLinkId;

    public AbstractRouteDtoImpl(double dist, Double travTime, Id<Link> startLinkId, Id<Link> endLinkId) {
        this.dist = dist;
        this.travTime = travTime;
        this.startLinkId = startLinkId;
        this.endLinkId = endLinkId;
    }

    public AbstractRouteDtoImpl() {
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public Double getTravTime() {
        return travTime;
    }

    public void setTravTime(Double travTime) {
        this.travTime = travTime;
    }

    @JsonIgnore
    public Id<Link> getStartLinkId() {
        return startLinkId;
    }

    @JsonIgnore
    public Id<Link> getEndLinkId() {
        return endLinkId;
    }

    @JsonGetter("startLinkId")
    public String getStartLinkIdAsString() {
        return startLinkId.toString();
    }

    @JsonSetter
    public void setStartLinkId(String startLinkId) {
        this.startLinkId = Id.createLinkId(startLinkId);
    }

    @JsonGetter("endLinkId")
    public String getEndLinkIdAsString() {
        return endLinkId.toString();
    }

    @JsonSetter
    public void setEndLinkId(String endLinkId) {
        this.endLinkId = Id.createLinkId(endLinkId);
    }

}
