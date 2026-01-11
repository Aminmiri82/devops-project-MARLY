package org.marly.mavigo.service.journey.preferences;

import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
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
        User user = context.user();
        if (user == null) {
            LOGGER.warn("No user in context, cannot apply comfort profile");
            return;
        }

        ComfortProfile profile = user.getComfortProfile();
        if (profile == null || !profile.hasSettings()) {
            LOGGER.debug("User {} has no comfort profile configured", user.getId());
            return;
        }

        if (profile.getDirectPath() != null) {
            request.withDirectPath(profile.getDirectPath());
        }

        if (profile.getMaxNbTransfers() != null) {
            request.withMaxNbTransfers(profile.getMaxNbTransfers());
        }

        if (profile.getMaxWaitingDuration() != null) {
            request.withMaxWaitingDuration(profile.getMaxWaitingDuration());
        }

        if (profile.getMaxWalkingDuration() != null) {
            request.withMaxWalkingDurationToPt(profile.getMaxWalkingDuration());
        }

        if (Boolean.TRUE.equals(profile.getRequireAirConditioning())) {
            request.withEquipmentDetails(true);
            LOGGER.debug("Requesting equipment details for A/C filtering");
        }
    }
}
