package org.marly.mavigo.service.journey.preferences;

import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TouristicModeJourneyStrategy implements JourneyPreferenceStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(TouristicModeJourneyStrategy.class);

    @Override
    public boolean supports(JourneyPreferences preferences) {
        return preferences.touristicModeEnabled();
    }

    @Override
    public void apply(JourneyPlanningContext context, PrimJourneyRequest request) {
        LOGGER.debug("Applying touristic mode preferences to journey request");
        // TODO: Implement touristic mode logic (e.g., prefer scenic routes, avoid
        // transfers, etc.)
    }
}
