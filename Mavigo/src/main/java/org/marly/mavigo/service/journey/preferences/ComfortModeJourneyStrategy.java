package org.marly.mavigo.service.journey.preferences;

import org.marly.mavigo.client.prim.PrimJourneyRequest;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ComfortModeJourneyStrategy implements JourneyPreferenceStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComfortModeJourneyStrategy.class);

    @Override
    public boolean supports(JourneyPreferences preferences) {
        return preferences.comfortModeEnabled();
    }

    @Override
    public void apply(JourneyPlanningContext context, PrimJourneyRequest request) {
        LOGGER.debug("Applying comfort mode for user {}", context.user() != null ? context.user().getId() : "unknown");
        // TODO: Use user's comfort profile to fine tune PrimJourneyRequest (e.g. no buses, limit transfers)
    }
}

