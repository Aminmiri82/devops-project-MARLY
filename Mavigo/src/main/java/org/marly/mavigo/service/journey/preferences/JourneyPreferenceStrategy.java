package org.marly.mavigo.service.journey.preferences;

import org.marly.mavigo.client.prim.PrimJourneyRequest;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

public interface JourneyPreferenceStrategy {

    boolean supports(JourneyPreferences preferences);

    void apply(JourneyPlanningContext context, PrimJourneyRequest request);
}

