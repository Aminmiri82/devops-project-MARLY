package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.controller.dto.JourneyPreferencesRequest;
import org.marly.mavigo.controller.dto.JourneyResponse;
import org.marly.mavigo.controller.dto.PlanJourneyCommand;
import org.marly.mavigo.controller.dto.PlanJourneyRequest;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.journey.JourneyManagementService;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.TaskOnRouteService;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyControllerTest {

    @Mock
    private JourneyPlanningService journeyPlanningService;
    @Mock
    private UserTaskRepository userTaskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskOnRouteService taskOnRouteService;
    @Mock
    private JourneyManagementService journeyManagementService;

    private JourneyController journeyController;

    @BeforeEach
    void setUp() {
        journeyController = new JourneyController(
            journeyPlanningService,
            userTaskRepository,
            userRepository,
            taskOnRouteService,
            journeyManagementService
        );
    }

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
    void planJourney_ShouldReturnJourneys_WithTasks() {
        // Given
        UUID userId = UUID.randomUUID();
        PlanJourneyRequest req = new PlanJourneyRequest(
            userId, "Paris", "Lyon", "2023-12-01T10:00:00"
        );
        JourneyPreferencesRequest prefs = new JourneyPreferencesRequest(false, false);
        PlanJourneyCommand command = new PlanJourneyCommand(req, prefs);

        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());
        journey.setUser(user);
        
        List<Journey> journeys = List.of(journey);
        when(journeyPlanningService.planAndPersist(any(JourneyPlanningParameters.class))).thenReturn(journeys);
        
        // Mock user tasks logic
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
        when(taskOnRouteService.extractRoutePoints(journey)).thenReturn(Collections.emptyList());
        // densify might return null or empty
        when(taskOnRouteService.densify(any(), anyInt())).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<JourneyResponse>> response = journeyController.planJourney(command);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void planJourney_ShouldHandleISOWithOffset() {
        UUID userId = UUID.randomUUID();
        PlanJourneyRequest req = new PlanJourneyRequest(
            userId, "Paris", "Lyon", "2023-12-01T10:00:00+01:00"
        );
        PlanJourneyCommand command = new PlanJourneyCommand(req, null);

        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());
        journey.setUser(user);
        
        when(journeyPlanningService.planAndPersist(any())).thenReturn(List.of(journey));
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
        when(taskOnRouteService.extractRoutePoints(journey)).thenReturn(Collections.emptyList());
        when(taskOnRouteService.densify(any(), anyInt())).thenReturn(new ArrayList<>());

        ResponseEntity<List<JourneyResponse>> response = journeyController.planJourney(command);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void planJourney_ShouldHandleISOWithoutSeconds() {
        UUID userId = UUID.randomUUID();
        PlanJourneyRequest req = new PlanJourneyRequest(
            userId, "Paris", "Lyon", "2023-12-01T10:00"
        );
        PlanJourneyCommand command = new PlanJourneyCommand(req, null);

        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());
        journey.setUser(user);
        
        when(journeyPlanningService.planAndPersist(any())).thenReturn(List.of(journey));
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
        when(taskOnRouteService.extractRoutePoints(journey)).thenReturn(Collections.emptyList());
        when(taskOnRouteService.densify(any(), anyInt())).thenReturn(new ArrayList<>());

        ResponseEntity<List<JourneyResponse>> response = journeyController.planJourney(command);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void planJourney_ShouldThrowBadRequest_WhenDepartureTimeIsNull() {
        UUID userId = UUID.randomUUID();
        PlanJourneyRequest req = new PlanJourneyRequest(userId, "Paris", "Lyon", null);
        PlanJourneyCommand command = new PlanJourneyCommand(req, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> journeyController.planJourney(command));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void planJourney_ShouldThrowBadRequest_WhenDepartureTimeIsInvalid() {
        UUID userId = UUID.randomUUID();
        PlanJourneyRequest req = new PlanJourneyRequest(userId, "Paris", "Lyon", "invalid-date");
        PlanJourneyCommand command = new PlanJourneyCommand(req, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> journeyController.planJourney(command));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void planJourney_ShouldFilterTasksOnRoute() {
        UUID userId = UUID.randomUUID();
        PlanJourneyRequest req = new PlanJourneyRequest(
            userId, "Paris", "Lyon", "2023-12-01T10:00:00"
        );
        PlanJourneyCommand command = new PlanJourneyCommand(req, null);

        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());
        journey.setUser(user);

        // Create a task with location
        UserTask task = new UserTask(user, "task-1", TaskSource.GOOGLE_TASKS, "Buy milk");
        task.setLocationHint(new GeoPoint(48.85, 2.35));
        task.setCompleted(false);
        setEntityId(task, UUID.randomUUID());

        when(journeyPlanningService.planAndPersist(any())).thenReturn(List.of(journey));
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(task));
        when(taskOnRouteService.extractRoutePoints(journey)).thenReturn(List.of(new GeoPoint(48.85, 2.35)));
        when(taskOnRouteService.densify(any(), anyInt())).thenReturn(List.of(new GeoPoint(48.85, 2.35)));
        when(taskOnRouteService.minDistanceMetersToPolyline(any(), any())).thenReturn(100.0);

        ResponseEntity<List<JourneyResponse>> response = journeyController.planJourney(command);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertFalse(response.getBody().get(0).tasksOnRoute().isEmpty());
    }

    @Test
    void startJourney_ShouldReturnUpdatedJourney() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey journey = new Journey();
        setEntityId(journey, journeyId);
        when(journeyManagementService.startJourney(journeyId)).thenReturn(journey);

        // When
        ResponseEntity<JourneyResponse> response = journeyController.startJourney(journeyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(journeyId, response.getBody().journeyId());
    }

    @Test
    void completeJourney_ShouldReturnUpdatedJourney() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey journey = new Journey();
        setEntityId(journey, journeyId);
        when(journeyManagementService.completeJourney(journeyId)).thenReturn(journey);

        // When
        ResponseEntity<JourneyResponse> response = journeyController.completeJourney(journeyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(journeyId, response.getBody().journeyId());
    }

    @Test
    void cancelJourney_ShouldReturnUpdatedJourney() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey journey = new Journey();
        setEntityId(journey, journeyId);
        when(journeyManagementService.cancelJourney(journeyId)).thenReturn(journey);

        // When
        ResponseEntity<JourneyResponse> response = journeyController.cancelJourney(journeyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(journeyId, response.getBody().journeyId());
    }

    @Test
    void getJourney_ShouldReturnJourney() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey journey = new Journey();
        setEntityId(journey, journeyId);
        when(journeyManagementService.getJourney(journeyId)).thenReturn(journey);

        // When
        ResponseEntity<JourneyResponse> response = journeyController.getJourney(journeyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(journeyId, response.getBody().journeyId());
    }

    // Debug endpoints tests

    @Test
    void debugUserTasks_ShouldReturnTaskList() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);
        
        UserTask task = new UserTask(user, "task-1", TaskSource.GOOGLE_TASKS, "Buy milk");
        task.setLocationHint(new GeoPoint(48.85, 2.35));
        setEntityId(task, UUID.randomUUID());

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(task));

        Map<String, Object> result = journeyController.debugUserTasks(userId);

        assertEquals(userId.toString(), result.get("userId"));
        assertEquals(1, result.get("taskCount"));
        assertNotNull(result.get("tasks"));
    }

    @Test
    void debugUserTasks_ShouldHandleEmptyTasks() {
        UUID userId = UUID.randomUUID();

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());

        Map<String, Object> result = journeyController.debugUserTasks(userId);

        assertEquals(0, result.get("taskCount"));
    }

    @Test
    void seedTaskNearGareDeLyon_ShouldCreateTask() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);

        JourneyController.SeedTaskRequest request = new JourneyController.SeedTaskRequest(userId, "Test Task");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userTaskRepository.save(any(UserTask.class))).thenAnswer(inv -> {
            UserTask t = inv.getArgument(0);
            setEntityId(t, UUID.randomUUID());
            return t;
        });

        Map<String, Object> result = journeyController.seedTaskNearGareDeLyon(request);

        assertTrue((Boolean) result.get("seeded"));
        assertEquals("Test Task", result.get("title"));
        assertEquals(48.8443, result.get("lat"));
    }

    @Test
    void seedTaskNearGareDeLyon_ShouldUseDefaultTitle() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-id", "test@example.com", "Test User");
        setEntityId(user, userId);

        JourneyController.SeedTaskRequest request = new JourneyController.SeedTaskRequest(userId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userTaskRepository.save(any(UserTask.class))).thenAnswer(inv -> {
            UserTask t = inv.getArgument(0);
            setEntityId(t, UUID.randomUUID());
            return t;
        });

        Map<String, Object> result = journeyController.seedTaskNearGareDeLyon(request);

        assertEquals("Acheter du lait", result.get("title"));
    }

    @Test
    void seedTaskNearGareDeLyon_ShouldThrowNotFound_WhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        JourneyController.SeedTaskRequest request = new JourneyController.SeedTaskRequest(userId, "Task");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> journeyController.seedTaskNearGareDeLyon(request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void seedTaskNearGareDeLyon_ShouldThrowBadRequest_WhenRequestIsNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> journeyController.seedTaskNearGareDeLyon(null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void seedTaskNearGareDeLyon_ShouldThrowBadRequest_WhenUserIdIsNull() {
        JourneyController.SeedTaskRequest request = new JourneyController.SeedTaskRequest(null, "Task");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> journeyController.seedTaskNearGareDeLyon(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}

