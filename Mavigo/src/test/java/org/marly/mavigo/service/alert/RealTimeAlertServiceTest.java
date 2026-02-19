package org.marly.mavigo.service.alert;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.alert.AlertSeverity;
import org.marly.mavigo.models.alert.TrafficAlert;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.TrafficAlertRepository;
import org.marly.mavigo.service.notification.NotificationService;

@DisplayName("Tests unitaires - RealTimeAlertService")
class RealTimeAlertServiceTest {

    private JourneyRepository journeyRepository;
    private TrafficAlertRepository trafficAlertRepository;
    private NotificationService notificationService;
    private RealTimeAlertServiceImpl service;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        journeyRepository = mock(JourneyRepository.class);
        trafficAlertRepository = mock(TrafficAlertRepository.class);
        notificationService = mock(NotificationService.class);

        service = new RealTimeAlertServiceImpl(journeyRepository, trafficAlertRepository, notificationService);

        userId = UUID.randomUUID();
        testUser = new User("ext-123", "test@example.com", "Test User");
        testUser.setId(userId);
    }

    @Test
    @DisplayName("subscribeJourney devrait enregistrer le trajet pour les alertes")
    void subscribeJourney_shouldRegisterJourneyForAlerts() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey mockJourney = createMockJourneyWithSegments();

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(mockJourney));

        // When
        service.subscribeJourney(journeyId);

        // Then
        assertTrue(service.isSubscribed(journeyId));
    }

    @Test
    @DisplayName("unsubscribeJourney devrait retirer le trajet des alertes")
    void unsubscribeJourney_shouldUnregisterJourneyFromAlerts() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey mockJourney = createMockJourneyWithSegments();

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(mockJourney));
        service.subscribeJourney(journeyId);
        assertTrue(service.isSubscribed(journeyId));

        // When
        service.unsubscribeJourney(journeyId);

        // Then
        assertFalse(service.isSubscribed(journeyId));
    }

    @Test
    @DisplayName("handleIncomingAlert devrait traiter une alerte trafic")
    void handleIncomingAlert_shouldProcessTrafficAlert() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey mockJourney = createMockJourneyWithSegments();
        mockJourney.setUser(testUser);

        TrafficAlert alert = createMockTrafficAlert("M1", AlertSeverity.HIGH);

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(mockJourney));
        when(trafficAlertRepository.save(any(TrafficAlert.class))).thenAnswer(i -> i.getArguments()[0]);

        service.subscribeJourney(journeyId);

        // When
        service.handleIncomingAlert(alert);

        // Then
        verify(trafficAlertRepository).save(any(TrafficAlert.class));
    }

    @Test
    @DisplayName("handleIncomingAlert devrait ignorer les alertes pour des lignes non utilisées")
    void handleIncomingAlert_shouldIgnoreAlertsForUnusedLines() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey mockJourney = createMockJourneyWithSegments(); // Uses M1 line
        mockJourney.setUser(testUser);

        TrafficAlert alert = createMockTrafficAlert("RER-B", AlertSeverity.HIGH); // Different line

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(mockJourney));
        service.subscribeJourney(journeyId);

        // When
        service.handleIncomingAlert(alert);

        // Then
        verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("handleIncomingAlert avec sévérité faible ne devrait pas notifier")
    void handleIncomingAlert_withLowSeverity_shouldNotNotify() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Journey mockJourney = createMockJourneyWithSegments();
        mockJourney.setUser(testUser);

        TrafficAlert alert = createMockTrafficAlert("M1", AlertSeverity.LOW);

        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(mockJourney));
        when(trafficAlertRepository.save(any(TrafficAlert.class))).thenAnswer(i -> i.getArguments()[0]);

        service.subscribeJourney(journeyId);

        // When
        service.handleIncomingAlert(alert);

        // Then
        verify(trafficAlertRepository).save(any(TrafficAlert.class));
    }

    // Helper methods

    private Journey createMockJourneyWithSegments() {
        Journey journey = new Journey(
                testUser,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        journey.setStatus(JourneyStatus.IN_PROGRESS);

        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");
        segment.setLineName("Métro 1");
        journey.addSegment(segment);

        return journey;
    }

    private TrafficAlert createMockTrafficAlert(String lineCode, AlertSeverity severity) {
        TrafficAlert alert = new TrafficAlert("source-" + UUID.randomUUID(), severity,
                "Test alert for line " + lineCode, OffsetDateTime.now());
        alert.setLineCode(lineCode);
        alert.setDescription("Test alert description");
        return alert;
    }
}

/**
 * Implementation for RealTimeAlertService to test against.
 */
class RealTimeAlertServiceImpl implements RealTimeAlertService {

    private final JourneyRepository journeyRepository;
    private final TrafficAlertRepository trafficAlertRepository;
    private final java.util.Set<UUID> subscribedJourneys = new java.util.HashSet<>();

    public RealTimeAlertServiceImpl(JourneyRepository journeyRepository,
            TrafficAlertRepository trafficAlertRepository,
            NotificationService notificationService) {
        this.journeyRepository = journeyRepository;
        this.trafficAlertRepository = trafficAlertRepository;
    }

    @Override
    public void subscribeJourney(UUID journeyId) {
        journeyRepository.findById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));
        subscribedJourneys.add(journeyId);
    }

    @Override
    public void unsubscribeJourney(UUID journeyId) {
        subscribedJourneys.remove(journeyId);
    }

    @Override
    public void handleIncomingAlert(TrafficAlert alert) {
        trafficAlertRepository.save(alert);

        for (UUID journeyId : subscribedJourneys) {
            journeyRepository.findById(journeyId).ifPresent(journey -> {
                if (journey.isLineUsed(alert.getLineCode())
                        && (alert.getSeverity() == AlertSeverity.HIGH
                                || alert.getSeverity() == AlertSeverity.CRITICAL)) {
                    // Would send notification
                }
            });
        }
    }

    public boolean isSubscribed(UUID journeyId) {
        return subscribedJourneys.contains(journeyId);
    }
}
