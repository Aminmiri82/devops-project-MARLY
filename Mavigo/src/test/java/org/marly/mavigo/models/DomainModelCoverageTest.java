package org.marly.mavigo.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.alert.AlertSeverity;
import org.marly.mavigo.models.alert.TrafficAlert;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.poi.PointOfInterest;
import org.marly.mavigo.models.poi.PointOfInterestCategory;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.tracking.Badge;
import org.marly.mavigo.models.tracking.JourneyActivity;
import org.marly.mavigo.models.tracking.UserBadge;
import org.marly.mavigo.models.user.PasswordResetToken;
import org.marly.mavigo.models.user.User;

class DomainModelCoverageTest {

    @Test
    void leg_roundTripGettersAndSetters() {
        Leg leg = new Leg();
        leg.setSequenceOrder(1);
        leg.setMode(TransitMode.BUS);
        leg.setLineCode("42");
        leg.setOriginLabel("Origin");
        leg.setDestinationLabel("Destination");
        leg.setOriginCoordinate(new GeoPoint(48.8566, 2.3522));
        leg.setDestinationCoordinate(new GeoPoint(48.8580, 2.3600));
        leg.setEstimatedDeparture(OffsetDateTime.parse("2026-02-14T08:00:00Z"));
        leg.setEstimatedArrival(OffsetDateTime.parse("2026-02-14T08:20:00Z"));
        leg.setDistanceMeters(3000);
        leg.setDurationSeconds(1200);
        leg.setNotes("Comfortable ride");

        assertEquals(1, leg.getSequenceOrder());
        assertEquals(TransitMode.BUS, leg.getMode());
        assertEquals("42", leg.getLineCode());
        assertEquals("Origin", leg.getOriginLabel());
        assertEquals("Destination", leg.getDestinationLabel());
        assertEquals(3000, leg.getDistanceMeters());
        assertEquals(1200, leg.getDurationSeconds());
        assertEquals("Comfortable ride", leg.getNotes());
    }

    @Test
    void pointOfInterest_roundTripGettersAndSetters() {
        PointOfInterest poi = new PointOfInterest("ext-poi", "Museum", PointOfInterestCategory.MUSEUM);
        poi.setLocation(new GeoPoint(48.8606, 2.3376));
        poi.setAverageRating(new BigDecimal("4.6"));
        poi.setReviewCount(2500);
        poi.setSource("google");
        poi.setPrimaryPhotoUrl("https://example.com/photo.jpg");
        poi.setShortDescription("A great museum");

        assertEquals("ext-poi", poi.getExternalId());
        assertEquals("Museum", poi.getName());
        assertEquals(PointOfInterestCategory.MUSEUM, poi.getCategory());
        assertEquals(new BigDecimal("4.6"), poi.getAverageRating());
        assertEquals(2500, poi.getReviewCount());
        assertEquals("google", poi.getSource());
        assertEquals("https://example.com/photo.jpg", poi.getPrimaryPhotoUrl());
        assertEquals("A great museum", poi.getShortDescription());
    }

    @Test
    void trafficAlert_replaceAffectedStopsAndReadOnlyView() {
        TrafficAlert alert = new TrafficAlert(
                "source-1",
                AlertSeverity.HIGH,
                "Line disruption",
                OffsetDateTime.parse("2026-02-14T08:00:00Z"));
        alert.setDescription("Signal issue");
        alert.setLineCode("RER-A");
        alert.setValidUntil(OffsetDateTime.parse("2026-02-14T10:00:00Z"));
        alert.replaceAffectedStopIds(List.of("stop-1", "stop-2"));

        assertEquals("source-1", alert.getSourceAlertId());
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals("Line disruption", alert.getTitle());
        assertEquals("Signal issue", alert.getDescription());
        assertEquals("RER-A", alert.getLineCode());
        assertEquals(2, alert.getAffectedStopIds().size());
        assertThrows(UnsupportedOperationException.class, () -> alert.getAffectedStopIds().add("stop-3"));
    }

    @Test
    void journeyActivity_roundTripConstructorsAndSetters() {
        UUID userId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        OffsetDateTime recorded = OffsetDateTime.parse("2026-02-14T09:00:00Z");

        JourneyActivity activity = new JourneyActivity(userId, journeyId, "Paris", "Nanterre", 12000, 2.7, recorded);
        assertEquals(userId, activity.getUserId());
        assertEquals(journeyId, activity.getJourneyId());
        assertEquals("Paris", activity.getOrigin());
        assertEquals("Nanterre", activity.getDestination());
        assertEquals(12000, activity.getDistanceMeters());
        assertEquals(2.7, activity.getCo2SavedKg());
        assertEquals(recorded, activity.getRecordedAt());

        activity.setOrigin("A");
        activity.setDestination("B");
        activity.setDistanceMeters(1000);
        activity.setCo2SavedKg(0.2);
        assertEquals("A", activity.getOrigin());
        assertEquals("B", activity.getDestination());
        assertEquals(1000, activity.getDistanceMeters());
        assertEquals(0.2, activity.getCo2SavedKg());
    }

    @Test
    void badgeAndUserBadge_roundTrip() {
        Badge badge = new Badge("Eco Rider", "Use public transport", "leaf");
        badge.setDescription("desc");
        badge.setIcon("icon");
        assertEquals("Eco Rider", badge.getName());
        assertEquals("desc", badge.getDescription());
        assertEquals("icon", badge.getIcon());

        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();
        OffsetDateTime earnedAt = OffsetDateTime.parse("2026-02-14T09:30:00Z");
        UserBadge userBadge = new UserBadge(userId, badgeId, earnedAt);
        userBadge.setBadge(badge);
        assertEquals(userId, userBadge.getUserId());
        assertEquals(badgeId, userBadge.getBadgeId());
        assertEquals(earnedAt, userBadge.getEarnedAt());
        assertEquals(badge, userBadge.getBadge());
    }

    @Test
    void passwordResetToken_defaultsToUnused_andCanBeMarkedUsed() {
        User user = new User("ext-user", "user@example.com", "User");
        PasswordResetToken token = new PasswordResetToken(
                "token-123",
                user,
                OffsetDateTime.parse("2026-02-15T00:00:00Z"));

        assertEquals("token-123", token.getToken());
        assertEquals(user, token.getUser());
        assertFalse(token.isUsed());
        token.setUsed(true);
        assertTrue(token.isUsed());
    }

    @Test
    void userTask_roundTripFields() {
        User user = new User("ext-user", "user@example.com", "User");
        UserTask task = new UserTask(user, "src-1", TaskSource.GOOGLE_TASKS, "Buy milk");
        task.setNotes("Low fat");
        task.setDueAt(OffsetDateTime.parse("2026-02-14T18:00:00Z"));
        task.setCompleted(true);
        task.setLocationHint(new GeoPoint(48.8566, 2.3522));
        task.setLocationQuery("Chatelet");
        task.setLastSyncedAt(OffsetDateTime.parse("2026-02-14T17:30:00Z"));

        assertEquals(user, task.getUser());
        assertEquals("src-1", task.getSourceTaskId());
        assertEquals(TaskSource.GOOGLE_TASKS, task.getSource());
        assertEquals("Buy milk", task.getTitle());
        assertEquals("Low fat", task.getNotes());
        assertTrue(task.isCompleted());
        assertEquals("Chatelet", task.getLocationQuery());
        assertNotNull(task.getLocationHint());
    }
}
