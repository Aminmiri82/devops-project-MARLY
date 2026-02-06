package org.marly.mavigo.service.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.shared.GeoPoint;
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
class JourneyOptimizationServiceTest {

        @Mock
        private JourneyPlanningService journeyPlanningService;
        @Mock
        private JourneyRepository journeyRepository;
        @Mock
        private UserTaskRepository userTaskRepository;

        @InjectMocks
        private JourneyOptimizationService journeyOptimizationService;

        private User user;
        private JourneyPlanningParameters params;

        @BeforeEach
        void setUp() {
                user = new User("user-1", "test@example.com", "Test User");
                params = new JourneyPlanningParameters(
                                UUID.randomUUID(), "Origin", "Destination", LocalDateTime.now(),
                                JourneyPreferences.disabled(),
                                false);
        }

        @Test
        void planOptimizedJourneyWithTasksReturnsEmptyWhenNoTasks() {
                var results = journeyOptimizationService.planOptimizedJourneyWithTasks(params, List.of());
                assertThat(results).isEmpty();
        }

        @Test
        void planOptimizedJourneyWithTasksReturnsBestCandidate() {
                UUID taskId = UUID.randomUUID();
                UserTask task = mock(UserTask.class);
                when(task.getId()).thenReturn(taskId);
                when(task.getTitle()).thenReturn("Task 1");
                when(task.getLocationHint()).thenReturn(new GeoPoint(48.8, 2.3));

                when(userTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

                // Mock journey results
                Journey baseJourney = new Journey(user, "Origin", "Destination", OffsetDateTime.now(),
                                OffsetDateTime.now().plusMinutes(30));
                Journey seg1 = new Journey(user, "Origin", "TaskLocation", OffsetDateTime.now(),
                                OffsetDateTime.now().plusMinutes(15));
                Journey seg2 = new Journey(user, "TaskLocation", "Destination", OffsetDateTime.now().plusMinutes(15),
                                OffsetDateTime.now().plusMinutes(40));

                // Mocking journeyPlanningService calls
                // Segment 1
                when(journeyPlanningService.planAndPersist(any())).thenReturn(List.of(seg1), List.of(seg2),
                                List.of(baseJourney));

                // Mocking repo save for aggregated journey
                when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);

                var results = journeyOptimizationService.planOptimizedJourneyWithTasks(params, List.of(taskId));

                assertThat(results).isNotEmpty();
                assertThat(results.get(0).totalDurationSeconds()).isGreaterThan(0);
        }
}
