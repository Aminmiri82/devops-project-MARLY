package org.marly.mavigo.service.journey;

import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;

public interface PrimJourneyRequestFactory {

    PrimJourneyRequest create(JourneyPlanningContext context);
}

