package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

public class DepartVehicleDto {

    private Id<Person> personId;
    private int personLinkIndex;
    private int planIndex;
    private Id<Link> toLinkId;

    public DepartVehicleDto(Id<Person> personId, int personLinkIndex, int planIndex, Id<Link> toLinkId) {
        this.personId = personId;
        this.personLinkIndex = personLinkIndex;
        this.planIndex = planIndex;
        this.toLinkId = toLinkId;
    }

    public DepartVehicleDto() {
    }

    @JsonIgnore
    public Id<Person> getPersonId() {
        return personId;
    }

    public int getPersonLinkIndex() {
        return personLinkIndex;
    }

    public int getPlanIndex() {
        return planIndex;
    }

    @JsonIgnore
    public Id<Link> getToLinkId() {
        return toLinkId;
    }

    @JsonSetter
    public void setPersonId(String  personId) {
        this.personId = Id.createPersonId(personId);
    }

    @JsonSetter
    public void setPersonLinkIndex(int personLinkIndex) {
        this.personLinkIndex = personLinkIndex;
    }

    public void setPlanIndex(int planIndex) {
        this.planIndex = planIndex;
    }

    @JsonSetter
    public void setToLinkId(String toLinkId) {
        this.toLinkId = Id.createLinkId(toLinkId);
    }

    @JsonGetter("personId")
    public String getPersonIdAsString() {
        return personId.toString();
    }

    @JsonGetter("toLinkId")
    public String getToLinkIdAsString() {
        return toLinkId.toString();
    }

}

