package org.marly.mavigo.client.prim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.marly.mavigo.client.prim.deserializer.NavitiaDateTimeDeserializer;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimStopDateTime(
        @JsonProperty("stop_point") PrimStopPoint stopPoint,
        @JsonProperty("arrival_date_time") @JsonDeserialize(using = NavitiaDateTimeDeserializer.class) LocalDateTime arrivalDateTime,
        @JsonProperty("departure_date_time") @JsonDeserialize(using = NavitiaDateTimeDeserializer.class) LocalDateTime departureDateTime) {
}