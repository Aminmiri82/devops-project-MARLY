package org.marly.mavigo.service.journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyManagementServiceImplTest {

    @Mock
    private JourneyRepository journeyRepository;

    private JourneyManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JourneyManagementServiceImpl(journeyRepository);
    }

    private Journey createTestJourney(UUID id, JourneyStatus status) {
        User user = new User("ext-1", "test@test.com", "Test User");
        Journey journey = new Journey(user, "Paris", "Lyon", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
        setEntityId(journey, id);
        journey.setStatus(status);
        return journey;
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

    // ========== startJourney tests ==========

    @Test
    void startJourney_ShouldStartPlannedJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.PLANNED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        Journey result = service.startJourney(journeyId);

        assertEquals(JourneyStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getActualDeparture());
    }

    @Test
    void startJourney_ShouldStartReroutedJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.REROUTED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        Journey result = service.startJourney(journeyId);

        assertEquals(JourneyStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void startJourney_ShouldThrowWhenAlreadyInProgress() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.IN_PROGRESS);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.startJourney(journeyId));
    }

    @Test
    void startJourney_ShouldThrowWhenCompleted() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.COMPLETED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.startJourney(journeyId));
    }

    @Test
    void startJourney_ShouldThrowWhenCancelled() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.CANCELLED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.startJourney(journeyId));
    }

    // ========== completeJourney tests ==========

    @Test
    void completeJourney_ShouldCompleteInProgressJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.IN_PROGRESS);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        Journey result = service.completeJourney(journeyId);

        assertEquals(JourneyStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getActualArrival());
    }

    @Test
    void completeJourney_ShouldCompleteReroutedJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.REROUTED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        Journey result = service.completeJourney(journeyId);

        assertEquals(JourneyStatus.COMPLETED, result.getStatus());
    }

    @Test
    void completeJourney_ShouldThrowWhenPlanned() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.PLANNED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.completeJourney(journeyId));
    }

    @Test
    void completeJourney_ShouldThrowWhenAlreadyCompleted() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.COMPLETED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.completeJourney(journeyId));
    }

    // ========== cancelJourney tests ==========

    @Test
    void cancelJourney_ShouldCancelPlannedJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.PLANNED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        Journey result = service.cancelJourney(journeyId);

        assertEquals(JourneyStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelJourney_ShouldCancelInProgressJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.IN_PROGRESS);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        Journey result = service.cancelJourney(journeyId);

        assertEquals(JourneyStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelJourney_ShouldThrowWhenAlreadyCompleted() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.COMPLETED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.cancelJourney(journeyId));
    }

    @Test
    void cancelJourney_ShouldThrowWhenAlreadyCancelled() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.CANCELLED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.cancelJourney(journeyId));
    }

    // ========== getJourney tests ==========

    @Test
    void getJourney_ShouldReturnJourney() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = createTestJourney(journeyId, JourneyStatus.PLANNED);

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(journey));

        Journey result = service.getJourney(journeyId);

        assertEquals(journeyId, result.getId());
    }

    @Test
    void getJourney_ShouldThrowWhenNotFound() {
        UUID journeyId = UUID.randomUUID();

        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getJourney(journeyId));
    }
}