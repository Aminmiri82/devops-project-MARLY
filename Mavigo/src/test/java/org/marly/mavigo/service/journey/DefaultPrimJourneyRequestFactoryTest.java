package org.marly.mavigo.service.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.journey.preferences.JourneyPreferenceStrategy;

class DefaultPrimJourneyRequestFactoryTest {

    @Test
    void delegatesToStrategiesWhenSupported() {
        JourneyPreferenceStrategy strategy = Mockito.mock(JourneyPreferenceStrategy.class);
        DefaultPrimJourneyRequestFactory factory = new DefaultPrimJourneyRequestFactory(List.of(strategy));

        JourneyPreferences preferences = new JourneyPreferences(true, false);
        JourneyPlanningContext context = buildContext(preferences);

        when(strategy.supports(preferences)).thenReturn(true);

        PrimJourneyRequest request = factory.create(context);

        assertThat(request.getFromStopAreaId()).isEqualTo("origin-ext");
        assertThat(request.getToStopAreaId()).isEqualTo("dest-ext");

        verify(strategy).supports(preferences);
        verify(strategy).apply(context, request);
    }

    @Test
    void skipsStrategiesWhenNotSupported() {
        JourneyPreferenceStrategy strategy = Mockito.mock(JourneyPreferenceStrategy.class);
        DefaultPrimJourneyRequestFactory factory = new DefaultPrimJourneyRequestFactory(List.of(strategy));

        JourneyPreferences preferences = JourneyPreferences.disabled();
        JourneyPlanningContext context = buildContext(preferences);

        when(strategy.supports(preferences)).thenReturn(false);

        factory.create(context);

        verify(strategy).supports(preferences);
        verify(strategy, never()).apply(Mockito.any(), Mockito.any());
    }

    private JourneyPlanningContext buildContext(JourneyPreferences preferences) {
        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                UUID.randomUUID(),
                "origin-query",
                "dest-query",
                LocalDateTime.now(),
                preferences);

        User user = new User("ext", "user@example.com", "Example User");
        StopArea origin = new StopArea("origin-ext", "Origin", new GeoPoint(0d, 0d));
        StopArea destination = new StopArea("dest-ext", "Destination", new GeoPoint(1d, 1d));
        return new JourneyPlanningContext(user, origin, destination, parameters);
    }
}

