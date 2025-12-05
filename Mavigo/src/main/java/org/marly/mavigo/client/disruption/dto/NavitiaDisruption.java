package org.marly.mavigo.client.disruption.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.util.List;
import org.marly.mavigo.client.prim.deserializer.NavitiaDateTimeDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NavitiaDisruption(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("cause") String cause,
        @JsonProperty("category") String category,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("updated_at") @JsonDeserialize(using = NavitiaDateTimeDeserializer.class) LocalDateTime updatedAt,
        @JsonProperty("severity") Severity severity,
        @JsonProperty("application_periods") List<ApplicationPeriod> applicationPeriods,
        @JsonProperty("messages") List<Message> messages
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Severity(
            @JsonProperty("name") String name,
            @JsonProperty("effect") String effect,
            @JsonProperty("priority") Integer priority,
            @JsonProperty("color") String color
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Message(
            @JsonProperty("text") String text,
            @JsonProperty("channel") Channel channel
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Channel(
            @JsonProperty("name") String name,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("types") List<String> types
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record ApplicationPeriod(
            @JsonProperty("begin") @JsonDeserialize(using = NavitiaDateTimeDeserializer.class) LocalDateTime begin,
            @JsonProperty("end") @JsonDeserialize(using = NavitiaDateTimeDeserializer.class) LocalDateTime end
    ) {
    }
}

