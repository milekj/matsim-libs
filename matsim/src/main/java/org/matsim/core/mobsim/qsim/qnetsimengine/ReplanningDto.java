package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.Optional;
import java.util.OptionalInt;

public class ReplanningDto {

    private Id<Person> personId;
    private OptionalInt planId;
    private Optional<PlanDto> newPlan;

    private ReplanningDto(Id<Person> personId, OptionalInt planId, Optional<PlanDto> newPlan) {
        this.personId = personId;
        this.planId = planId;
        this.newPlan = newPlan;
    }

    public ReplanningDto() {
    }

    public static ReplanningDto withSelectedPlan(Id<Person> personId, int planId) {
        return new ReplanningDto(personId, OptionalInt.of(planId), Optional.empty());
    }

    public static ReplanningDto withNewPlan(Id<Person> personId, PlanDto planDto) {
        return new ReplanningDto(personId, OptionalInt.empty(), Optional.of(planDto));
    }

    @JsonIgnore
    public Id<Person> getPersonId() {
        return personId;
    }

    public OptionalInt getPlanId() {
        return planId;
    }

    public void setPlanId(OptionalInt planId) {
        this.planId = planId;
    }

    public Optional<PlanDto> getNewPlan() {
        return newPlan;
    }

    public void setNewPlan(Optional<PlanDto> newPlan) {
        this.newPlan = newPlan;
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
