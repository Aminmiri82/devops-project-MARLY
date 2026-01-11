package org.marly.mavigo.service.journey.preferences;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

class ComfortModeJourneyStrategyTest {

    private ComfortModeJourneyStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ComfortModeJourneyStrategy();
    }

    @Test
    void supports_returnsTrueWhenComfortModeEnabled() {
        JourneyPreferences prefs = new JourneyPreferences(true, false);
        assertThat(strategy.supports(prefs)).isTrue();
    }

    @Test
    void supports_returnsFalseWhenComfortModeDisabled() {
        JourneyPreferences prefs = new JourneyPreferences(false, true);
        assertThat(strategy.supports(prefs)).isFalse();
    }

    @Test
    void apply_setsDirectPathWhenConfigured() {
        User user = createUserWithProfile(profile -> profile.setDirectPath("none"));
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getDirectPath()).isPresent().hasValue("none");
    }

    @Test
    void apply_setsMaxNbTransfersWhenConfigured() {
        User user = createUserWithProfile(profile -> profile.setMaxNbTransfers(2));
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getMaxNbTransfers()).isPresent().hasValue(2);
    }

    @Test
    void apply_setsMaxWaitingDurationWhenConfigured() {
        User user = createUserWithProfile(profile -> profile.setMaxWaitingDuration(600));
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getMaxWaitingDuration()).isPresent().hasValue(600);
    }

    @Test
    void apply_setsMaxWalkingDurationWhenConfigured() {
        User user = createUserWithProfile(profile -> profile.setMaxWalkingDuration(300));
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getMaxWalkingDurationToPt()).isPresent().hasValue(300);
    }

    @Test
    void apply_setsEquipmentDetailsWhenAirConditioningRequired() {
        User user = createUserWithProfile(profile -> profile.setRequireAirConditioning(true));
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getEquipmentDetails()).isPresent().hasValue(true);
    }

    @Test
    void apply_doesNotModifyRequestWhenNoProfile() {
        User user = new User("ext", "test@example.com", "Test User");
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getDirectPath()).isEmpty();
        assertThat(request.getMaxNbTransfers()).isEmpty();
        assertThat(request.getMaxWaitingDuration()).isEmpty();
        assertThat(request.getMaxWalkingDurationToPt()).isEmpty();
        assertThat(request.getEquipmentDetails()).isEmpty();
    }

    @Test
    void apply_doesNotModifyRequestWhenNullUser() {
        JourneyPlanningContext context = buildContext(null);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getDirectPath()).isEmpty();
    }

    @Test
    void apply_appliesAllSettingsTogether() {
        User user = createUserWithProfile(profile -> {
            profile.setDirectPath("only");
            profile.setMaxNbTransfers(1);
            profile.setMaxWaitingDuration(900);
            profile.setMaxWalkingDuration(600);
            profile.setRequireAirConditioning(true);
        });
        JourneyPlanningContext context = buildContext(user);
        PrimJourneyRequest request = new PrimJourneyRequest("origin", "dest", LocalDateTime.now());

        strategy.apply(context, request);

        assertThat(request.getDirectPath()).hasValue("only");
        assertThat(request.getMaxNbTransfers()).hasValue(1);
        assertThat(request.getMaxWaitingDuration()).hasValue(900);
        assertThat(request.getMaxWalkingDurationToPt()).hasValue(600);
        assertThat(request.getEquipmentDetails()).hasValue(true);
    }

    private User createUserWithProfile(java.util.function.Consumer<ComfortProfile> configurer) {
        User user = new User("ext", "test@example.com", "Test User");
        ComfortProfile profile = new ComfortProfile();
        configurer.accept(profile);
        user.setComfortProfile(profile);
        return user;
    }

    private JourneyPlanningContext buildContext(User user) {
        JourneyPlanningParameters params = new JourneyPlanningParameters(
                UUID.randomUUID(),
                "origin-query",
                "dest-query",
                LocalDateTime.now(),
                new JourneyPreferences(true, false));
        StopArea origin = new StopArea("origin-ext", "Origin", new GeoPoint(0d, 0d));
        StopArea destination = new StopArea("dest-ext", "Destination", new GeoPoint(1d, 1d));
        return new JourneyPlanningContext(user, origin, destination, params);
    }
}
