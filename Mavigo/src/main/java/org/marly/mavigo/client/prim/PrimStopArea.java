package org.marly.mavigo.client.prim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimStopArea(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("coord") PrimCoordinates coordinates
) {
}
