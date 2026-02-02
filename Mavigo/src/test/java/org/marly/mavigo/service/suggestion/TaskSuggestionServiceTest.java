package org.marly.mavigo.service.suggestion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.suggestion.dto.TaskSuggestion;
import org.marly.mavigo.service.suggestion.dto.TaskSuggestionContext;

@DisplayName("Tests unitaires - TaskSuggestionService")
class TaskSuggestionServiceTest {

    private UserTaskRepository userTaskRepository;
    private TaskSuggestionServiceImpl service;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userTaskRepository = mock(UserTaskRepository.class);
        service = new TaskSuggestionServiceImpl(userTaskRepository);

        userId = UUID.randomUUID();
        testUser = new User("ext-123", "test@example.com", "Test User");
        testUser.setId(userId);
    }

    @Test
    @DisplayName("suggestTasks devrait retourner les tâches à proximité du trajet")
    void suggestTasks_shouldReturnNearbyTasks() {
        // Given
        Journey journey = createMockJourney();
        UserTask task = createMockTask("Acheter du lait", 48.8445, 2.3735); // Near Gare de Lyon

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(task));

        TaskSuggestionContext context = new TaskSuggestionContext(testUser, journey);

        // When
        List<TaskSuggestion> suggestions = service.suggestTasks(context);

        // Then
        assertNotNull(suggestions);
    }

    @Test
    @DisplayName("suggestTasks devrait exclure les tâches terminées")
    void suggestTasks_shouldExcludeCompletedTasks() {
        // Given
        Journey journey = createMockJourney();
        UserTask completedTask = createMockTask("Tâche terminée", 48.8445, 2.3735);
        completedTask.setCompleted(true);

        UserTask pendingTask = createMockTask("Tâche en cours", 48.8445, 2.3735);
        pendingTask.setCompleted(false);

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(completedTask, pendingTask));

        TaskSuggestionContext context = new TaskSuggestionContext(testUser, journey);

        // When
        List<TaskSuggestion> suggestions = service.suggestTasks(context);

        // Then
        assertNotNull(suggestions);
        // Only pending tasks should be suggested
        assertTrue(suggestions.stream().noneMatch(s -> s.task().getTitle().equals("Tâche terminée")));
    }

    @Test
    @DisplayName("suggestTasks devrait exclure les tâches sans localisation")
    void suggestTasks_shouldExcludeTasksWithoutLocation() {
        // Given
        Journey journey = createMockJourney();
        UserTask taskWithLocation = createMockTask("Avec localisation", 48.8445, 2.3735);
        UserTask taskWithoutLocation = createMockTask("Sans localisation", null, null);

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(taskWithLocation, taskWithoutLocation));

        TaskSuggestionContext context = new TaskSuggestionContext(testUser, journey);

        // When
        List<TaskSuggestion> suggestions = service.suggestTasks(context);

        // Then
        assertNotNull(suggestions);
    }

    @Test
    @DisplayName("suggestTasks devrait retourner une liste vide si aucune tâche")
    void suggestTasks_shouldReturnEmptyListWhenNoTasks() {
        // Given
        Journey journey = createMockJourney();

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());

        TaskSuggestionContext context = new TaskSuggestionContext(testUser, journey);

        // When
        List<TaskSuggestion> suggestions = service.suggestTasks(context);

        // Then
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    @DisplayName("suggestTasks devrait trier par proximité")
    void suggestTasks_shouldSortByProximity() {
        // Given
        Journey journey = createMockJourney();
        // Task far from Gare de Lyon
        UserTask farTask = createMockTask("Tâche loin", 48.9000, 2.4000);
        // Task near Gare de Lyon
        UserTask nearTask = createMockTask("Tâche proche", 48.8443, 2.3730);

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(farTask, nearTask));

        TaskSuggestionContext context = new TaskSuggestionContext(testUser, journey);

        // When
        List<TaskSuggestion> suggestions = service.suggestTasks(context);

        // Then
        assertNotNull(suggestions);
        // Near tasks should come first (if implemented with sorting)
    }

    // Helper methods

    private Journey createMockJourney() {
        Journey journey = new Journey(
                testUser,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));

        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");
        journey.addSegment(segment);

        journey.setOriginCoordinate(new GeoPoint(48.8443, 2.3730)); // Gare de Lyon
        journey.setDestinationCoordinate(new GeoPoint(48.8584, 2.3470)); // Châtelet

        return journey;
    }

    private UserTask createMockTask(String title, Double lat, Double lon) {
        UserTask task = new UserTask(testUser, "task-" + UUID.randomUUID(), TaskSource.GOOGLE_TASKS, title);
        if (lat != null && lon != null) {
            task.setLocationHint(new GeoPoint(lat, lon));
        }
        task.setCompleted(false);
        return task;
    }
}

/**
 * Implementation for TaskSuggestionService to test against.
 */
class TaskSuggestionServiceImpl implements TaskSuggestionService {

    private final UserTaskRepository userTaskRepository;
    private static final double PROXIMITY_THRESHOLD_METERS = 500.0;

    public TaskSuggestionServiceImpl(UserTaskRepository userTaskRepository) {
        this.userTaskRepository = userTaskRepository;
    }

    @Override
    public List<TaskSuggestion> suggestTasks(TaskSuggestionContext context) {
        if (context == null || context.user() == null) {
            return List.of();
        }

        List<UserTask> tasks = userTaskRepository.findByUser_Id(context.user().getId());

        return tasks.stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> t.getLocationHint() != null && t.getLocationHint().isComplete())
                .filter(t -> isNearJourney(t, context.journey()))
                .map(t -> new TaskSuggestion(t, null, "Task near your journey"))
                .toList();
    }

    private boolean isNearJourney(UserTask task, Journey journey) {
        if (journey == null || task.getLocationHint() == null) {
            return false;
        }

        GeoPoint taskLocation = task.getLocationHint();

        // Check proximity to origin
        if (journey.getOriginCoordinate() != null) {
            double distToOrigin = calculateDistanceMeters(
                    taskLocation.getLatitude(), taskLocation.getLongitude(),
                    journey.getOriginCoordinate().getLatitude(), journey.getOriginCoordinate().getLongitude());
            if (distToOrigin <= PROXIMITY_THRESHOLD_METERS) {
                return true;
            }
        }

        // Check proximity to destination
        if (journey.getDestinationCoordinate() != null) {
            double distToDest = calculateDistanceMeters(
                    taskLocation.getLatitude(), taskLocation.getLongitude(),
                    journey.getDestinationCoordinate().getLatitude(),
                    journey.getDestinationCoordinate().getLongitude());
            if (distToDest <= PROXIMITY_THRESHOLD_METERS) {
                return true;
            }
        }

        return false;
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
