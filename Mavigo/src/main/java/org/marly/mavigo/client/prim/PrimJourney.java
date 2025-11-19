package org.marly.mavigo.client.prim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.marly.mavigo.client.prim.deserializer.NavitiaDateTimeDeserializer;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrimJourney(
        @JsonProperty("duration") Integer duration,
        @JsonProperty("nb_transfers") Integer nbTransfers,
        @JsonProperty("departure_date_time")
        @JsonDeserialize(using = NavitiaDateTimeDeserializer.class)
        LocalDateTime departureDateTime,
        @JsonProperty("arrival_date_time")
        @JsonDeserialize(using = NavitiaDateTimeDeserializer.class)
        LocalDateTime arrivalDateTime,
        @JsonProperty("sections") List<PrimSection> sections
) {
}
