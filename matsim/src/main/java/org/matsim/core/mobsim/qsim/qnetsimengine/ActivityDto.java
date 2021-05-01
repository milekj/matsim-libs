package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

public class ActivityDto implements PlanElementDto {

    private double endTime;
    private double startTime;
    private double dur;
    private String type;
    private CoordDto coord;
    private Id<Link> linkId;
    private Id<ActivityFacility> facilityId;

    public ActivityDto(double endTime, double startTime, double dur, String type, CoordDto coord, Id<Link> linkId, Id<ActivityFacility> facilityId) {
        this.endTime = endTime;
        this.startTime = startTime;
        this.dur = dur;
        this.type = type;
        this.coord = coord;
        this.linkId = linkId;
        this.facilityId = facilityId;
    }

    public ActivityDto() {
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getDur() {
        return dur;
    }

    public void setDur(double dur) {
        this.dur = dur;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CoordDto getCoord() {
        return coord;
    }

    public void setCoord(CoordDto coord) {
        this.coord = coord;
    }

    @JsonIgnore
    public Id<Link> getLinkId() {
        return linkId;
    }

    @JsonIgnore
    public Id<ActivityFacility> getFacilityId() {
        return facilityId;
    }

    @JsonGetter("linkId")
    public String getLinkIdAsString() {
        return linkId.toString();
    }

    @JsonSetter
    public void setLinkId(String linkId) {
        this.linkId = Id.createLinkId(linkId);
    }

    @JsonGetter("facilityId")
    public String getFacilityIdAsString() {
        return facilityId != null ? facilityId.toString() : null;
    }

    @JsonSetter
    public void setFacilityId(String facilityId) {
        this.facilityId = EventsMapper.idOrNull(facilityId, ActivityFacility.class);
    }

    @Override
    public Activity toPlanElement() {
        Activity activity = PopulationUtils.createActivityFromCoordAndLinkId(type, coord.toCoord(), linkId);
        activity.setEndTime(endTime);
        activity.setFacilityId(facilityId);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        return activity;
    }

}
