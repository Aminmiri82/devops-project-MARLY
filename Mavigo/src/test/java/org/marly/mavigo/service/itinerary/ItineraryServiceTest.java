package org.marly.mavigo.service.itinerary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.itinerary.dto.PlanJourneyCommand;
import org.marly.mavigo.service.itinerary.dto.RerouteCommand;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

@DisplayName("Tests unitaires - ItineraryService")
class ItineraryServiceTest {

    private JourneyPlanningService journeyPlanningService;
    private JourneyRepository journeyRepository;
    private ItineraryServiceImpl service;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        journeyPlanningService = mock(JourneyPlanningService.class);
        journeyRepository = mock(JourneyRepository.class);

        service = new ItineraryServiceImpl(journeyPlanningService, journeyRepository);

        userId = UUID.randomUUID();
        testUser = new User("ext-123", "test@example.com", "Test User");
        testUser.setId(userId);
    }

    @Test
    @DisplayName("planJourney devrait créer un nouveau trajet")
    void planJourney_shouldCreateNewJourney() {
        // Given
        GeoPoint origin = new GeoPoint(48.8443, 2.3730);
        GeoPoint destination = new GeoPoint(48.8584, 2.3470);
        PlanJourneyCommand command = new PlanJourneyCommand(
                userId,
                "Gare de Lyon",
                origin,
                "Châtelet",
                destination,
                OffsetDateTime.now().plusHours(1),
                false,
                false);

        Journey mockJourney = createMockJourney();
        when(journeyPlanningService.planAndPersist(any(JourneyPlanningParameters.class)))
                .thenReturn(java.util.List.of(mockJourney));

        // When
        Journey result = service.planJourney(command);

        // Then
        assertNotNull(result);
        verify(journeyPlanningService).planAndPersist(any(JourneyPlanningParameters.class));
    }

    @Test
    @DisplayName("planJourney avec mode confort devrait passer les préférences")
    void planJourney_withComfortMode_shouldPassPreferences() {
        // Given
        GeoPoint origin = new GeoPoint(48.8443, 2.3730);
        GeoPoint destination = new GeoPoint(48.8584, 2.3470);
        PlanJourneyCommand command = new PlanJourneyCommand(
                userId,
                "Gare de Lyon",
                origin,
                "Châtelet",
                destination,
                OffsetDateTime.now().plusHours(1),
                true,
                false);

        Journey mockJourney = createMockJourney();
        mockJourney.setComfortModeEnabled(true);
        when(journeyPlanningService.planAndPersist(any(JourneyPlanningParameters.class)))
                .thenReturn(java.util.List.of(mockJourney));

        // When
        Journey result = service.planJourney(command);

        // Then
        assertNotNull(result);
        assertTrue(result.isComfortModeEnabled());
    }

    @Test
    @DisplayName("rerouteJourney devrait recalculer un trajet existant")
    void rerouteJourney_shouldRecalculateExistingJourney() {
        // Given
        UUID journeyId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        Journey existingJourney = createMockJourney();
        existingJourney.setUser(testUser);

        RerouteCommand command = new RerouteCommand(
                journeyId,
                alertId,
                OffsetDateTime.now(),
                true);

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(existingJourney));
        when(journeyPlanningService.planAndPersist(any(JourneyPlanningParameters.class)))
                .thenReturn(java.util.List.of(createMockJourney()));

        // When
        Journey result = service.rerouteJourney(command);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("getActiveJourney devrait retourner un trajet actif")
    void getActiveJourney_shouldReturnActiveJourney() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey mockJourney = createMockJourney();
        mockJourney.setStatus(JourneyStatus.IN_PROGRESS);

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(mockJourney));

        // When
        Journey result = service.getActiveJourney(journeyId);

        // Then
        assertNotNull(result);
        assertEquals(JourneyStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    @DisplayName("getActiveJourney devrait lever une exception si le trajet n'existe pas")
    void getActiveJourney_shouldThrowExceptionWhenJourneyNotFound() {
        // Given
        UUID journeyId = UUID.randomUUID();
        when(journeyRepository.findById(journeyId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.getActiveJourney(journeyId));
    }

    // Helper methods

    private Journey createMockJourney() {
        return new Journey(
                testUser,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
    }
}

/**
 * Implementation stub for ItineraryService to test against.
 * This is needed since the actual implementation wasn't provided.
 */
class ItineraryServiceImpl implements ItineraryService {

    private final JourneyPlanningService journeyPlanningService;
    private final JourneyRepository journeyRepository;

    public ItineraryServiceImpl(JourneyPlanningService journeyPlanningService, JourneyRepository journeyRepository) {
        this.journeyPlanningService = journeyPlanningService;
        this.journeyRepository = journeyRepository;
    }

    @Override
    public Journey planJourney(PlanJourneyCommand command) {
        JourneyPreferences preferences = new JourneyPreferences(
                command.enableComfortMode(),
                false,
                null);

        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                command.userId(),
                command.originLabel(),
                command.destinationLabel(),
                command.departureTime().toLocalDateTime(),
                preferences,
                false);

        var journeys = journeyPlanningService.planAndPersist(parameters);
        return journeys.isEmpty() ? null : journeys.get(0);
    }

    @Override
    public Journey rerouteJourney(RerouteCommand command) {
        Journey existingJourney = journeyRepository.findById(command.journeyId())
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + command.journeyId()));

        JourneyPreferences preferences = new JourneyPreferences(
                existingJourney.isComfortModeEnabled(),
                existingJourney.isEcoModeEnabled(),
                existingJourney.getNamedComfortSettingId());

        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                existingJourney.getUser().getId(),
                existingJourney.getOriginLabel(),
                existingJourney.getDestinationLabel(),
                LocalDateTime.now(),
                preferences,
                existingJourney.isEcoModeEnabled());

        var journeys = journeyPlanningService.planAndPersist(parameters);
        return journeys.isEmpty() ? null : journeys.get(0);
    }

    @Override
    public Journey getActiveJourney(UUID journeyId) {
        return journeyRepository.findById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));
    }
}
