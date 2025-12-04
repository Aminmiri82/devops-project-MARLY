package org.marly.mavigo.service.journey.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record JourneyPlanningParameters(
        UUID userId,
        String originQuery,
        String destinationQuery,
        LocalDateTime departureDateTime,
        JourneyPreferences preferences) {

    public JourneyPlanningParameters {
        Objects.requireNonNull(userId, "userId must be provided when planning a journey");
        originQuery = sanitize(originQuery, "originQuery");
        destinationQuery = sanitize(destinationQuery, "destinationQuery");
        departureDateTime = departureDateTime != null ? departureDateTime : LocalDateTime.now();
        preferences = preferences != null ? preferences : JourneyPreferences.disabled();
    }

    private static String sanitize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}

