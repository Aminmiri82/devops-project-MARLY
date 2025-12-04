package org.marly.mavigo.service.journey.dto;

import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;

public record JourneyPlanningContext(
        User user,
        StopArea origin,
        StopArea destination,
        JourneyPlanningParameters parameters) {
}

