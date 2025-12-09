package org.marly.mavigo.service.itinerary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.marly.mavigo.models.shared.GeoPoint;

public record PlanJourneyCommand(
        UUID userId,
        String originLabel,
        GeoPoint originCoordinate,
        String destinationLabel,
        GeoPoint destinationCoordinate,
        OffsetDateTime departureTime,
        boolean enableComfortMode,
        boolean enableTouristicMode) {
}

