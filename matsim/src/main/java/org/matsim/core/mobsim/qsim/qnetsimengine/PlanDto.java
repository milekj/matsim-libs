package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.List;

public class PlanDto {

    private List<PlanElementDto> actsLegs;
    private Double score;
    private Id<Person> personId;
    private String type;

    public PlanDto(List<PlanElementDto> actsLegs, Double score, Id<Person> personId, String type) {
        this.actsLegs = actsLegs;
        this.score = score;
        this.personId = personId;
        this.type = type;
    }

    public PlanDto() {
    }

    public List<PlanElementDto> getActsLegs() {
        return actsLegs;
    }

    public void setActsLegs(List<PlanElementDto> actsLegs) {
        this.actsLegs = actsLegs;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @JsonIgnore
    public Id<Person> getPersonId() {
        return personId;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonGetter("personId")
    public String getPersonIdAsString() {
        return personId.toString();
    }

    @JsonSetter
    public void setPersonId(String personId) {
        this.personId = Id.createPersonId(personId);
    }

}
