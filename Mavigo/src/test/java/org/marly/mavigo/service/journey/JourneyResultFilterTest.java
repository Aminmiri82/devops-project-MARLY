package org.marly.mavigo.service.journey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.journey.preferences.ComfortModeJourneyStrategy;

import static org.mockito.Mockito.when;

class JourneyResultFilterTest {

    private JourneyResultFilter filter;
    private ComfortModeJourneyStrategy comfortStrategy;

    @BeforeEach
    void setUp() {
        comfortStrategy = Mockito.mock(ComfortModeJourneyStrategy.class);
        filter = new JourneyResultFilter(comfortStrategy);
    }

    @Test
    void filterByComfortProfile_returnsOriginalListWhenComfortModeDisabled() {
        User user = createUserWithAcRequired();
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc());
        JourneyPlanningContext context = createDefaultContext(user, false);

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, context, false);

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByComfortProfile_returnsOriginalListWhenUserIsNull() {
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, null, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByComfortProfile_returnsOriginalListWhenNoProfile() {
        User user = new User("ext", "test@example.com", "Test User");
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc());
        JourneyPlanningContext context = createDefaultContext(user, true);

        when(comfortStrategy.resolveProfile(context)).thenReturn(null);

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, context, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByComfortProfile_filtersOutPlansWithoutAcWhenRequired() {
        User user = createUserWithAcRequired();
        PrimJourneyPlanDto planWithAc = createPlanWithAc();
        PrimJourneyPlanDto planWithoutAc = createPlanWithoutAc();
        List<PrimJourneyPlanDto> plans = List.of(planWithAc, planWithoutAc);
        JourneyPlanningContext context = createDefaultContext(user, true);

        when(comfortStrategy.resolveProfile(context)).thenReturn(user.getComfortProfile());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, context, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(planWithAc);
    }

    @Test
    void filterByComfortProfile_returnsAllPlansWhenNoMatchingAc() {
        User user = createUserWithAcRequired();
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc(), createPlanWithoutAc());
        JourneyPlanningContext context = createDefaultContext(user, true);

        when(comfortStrategy.resolveProfile(context)).thenReturn(user.getComfortProfile());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, context, true);

        assertThat(result).hasSize(2);
    }

    @Test
    void filterByComfortProfile_returnsOriginalListWhenAcNotRequired() {
        User user = createUserWithProfile(profile -> profile.setMaxNbTransfers(2));
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc());
        JourneyPlanningContext context = createDefaultContext(user, true);

        when(comfortStrategy.resolveProfile(context)).thenReturn(user.getComfortProfile());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, context, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByComfortProfile_handlesEmptyPlansList() {
        User user = createUserWithAcRequired();
        JourneyPlanningContext context = createDefaultContext(user, true);

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(Collections.emptyList(), context, true);

        assertThat(result).isEmpty();
    }

    private JourneyPlanningContext createDefaultContext(User user, boolean comfortEnabled) {
        UUID userId = (user != null && user.getId() != null) ? user.getId() : UUID.randomUUID();
        JourneyPreferences prefs = new JourneyPreferences(comfortEnabled, false, null);
        JourneyPlanningParameters params = new JourneyPlanningParameters(
                userId, "From", "To", LocalDateTime.now(), prefs, false);
        return new JourneyPlanningContext(user, null, null, params);
    }

    private User createUserWithAcRequired() {
        return createUserWithProfile(profile -> profile.setRequireAirConditioning(true));
    }

    private User createUserWithProfile(java.util.function.Consumer<ComfortProfile> configurer) {
        User user = new User("ext", "test@example.com", "Test User");
        ComfortProfile profile = new ComfortProfile();
        configurer.accept(profile);
        user.setComfortProfile(profile);
        return user;
    }

    private PrimJourneyPlanDto createPlanWithAc() {
        return createPlan("journey-1", true);
    }

    private PrimJourneyPlanDto createPlanWithoutAc() {
        return createPlan("journey-2", false);
    }

    private PrimJourneyPlanDto createPlan(String journeyId, boolean hasAc) {
        OffsetDateTime now = OffsetDateTime.now();
        var leg = new PrimJourneyPlanDto.LegDto(
                1, "section-1", "public_transport", "METRO", "M1",
                "Metro Line 1", "FFCD00", "RATP",
                now, now.plusMinutes(30), 1800,
                "stop-origin", "Origin Station", 48.8566, 2.3522,
                "stop-dest", "Destination Station", 48.8606, 2.3376,
                null, hasAc, null);
        return new PrimJourneyPlanDto(journeyId, now, now.plusMinutes(30), 1800, 0, List.of(leg));
    }
}
