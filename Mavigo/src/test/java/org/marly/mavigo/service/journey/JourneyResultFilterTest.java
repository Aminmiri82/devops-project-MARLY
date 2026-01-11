package org.marly.mavigo.service.journey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;

class JourneyResultFilterTest {

    private JourneyResultFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JourneyResultFilter();
    }

    @Test
    void filterByComfortProfile_returnsOriginalListWhenComfortModeDisabled() {
        User user = createUserWithAcRequired();
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, user, false);

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

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, user, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByComfortProfile_filtersOutPlansWithoutAcWhenRequired() {
        User user = createUserWithAcRequired();
        PrimJourneyPlanDto planWithAc = createPlanWithAc();
        PrimJourneyPlanDto planWithoutAc = createPlanWithoutAc();
        List<PrimJourneyPlanDto> plans = List.of(planWithAc, planWithoutAc);

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, user, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(planWithAc);
    }

    @Test
    void filterByComfortProfile_returnsAllPlansWhenNoMatchingAc() {
        User user = createUserWithAcRequired();
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc(), createPlanWithoutAc());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, user, true);

        assertThat(result).hasSize(2);
    }

    @Test
    void filterByComfortProfile_returnsOriginalListWhenAcNotRequired() {
        User user = createUserWithProfile(profile -> profile.setMaxNbTransfers(2));
        List<PrimJourneyPlanDto> plans = List.of(createPlanWithoutAc());

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(plans, user, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByComfortProfile_handlesEmptyPlansList() {
        User user = createUserWithAcRequired();

        List<PrimJourneyPlanDto> result = filter.filterByComfortProfile(Collections.emptyList(), user, true);

        assertThat(result).isEmpty();
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
        OffsetDateTime now = OffsetDateTime.now();
        PrimJourneyPlanDto.LegDto leg = new PrimJourneyPlanDto.LegDto(
                1,                      // sequenceOrder
                "section-1",            // sectionId
                "public_transport",     // sectionType - transit leg
                "METRO",                // commercialMode
                "M1",                   // lineCode
                now,                    // departureDateTime
                now.plusMinutes(30),    // arrivalDateTime
                1800,                   // durationSeconds
                "stop-origin",          // originStopId
                "Origin Station",       // originLabel
                48.8566,                // originLatitude
                2.3522,                 // originLongitude
                "stop-dest",            // destinationStopId
                "Destination Station",  // destinationLabel
                48.8606,                // destinationLatitude
                2.3376,                 // destinationLongitude
                null,                   // notes
                true);                  // hasAirConditioning
        return new PrimJourneyPlanDto(
                "journey-1",
                now,
                now.plusMinutes(30),
                1800,
                0,
                List.of(leg));
    }

    private PrimJourneyPlanDto createPlanWithoutAc() {
        OffsetDateTime now = OffsetDateTime.now();
        PrimJourneyPlanDto.LegDto leg = new PrimJourneyPlanDto.LegDto(
                1,                      // sequenceOrder
                "section-1",            // sectionId
                "public_transport",     // sectionType - transit leg
                "METRO",                // commercialMode
                "M1",                   // lineCode
                now,                    // departureDateTime
                now.plusMinutes(30),    // arrivalDateTime
                1800,                   // durationSeconds
                "stop-origin",          // originStopId
                "Origin Station",       // originLabel
                48.8566,                // originLatitude
                2.3522,                 // originLongitude
                "stop-dest",            // destinationStopId
                "Destination Station",  // destinationLabel
                48.8606,                // destinationLatitude
                2.3376,                 // destinationLongitude
                null,                   // notes
                false);                 // hasAirConditioning
        return new PrimJourneyPlanDto(
                "journey-2",
                now,
                now.plusMinutes(30),
                1800,
                0,
                List.of(leg));
    }
}
