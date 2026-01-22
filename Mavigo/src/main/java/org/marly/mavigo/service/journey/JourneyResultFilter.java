package org.marly.mavigo.service.journey;

import java.util.List;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JourneyResultFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JourneyResultFilter.class);

    public List<PrimJourneyPlanDto> filterByComfortProfile(
            List<PrimJourneyPlanDto> plans,
            User user,
            boolean comfortModeEnabled) {

        if (!comfortModeEnabled || user == null) {
            return plans;
        }

        ComfortProfile profile = user.getComfortProfile();
        if (profile == null) {
            return plans;
        }

        if (Boolean.TRUE.equals(profile.getRequireAirConditioning())) {
            List<PrimJourneyPlanDto> filtered = plans.stream()
                    .filter(PrimJourneyPlanDto::hasAirConditioningOnAllTransitLegs)
                    .toList();

            LOGGER.info("A/C filter: {} of {} journeys have air conditioning on all transit legs",
                    filtered.size(), plans.size());

            if (filtered.isEmpty()) {
                LOGGER.warn("No journeys with A/C found, returning all options");
                return plans;
            }
            return filtered;
        }

        return plans;
    }
}
