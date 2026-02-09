package org.marly.mavigo.service.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.tracking.GamificationService;
import java.util.Collections;

class JourneyManagementServiceImplTest {

    private JourneyRepository journeyRepository;
    private GamificationService gamificationService;
    private JourneyManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        journeyRepository = mock(JourneyRepository.class);
        gamificationService = mock(GamificationService.class);
        service = new JourneyManagementServiceImpl(journeyRepository, gamificationService);
    }

    @Test
    void startJourney_shouldUpdateStatusAndTimestamp() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setUser(mock(User.class));
        journey.setStatus(JourneyStatus.PLANNED);

        when(journeyRepository.findWithSegmentsById(any(UUID.class))).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);

        JourneyActionResult result = service.startJourney(id);

        assertEquals(JourneyStatus.IN_PROGRESS, result.journey().getStatus());
        assertNotNull(result.journey().getActualDeparture());
    }

    @Test
    void startJourney_shouldThrowIfAlreadyStarted() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.IN_PROGRESS);

        when(journeyRepository.findWithSegmentsById(any(UUID.class))).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.startJourney(id));
    }

    @Test
    void completeJourney_shouldUpdateStatusAndTimestamp() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setUser(mock(User.class));
        journey.setStatus(JourneyStatus.IN_PROGRESS);

        when(journeyRepository.findWithSegmentsById(any(UUID.class))).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);
        when(gamificationService.trackActivityAndCheckBadges(any(Journey.class)))
                .thenReturn(Collections.emptyList());

        JourneyActionResult result = service.completeJourney(id);

        assertEquals(JourneyStatus.COMPLETED, result.journey().getStatus());
        assertNotNull(result.journey().getActualArrival());
    }

    @Test
    void cancelJourney_shouldUpdateStatus() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.PLANNED);

        when(journeyRepository.findWithSegmentsById(any(UUID.class))).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);

        Journey result = service.cancelJourney(id);

        assertEquals(JourneyStatus.CANCELLED, result.getStatus());
    }
}
