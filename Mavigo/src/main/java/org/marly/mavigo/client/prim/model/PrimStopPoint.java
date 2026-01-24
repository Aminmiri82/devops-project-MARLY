package org.marly.mavigo.client.prim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimStopPoint(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("coord") PrimCoordinates coordinates,
        @JsonProperty("stop_area") PrimStopArea stopArea
) {}
