package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GenericRouteDtoImpl.class, name = "gen"),
        @JsonSubTypes.Type(value = LinkNetworkRouteDtoImpl.class, name = "link"),
        @JsonSubTypes.Type(value = ExperimentalTransitRouteDto.class, name = "exp")
})
public interface RouteDto {

    Route toRoute();

}
