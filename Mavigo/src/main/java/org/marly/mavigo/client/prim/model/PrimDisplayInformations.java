package org.marly.mavigo.client.prim.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimDisplayInformations(
        @JsonProperty("label") String label,
        @JsonProperty("code") String code,
        @JsonProperty("color") String color,
        @JsonProperty("network") String network,
        @JsonProperty("commercial_mode") String commercialMode,
        @JsonProperty("equipments") List<String> equipments
) {
}
