package org.marly.mavigo.client.prim;

import org.marly.mavigo.client.prim.deserializer.PrimCoordinatesDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PrimCoordinatesDeserializer.class)
public record PrimCoordinates(
        @JsonProperty("lat") Double latitude,
        @JsonProperty("lon") Double longitude
) {
}
