package org.marly.mavigo.service.journey;

import java.util.List;

import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.journey.preferences.JourneyPreferenceStrategy;
import org.springframework.stereotype.Component;

@Component
public class DefaultPrimJourneyRequestFactory implements PrimJourneyRequestFactory {

    private final List<JourneyPreferenceStrategy> strategies;

    DefaultPrimJourneyRequestFactory(List<JourneyPreferenceStrategy> strategies) {
        this.strategies = strategies != null ? List.copyOf(strategies) : List.of();
    }

    @Override
    public PrimJourneyRequest create(JourneyPlanningContext context) {
        PrimJourneyRequest request = new PrimJourneyRequest(
                context.origin().getExternalId(),
                context.destination().getExternalId(),
                context.parameters().departureDateTime());

        // Allow walking to reach the first/last stop when using coordinates
        request.withFirstSectionModes(List.of("walking"));
        request.withLastSectionModes(List.of("walking"));

        // Autoriser plus de marche pour rejoindre un arrêt quand on part d'une adresse précise
        request.withMaxWalkingDurationToPt(3600);

        JourneyPreferences preferences = context.parameters().preferences();
        for (JourneyPreferenceStrategy strategy : strategies) {
            if (strategy.supports(preferences)) {
                strategy.apply(context, request);
            }
        }
        return request;
    }
}

