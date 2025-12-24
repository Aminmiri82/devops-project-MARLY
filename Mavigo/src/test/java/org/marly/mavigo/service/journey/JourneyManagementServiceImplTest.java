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
import org.marly.mavigo.repository.JourneyRepository;

class JourneyManagementServiceImplTest {

    private JourneyRepository journeyRepository;
    private JourneyManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        journeyRepository = mock(JourneyRepository.class);
        service = new JourneyManagementServiceImpl(journeyRepository);
    }

    @Test
    void startJourney_shouldUpdateStatusAndTimestamp() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.PLANNED);
        
        when(journeyRepository.findWithLegsById(any(UUID.class))).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);

        Journey result = service.startJourney(id);

        assertEquals(JourneyStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getActualDeparture());
    }

    @Test
    void startJourney_shouldThrowIfAlreadyStarted() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.IN_PROGRESS);
        
        when(journeyRepository.findWithLegsById(any(UUID.class))).thenReturn(Optional.of(journey));

        assertThrows(IllegalStateException.class, () -> service.startJourney(id));
    }

    @Test
    void completeJourney_shouldUpdateStatusAndTimestamp() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.IN_PROGRESS);
        
        when(journeyRepository.findWithLegsById(any(UUID.class))).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);

        Journey result = service.completeJourney(id);

        assertEquals(JourneyStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getActualArrival());
    }

    @Test
    void cancelJourney_shouldUpdateStatus() {
        UUID id = UUID.randomUUID();
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.PLANNED);
        
        when(journeyRepository.findWithLegsById(any(UUID.class))).thenReturn(Optional.of(journey));
        when(journeyRepository.save(any(Journey.class))).thenAnswer(i -> i.getArguments()[0]);

        Journey result = service.cancelJourney(id);

        assertEquals(JourneyStatus.CANCELLED, result.getStatus());
    }
}
