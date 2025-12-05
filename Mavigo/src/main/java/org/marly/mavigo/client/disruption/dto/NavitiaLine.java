package org.marly.mavigo.client.disruption.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NavitiaLine(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("code") String code,
        @JsonProperty("commercial_mode") CommercialMode commercialMode,
        @JsonProperty("network") Network network,
        @JsonProperty("color") String color,
        @JsonProperty("text_color") String textColor
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record CommercialMode(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Network(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {
    }
}

