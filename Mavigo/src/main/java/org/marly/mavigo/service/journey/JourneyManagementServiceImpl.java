package org.marly.mavigo.service.journey;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.models.tracking.Badge;
import org.marly.mavigo.service.tracking.GamificationService;
import org.springframework.stereotype.Service;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class JourneyManagementServiceImpl implements JourneyManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JourneyManagementServiceImpl.class);

    private final JourneyRepository journeyRepository;
    private final GamificationService gamificationService;

    public JourneyManagementServiceImpl(JourneyRepository journeyRepository, GamificationService gamificationService) {
        this.journeyRepository = journeyRepository;
        this.gamificationService = gamificationService;
    }

    @Override
    public JourneyActionResult startJourney(UUID journeyId) {
        Journey journey = fetchJourneyWithFullGraph(journeyId);

        if (journey.getStatus() != JourneyStatus.PLANNED && journey.getStatus() != JourneyStatus.REROUTED) {
            throw new IllegalStateException("Journey cannot be started. Current status: " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.IN_PROGRESS);
        journey.setActualDeparture(OffsetDateTime.now());

        journeyRepository.save(journey);

        // Re-fetch with full graph
        Journey updatedJourney = fetchJourneyWithFullGraph(journeyId);
        return new JourneyActionResult(updatedJourney, List.of());
    }

    @Override
    public JourneyActionResult completeJourney(UUID journeyId) {
        Journey journey = fetchJourneyWithFullGraph(journeyId);

        if (journey.getStatus() != JourneyStatus.IN_PROGRESS && journey.getStatus() != JourneyStatus.REROUTED) {
            throw new IllegalStateException(
                    "Journey cannot be completed. The journey must be IN_PROGRESS or REROUTED. Current status: "
                            + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.COMPLETED);
        journey.setActualArrival(OffsetDateTime.now());

        journeyRepository.save(journey);

        // Track activity and check for badges
        List<Badge> newBadges = gamificationService.trackActivityAndCheckBadges(journey);

        Journey updatedJourney = fetchJourneyWithFullGraph(journeyId);
        return new JourneyActionResult(updatedJourney, newBadges);
    }

    @Override
    public Journey cancelJourney(UUID journeyId) {
        Journey journey = fetchJourneyWithFullGraph(journeyId);

        if (journey.getStatus() == JourneyStatus.COMPLETED || journey.getStatus() == JourneyStatus.CANCELLED) {
            throw new IllegalStateException("Journey cannot be cancelled. It is already " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.CANCELLED);

        journeyRepository.save(journey);
        return fetchJourneyWithFullGraph(journeyId);
    }

    @Override
    public Journey getJourney(UUID journeyId) {
        return fetchJourneyWithFullGraph(journeyId);
    }

    /**
     * Fetches a journey with full graph using JOIN FETCH query.
     * Points and disruptions are initialized separately to avoid
     * MultipleBagFetchException.
     */
    private Journey fetchJourneyWithFullGraph(UUID journeyId) {
        Journey journey = journeyRepository.findWithSegmentsById(journeyId)
                .orElseThrow(() -> new EntityNotFoundException("Journey not found with id: " + journeyId));
        // Force initialization by accessing the collections.
        // Note: Hibernate.initialize() doesn't work here because getPoints() returns
        // an UnmodifiableList wrapper, which Hibernate doesn't recognize as a lazy
        // proxy.
        for (JourneySegment segment : journey.getSegments()) {
            segment.getPoints().size();
        }
        journey.getDisruptions().size();
        return journey;
    }

    @Override
    public void clearAllData() {
        LOGGER.info("Clearing all journey data");
        journeyRepository.deleteAll();
        gamificationService.clearAllActivity();
    }
}
