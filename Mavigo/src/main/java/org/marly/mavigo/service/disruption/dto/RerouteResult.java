package org.marly.mavigo.service.disruption.dto;

import java.util.List;

import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;

public record RerouteResult(
        Disruption disruption,
        JourneyPoint disruptedPoint,
        JourneyPoint newOriginPoint,
        List<Journey> alternatives
) {}
