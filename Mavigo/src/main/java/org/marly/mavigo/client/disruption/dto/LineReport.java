package org.marly.mavigo.client.disruption.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineReport(
        @JsonProperty("line") NavitiaLine line,
        @JsonProperty("pt_objects") List<PtObject> ptObjects
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record PtObject(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("embedded_type") String embeddedType,
            @JsonProperty("stop_area") StopArea stopArea,
            @JsonProperty("links") List<Link> links
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record StopArea(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("label") String label,
            @JsonProperty("links") List<Link> links
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Link(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("rel") String rel,
            @JsonProperty("internal") Boolean internal
    ) {
    }
}

