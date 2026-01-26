package org.marly.mavigo.service.journey;

import java.util.List;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.user.ComfortProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JourneyResultFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JourneyResultFilter.class);

    private final org.marly.mavigo.service.journey.preferences.ComfortModeJourneyStrategy comfortStrategy;

    public JourneyResultFilter(
            org.marly.mavigo.service.journey.preferences.ComfortModeJourneyStrategy comfortStrategy) {
        this.comfortStrategy = comfortStrategy;
    }

    public List<PrimJourneyPlanDto> filterByComfortProfile(
            List<PrimJourneyPlanDto> plans,
            org.marly.mavigo.service.journey.dto.JourneyPlanningContext context,
            boolean comfortModeEnabled) {

        if (!comfortModeEnabled || context == null || context.user() == null) {
            return plans;
        }

        ComfortProfile profile = comfortStrategy.resolveProfile(context);
        if (profile == null) {
            LOGGER.debug("JourneyResultFilter: User has no comfort profile, bypassing filters");
            return plans;
        }

        LOGGER.debug("JourneyResultFilter starting with {} plans. ComfortMode={}, RequireAC={}",
                plans.size(), comfortModeEnabled, profile.getRequireAirConditioning());

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
