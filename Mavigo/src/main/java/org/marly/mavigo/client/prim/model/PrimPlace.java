package org.marly.mavigo.client.prim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimPlace(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("embedded_type") String embeddedType,
        @JsonProperty("stop_area") PrimStopArea stopArea,
        @JsonProperty("stop_point") PrimStopPoint stopPoint,
        @JsonProperty("coord") PrimCoordinates coordinates
) {
}
