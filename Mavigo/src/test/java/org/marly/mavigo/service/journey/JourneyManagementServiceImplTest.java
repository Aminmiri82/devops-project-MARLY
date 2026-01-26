package org.marly.mavigo.service.journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.mockito.InjectMocks;
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
@DisplayName("Tests unitaires - JourneyManagementService")
class JourneyManagementServiceImplTest {

    @Mock
    private JourneyRepository journeyRepository;

    @InjectMocks
    private JourneyManagementServiceImpl journeyManagementService;

    private Journey testJourney;
    private UUID journeyId;
    private User testUser;

    @BeforeEach
    void setUp() {
        journeyId = UUID.randomUUID();
        testUser = new User("ext-123", "test@example.com", "Test User");

        testJourney = new Journey(
                testUser,
                "Gare du Nord",
                "Tour Eiffel",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1)
        );
        testJourney.setStatus(JourneyStatus.PLANNED);
    }

    // ============================================
    // TESTS - startJourney()
    // ============================================

    @Test
    @DisplayName("Démarrer un trajet PLANNED doit passer à IN_PROGRESS")
    void startJourney_shouldUpdateStatusAndTimestamp() {
        // Given
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));
        when(journeyRepository.save(any(Journey.class))).thenReturn(testJourney);

        // When
        Journey result = journeyManagementService.startJourney(journeyId);

        // Then
        assertEquals(JourneyStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getActualDeparture());
        verify(journeyRepository).save(testJourney);
    }

    @Test
    @DisplayName("Démarrer un trajet REROUTED doit passer à IN_PROGRESS")
    void testStartJourney_WithReroutedStatus_ShouldStartSuccessfully() {
        // Given
        testJourney.setStatus(JourneyStatus.REROUTED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));
        when(journeyRepository.save(any(Journey.class))).thenReturn(testJourney);

        // When
        Journey result = journeyManagementService.startJourney(journeyId);

        // Then
        assertEquals(JourneyStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getActualDeparture());
    }

    @Test
    @DisplayName("Démarrer un trajet déjà IN_PROGRESS doit lever une exception")
    void startJourney_shouldThrowIfAlreadyStarted() {
        // Given
        testJourney.setStatus(JourneyStatus.IN_PROGRESS);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            journeyManagementService.startJourney(journeyId);
        });
        verify(journeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Démarrer un trajet COMPLETED doit lever une exception")
    void testStartJourney_WithCompletedStatus_ShouldThrowException() {
        // Given
        testJourney.setStatus(JourneyStatus.COMPLETED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            journeyManagementService.startJourney(journeyId);
        });
    }

    @Test
    @DisplayName("Démarrer un trajet CANCELLED doit lever une exception")
    void testStartJourney_WithCancelledStatus_ShouldThrowException() {
        // Given
        testJourney.setStatus(JourneyStatus.CANCELLED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            journeyManagementService.startJourney(journeyId);
        });
    }

    @Test
    @DisplayName("Démarrer un trajet inexistant doit lever EntityNotFoundException")
    void testStartJourney_WithInvalidId_ShouldThrowEntityNotFoundException() {
        // Given
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            journeyManagementService.startJourney(journeyId);
        });
    }

    // ============================================
    // TESTS - completeJourney()
    // ============================================

    @Test
    @DisplayName("Compléter un trajet IN_PROGRESS doit passer à COMPLETED")
    void completeJourney_shouldUpdateStatusAndTimestamp() {
        // Given
        testJourney.setStatus(JourneyStatus.IN_PROGRESS);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));
        when(journeyRepository.save(any(Journey.class))).thenReturn(testJourney);

        // When
        Journey result = journeyManagementService.completeJourney(journeyId);

        // Then
        assertEquals(JourneyStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getActualArrival());
    }

    @Test
    @DisplayName("Compléter un trajet REROUTED doit passer à COMPLETED")
    void testCompleteJourney_WithReroutedStatus_ShouldCompleteSuccessfully() {
        // Given
        testJourney.setStatus(JourneyStatus.REROUTED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));
        when(journeyRepository.save(any(Journey.class))).thenReturn(testJourney);

        // When
        Journey result = journeyManagementService.completeJourney(journeyId);

        // Then
        assertEquals(JourneyStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getActualArrival());
    }

    @Test
    @DisplayName("Compléter un trajet PLANNED doit lever une exception")
    void testCompleteJourney_WithPlannedStatus_ShouldThrowException() {
        // Given
        testJourney.setStatus(JourneyStatus.PLANNED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            journeyManagementService.completeJourney(journeyId);
        });
    }

    // ============================================
    // TESTS - cancelJourney()
    // ============================================

    @Test
    @DisplayName("Annuler un trajet PLANNED doit passer à CANCELLED")
    void cancelJourney_shouldUpdateStatus() {
        // Given
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));
        when(journeyRepository.save(any(Journey.class))).thenReturn(testJourney);

        // When
        Journey result = journeyManagementService.cancelJourney(journeyId);

        // Then
        assertEquals(JourneyStatus.CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("Annuler un trajet IN_PROGRESS doit passer à CANCELLED")
    void testCancelJourney_WithInProgressStatus_ShouldCancelSuccessfully() {
        // Given
        testJourney.setStatus(JourneyStatus.IN_PROGRESS);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));
        when(journeyRepository.save(any(Journey.class))).thenReturn(testJourney);

        // When
        Journey result = journeyManagementService.cancelJourney(journeyId);

        // Then
        assertEquals(JourneyStatus.CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("Annuler un trajet COMPLETED doit lever une exception")
    void testCancelJourney_WithCompletedStatus_ShouldThrowException() {
        // Given
        testJourney.setStatus(JourneyStatus.COMPLETED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            journeyManagementService.cancelJourney(journeyId);
        });
    }

    @Test
    @DisplayName("Annuler un trajet déjà CANCELLED doit lever une exception")
    void testCancelJourney_WithCancelledStatus_ShouldThrowException() {
        // Given
        testJourney.setStatus(JourneyStatus.CANCELLED);
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            journeyManagementService.cancelJourney(journeyId);
        });
    }

    // ============================================
    // TESTS - getJourney()
    // ============================================

    @Test
    @DisplayName("Récupérer un trajet existant doit retourner le trajet")
    void testGetJourney_WithValidId_ShouldReturnJourney() {
        // Given
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(testJourney));

        // When
        Journey result = journeyManagementService.getJourney(journeyId);

        // Then
        assertNotNull(result);
        assertEquals(testJourney, result);
        assertEquals("Gare du Nord", result.getOriginLabel());
        assertEquals("Tour Eiffel", result.getDestinationLabel());
    }

    @Test
    @DisplayName("Récupérer un trajet inexistant doit lever EntityNotFoundException")
    void testGetJourney_WithInvalidId_ShouldThrowEntityNotFoundException() {
        // Given
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            journeyManagementService.getJourney(journeyId);
        });
    }
}