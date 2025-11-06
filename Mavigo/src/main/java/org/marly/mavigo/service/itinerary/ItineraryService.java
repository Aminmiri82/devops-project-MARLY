 package org.marly.mavigo.service.itinerary;

import java.util.UUID;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.service.itinerary.dto.PlanJourneyCommand;
import org.marly.mavigo.service.itinerary.dto.RerouteCommand;

public interface ItineraryService {

    Journey planJourney(PlanJourneyCommand command);

    Journey rerouteJourney(RerouteCommand command); // alert tells us what lines we have to avoid

    Journey getActiveJourney(UUID journeyId);
}

