package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.matsim.api.core.v01.population.PlanElement;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ActivityDto.class, name = "act"),
        @JsonSubTypes.Type(value = LegDto.class, name = "leg")
})
public interface PlanElementDto {

    PlanElement toPlanElement();

}
