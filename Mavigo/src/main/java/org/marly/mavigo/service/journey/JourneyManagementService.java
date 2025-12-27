package org.marly.mavigo.service.journey;

import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;

public interface JourneyManagementService {
    
    Journey startJourney(UUID journeyId);

    Journey completeJourney(UUID journeyId);

    Journey cancelJourney(UUID journeyId);

    Journey getJourney(UUID journeyId);
}
