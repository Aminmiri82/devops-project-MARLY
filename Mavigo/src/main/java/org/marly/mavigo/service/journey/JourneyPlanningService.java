package org.marly.mavigo.service.journey;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;

public interface JourneyPlanningService {

    java.util.List<Journey> planAndPersist(JourneyPlanningParameters parameters);

    java.util.List<Journey> updateJourneyWithDisruption(java.util.UUID journeyId, org.marly.mavigo.models.disruption.Disruption disruption, Double userLat, Double userLng, String newOrigin);
}

