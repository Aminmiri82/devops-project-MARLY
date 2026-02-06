package org.marly.mavigo.service.journey;

import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;

public interface JourneyManagementService {

    JourneyActionResult startJourney(UUID journeyId);

    JourneyActionResult completeJourney(UUID journeyId);

    Journey cancelJourney(UUID journeyId);

    Journey getJourney(UUID journeyId);

    void clearAllData();
}
