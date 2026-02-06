package org.marly.mavigo.controller.dto;

import java.util.List;
import java.util.UUID;

public record EcoScoreResponse(
        double totalCo2Saved,
        int badgeCount,
        List<BadgeResponse> badges,
        List<JourneyActivityResponse> history) {

    public record BadgeResponse(String name, String description, String icon, java.time.OffsetDateTime earnedAt) {
    }

    public record JourneyActivityResponse(
            UUID journeyId,
            String origin,
            String destination,
            double distance,
            double co2Saved,
            java.time.OffsetDateTime timestamp) {
    }
}
