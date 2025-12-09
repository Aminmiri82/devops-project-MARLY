package org.marly.mavigo.client.prim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;

import org.marly.mavigo.client.prim.deserializer.NavitiaDateTimeDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimSection(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("departure_date_time")
        @JsonDeserialize(using = NavitiaDateTimeDeserializer.class)
        LocalDateTime departureDateTime,
        @JsonProperty("arrival_date_time")
        @JsonDeserialize(using = NavitiaDateTimeDeserializer.class)
        LocalDateTime arrivalDateTime,
        @JsonProperty("from") PrimStopPoint from,
        @JsonProperty("to") PrimStopPoint to,
        @JsonProperty("display_informations") PrimDisplayInformations displayInformations
) {
}
