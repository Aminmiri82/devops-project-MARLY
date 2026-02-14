package org.marly.mavigo.service.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.controller.dto.TaskDetailDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JourneyOptimizationServiceAdvancedTest {

    @Mock
    private JourneyPlanningService journeyPlanningService;
    @Mock
    private JourneyRepository journeyRepository;
    @Mock
    private UserTaskRepository userTaskRepository;

    @InjectMocks
    private JourneyOptimizationService service;

    private JourneyPlanningParameters parameters;
    private User user;

    @BeforeEach
    void setUp() {
        parameters = new JourneyPlanningParameters(
                UUID.randomUUID(),
                "Origin",
                "Destination",
                LocalDateTime.of(2026, 2, 14, 9, 0),
                JourneyPreferences.disabled(),
                false,
                false);
        user = new User("ext-user", "user@example.com", "User");
    }

    @Test
    void planOptimizedJourneyWithTaskDetails_returnsEmptyWhenAllTaskDetailsAreInvalid() {
        List<TaskDetailDto> taskDetails = List.of(
                new TaskDetailDto(null, "missing-id", "A", 48.8, 2.3, false),
                new TaskDetailDto("t2", "completed", "B", 48.8, 2.3, true),
                new TaskDetailDto("t3", "missing-coords", "C", null, 2.3, false));

        List<JourneyOptimizationService.OptimizedJourneyResult> results = service
                .planOptimizedJourneyWithTaskDetails(parameters, taskDetails);

        assertTrue(results.isEmpty());
    }

    @Test
    void planOptimizedJourneyWithTasks_ignoresCompletedAndMissingLocationTasks() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();

        UserTask completed = new UserTask(user, "src-1", TaskSource.MANUAL, "Completed");
        completed.setCompleted(true);
        completed.setLocationHint(new GeoPoint(48.8, 2.3));

        UserTask noLocation = new UserTask(user, "src-2", TaskSource.MANUAL, "NoLocation");
        noLocation.setCompleted(false);
        noLocation.setLocationHint(null);

        when(userTaskRepository.findById(t1)).thenReturn(Optional.of(completed));
        when(userTaskRepository.findById(t2)).thenReturn(Optional.of(noLocation));

        List<JourneyOptimizationService.OptimizedJourneyResult> results = service
                .planOptimizedJourneyWithTasks(parameters, List.of(t1, t2));

        assertTrue(results.isEmpty());
        verify(journeyPlanningService, times(0)).planAndPersist(any());
    }

    @Test
    void planOptimizedJourneyWithTaskDetails_usesBaseDurationFallbackAndCopiesSegmentsAndPoints() {
        TaskDetailDto task = new TaskDetailDto("task-1", "Groceries", "Task Place", 48.8568, 2.3530, false);
        Journey seg1 = journeyWithSingleSegment("Origin", "Task Place", 900, 48.8566, 2.3522);
        Journey seg2 = journeyWithSingleSegment("Task Place", "Destination", 1200, 48.8570, 2.3600);

        when(journeyPlanningService.planAndPersist(any()))
                .thenReturn(List.of(seg1), List.of(seg2), List.of(), List.of());
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        List<JourneyOptimizationService.OptimizedJourneyResult> results = service
                .planOptimizedJourneyWithTaskDetails(parameters, List.of(task));

        assertEquals(1, results.size());
        JourneyOptimizationService.OptimizedJourneyResult result = results.get(0);
        assertEquals(result.totalDurationSeconds(), result.baseDurationSeconds());
        assertEquals(1, result.includedTasks().size());
        assertEquals("task-1", result.includedTasks().get(0).id());
        assertNotNull(result.journey());
        assertEquals(2, result.journey().getSegments().size());
        assertTrue(result.journey().getSegments().stream().allMatch(s -> !s.getPoints().isEmpty()));
    }

    @Test
    void planOptimizedJourneyWithTaskDetails_capsReturnedPathsToFive() {
        TaskDetailDto task = new TaskDetailDto("task-2", "Pickup", "Task Place", 48.8568, 2.3530, false);
        Journey seg1 = journeyWithSingleSegment("Origin", "Task Place", 600, 48.8566, 2.3522);
        Journey seg2 = journeyWithSingleSegment("Task Place", "Destination", 900, 48.8570, 2.3600);
        Journey base = journeyWithSingleSegment("Origin", "Destination", 1000, 48.8566, 2.3522);

        List<Journey> sixOptions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixOptions.add(journeyWithSingleSegment("Origin", "Task Place", 600 + i, 48.8566, 2.3522));
        }

        when(journeyPlanningService.planAndPersist(any())).thenReturn(
                List.of(seg1),  // calculateJourneyWithTaskOpt segment1
                List.of(seg2),  // calculateJourneyWithTaskOpt segment2
                List.of(base),  // base journey
                sixOptions,     // calculateAllPathsForTaskOpt segment1
                List.of(seg2),  // path 1 segment2
                List.of(seg2),  // path 2 segment2
                List.of(seg2),  // path 3 segment2
                List.of(seg2),  // path 4 segment2
                List.of(seg2)); // path 5 segment2

        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        List<JourneyOptimizationService.OptimizedJourneyResult> results = service
                .planOptimizedJourneyWithTaskDetails(parameters, List.of(task));

        assertEquals(5, results.size());
        verify(journeyPlanningService, times(9)).planAndPersist(any());
    }

    @Test
    void planOptimizedJourneyWithTaskDetails_returnsEmptyWhenCandidateComputationFails() {
        TaskDetailDto task = new TaskDetailDto("task-3", "Broken", "Task Place", 48.8568, 2.3530, false);

        when(journeyPlanningService.planAndPersist(any()))
                .thenThrow(new RuntimeException("planner failed"));

        List<JourneyOptimizationService.OptimizedJourneyResult> results = service
                .planOptimizedJourneyWithTaskDetails(parameters, List.of(task));

        assertTrue(results.isEmpty());
    }

    private Journey journeyWithSingleSegment(String origin, String destination, long durationSeconds, double lat,
            double lon) {
        OffsetDateTime departure = OffsetDateTime.parse("2026-02-14T09:00:00Z");
        OffsetDateTime arrival = departure.plusSeconds(durationSeconds);
        Journey journey = new Journey(user, origin, destination, departure, arrival);
        JourneySegment segment = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        segment.setDurationSeconds((int) durationSeconds);
        JourneyPoint point = new JourneyPoint(segment, 0, JourneyPointType.INTERMEDIATE_STOP, origin + " stop");
        point.setCoordinates(new GeoPoint(lat, lon));
        segment.addPoint(point);
        journey.addSegment(segment);
        return journey;
    }
}
