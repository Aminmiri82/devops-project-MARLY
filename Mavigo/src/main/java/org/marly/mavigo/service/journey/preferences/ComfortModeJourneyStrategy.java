package org.marly.mavigo.service.journey.preferences;

import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.NamedComfortSettingRepository;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ComfortModeJourneyStrategy implements JourneyPreferenceStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComfortModeJourneyStrategy.class);

    private final NamedComfortSettingRepository namedComfortSettingRepository;

    public ComfortModeJourneyStrategy(NamedComfortSettingRepository namedComfortSettingRepository) {
        this.namedComfortSettingRepository = namedComfortSettingRepository;
    }

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

        ComfortProfile profile = resolveProfile(context);
        if (profile == null || !profile.hasSettings()) {
            LOGGER.info("Applied comfort profile is empty or null for user {}", user.getId());
            return;
        }

        LOGGER.debug(
                "Applying comfort profile details: directPath={}, transfers={}, waiting={}, walking={}, requireAC={}, wheelchair={}",
                profile.getDirectPath(), profile.getMaxNbTransfers(), profile.getMaxWaitingDuration(),
                profile.getMaxWalkingDuration(), profile.getRequireAirConditioning(), profile.getWheelchairAccessible());

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

        if (Boolean.TRUE.equals(profile.getWheelchairAccessible())) {
            request.withWheelchair(true);
            LOGGER.debug("Requesting wheelchair accessible routes");
        }
    }

    public ComfortProfile resolveProfile(JourneyPlanningContext context) {
        if (context == null || !context.parameters().preferences().comfortModeEnabled()) {
            return null;
        }

        User user = context.user();
        UUID namedSettingId = context.parameters().preferences().namedComfortSettingId();

        if (namedSettingId != null) {
            return namedComfortSettingRepository.findById(namedSettingId)
                    .filter(ns -> ns.getUser().getId().equals(user.getId()))
                    .map(NamedComfortSetting::getComfortProfile)
                    .orElseGet(user::getComfortProfile);
        }
        return user.getComfortProfile();
    }
}
