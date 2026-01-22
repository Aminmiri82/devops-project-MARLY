package org.marly.mavigo.client.prim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimPlacesResponse(
        @JsonProperty("places") List<PrimPlace> places
) {
}
