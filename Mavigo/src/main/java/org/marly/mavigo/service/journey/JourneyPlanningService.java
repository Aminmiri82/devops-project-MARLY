package org.marly.mavigo.service.journey;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;

public interface JourneyPlanningService {

    Journey planAndPersist(JourneyPlanningParameters parameters);
}

