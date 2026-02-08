package org.marly.mavigo.controller.dto;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.user.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JourneyResponseTest {

    private void setEntityId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void from_ShouldMapJourneyToResponse() {
        User user = new User("ext-1", "test@test.com", "Test User");
        UUID userId = UUID.randomUUID();
        setEntityId(user, userId);

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        UUID journeyId = UUID.randomUUID();
        setEntityId(journey, journeyId);
        journey.setOriginCoordinate(new GeoPoint(48.85, 2.35));
        journey.setDestinationCoordinate(new GeoPoint(45.75, 4.85));

        JourneyResponse response = JourneyResponse.from(journey);

        assertEquals(journeyId, response.journeyId());
        assertEquals("Paris", response.originLabel());
        assertEquals("Lyon", response.destinationLabel());
        assertEquals(userId, response.userId());
    }

    @Test
    void from_ShouldMapJourneyWithLegs() {
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, UUID.randomUUID());

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, UUID.randomUUID());

        Leg leg = new Leg();
        leg.setSequenceOrder(1);
        leg.setMode(TransitMode.METRO);
        leg.setLineCode("M1");
        leg.setOriginLabel("Gare de Lyon");
        leg.setDestinationLabel("Nation");
        leg.setOriginCoordinate(new GeoPoint(48.85, 2.35));
        leg.setDestinationCoordinate(new GeoPoint(48.84, 2.40));
        journey.addLeg(leg);

        JourneyResponse response = JourneyResponse.from(journey);

        assertNotNull(response.legs());
        assertEquals(1, response.legs().size());
        assertEquals("M1", response.legs().get(0).lineCode());
    }

    @Test
    void from_ShouldMapJourneyWithTasksOnRoute() {
        User user = new User("ext-1", "test@test.com", "Test User");
        UUID userId = UUID.randomUUID();
        setEntityId(user, userId);

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, UUID.randomUUID());

        UserTask task = new UserTask(user, "task-1", TaskSource.GOOGLE_TASKS, "Buy milk");
        setEntityId(task, UUID.randomUUID());
        task.setLocationHint(new GeoPoint(48.85, 2.35));

        JourneyResponse.TaskOnRouteResponse taskResponse = JourneyResponse.fromTask(task, 100.0);
        List<JourneyResponse.TaskOnRouteResponse> tasksOnRoute = List.of(taskResponse);

        JourneyResponse response = JourneyResponse.from(journey, tasksOnRoute);

        assertNotNull(response.tasksOnRoute());
        assertEquals(1, response.tasksOnRoute().size());
        assertEquals("Buy milk", response.tasksOnRoute().get(0).title());
        assertEquals(100.0, response.tasksOnRoute().get(0).distanceMeters());
    }

    @Test
    void from_ShouldHandleNullUser() {
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());

        JourneyResponse response = JourneyResponse.from(journey);

        assertNull(response.userId());
    }

    @Test
    void fromTask_ShouldMapTaskToResponse() {
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, UUID.randomUUID());

        UserTask task = new UserTask(user, "task-1", TaskSource.GOOGLE_TASKS, "Pick up package");
        UUID taskId = UUID.randomUUID();
        setEntityId(task, taskId);
        task.setLocationHint(new GeoPoint(48.85, 2.35));

        JourneyResponse.TaskOnRouteResponse response = JourneyResponse.fromTask(task, 50.0);

        assertEquals(taskId, response.taskId());
        assertEquals("Pick up package", response.title());
        assertEquals(48.85, response.locationLat());
        assertEquals(2.35, response.locationLng());
        assertEquals(50.0, response.distanceMeters());
    }

    @Test
    void legResponse_ShouldContainAllFields() {
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, UUID.randomUUID());

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, UUID.randomUUID());

        Leg leg = new Leg();
        leg.setSequenceOrder(1);
        leg.setMode(TransitMode.METRO);
        leg.setLineCode("M14");
        leg.setOriginLabel("Gare de Lyon");
        leg.setDestinationLabel("Bercy");
        leg.setOriginCoordinate(new GeoPoint(48.85, 2.35));
        leg.setDestinationCoordinate(new GeoPoint(48.84, 2.38));
        leg.setDurationSeconds(600);
        leg.setEstimatedDeparture(OffsetDateTime.now());
        leg.setEstimatedArrival(OffsetDateTime.now().plusMinutes(10));
        journey.addLeg(leg);

        JourneyResponse response = JourneyResponse.from(journey);

        JourneyResponse.LegResponse legResponse = response.legs().get(0);
        assertEquals(1, legResponse.sequenceOrder());
        assertEquals(TransitMode.METRO, legResponse.mode());
        assertEquals("M14", legResponse.lineCode());
        assertEquals("Gare de Lyon", legResponse.originLabel());
        assertEquals("Bercy", legResponse.destinationLabel());
    }

    @Test
    void from_ShouldHandleEmptyLegs() {
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, UUID.randomUUID());

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, UUID.randomUUID());

        JourneyResponse response = JourneyResponse.from(journey);

        assertNotNull(response.legs());
        assertTrue(response.legs().isEmpty());
    }

    @Test
    void from_ShouldIncludeDisruptionCount() {
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, UUID.randomUUID());

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, UUID.randomUUID());

        JourneyResponse response = JourneyResponse.from(journey);

        assertEquals(0, response.disruptionCount());
    }

    @Test
    void from_ShouldMapJourneyStatus() {
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, UUID.randomUUID());

        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, UUID.randomUUID());
        journey.setStatus(JourneyStatus.IN_PROGRESS);

        JourneyResponse response = JourneyResponse.from(journey);

        assertEquals("IN_PROGRESS", response.status());
    }
}
