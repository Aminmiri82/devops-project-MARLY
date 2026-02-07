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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
}
