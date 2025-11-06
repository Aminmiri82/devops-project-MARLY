package org.marly.mavigo.service.itinerary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RerouteCommand(
        UUID journeyId,
        UUID alertId,
        OffsetDateTime detectedAt,
        boolean notifyUser) {
}

