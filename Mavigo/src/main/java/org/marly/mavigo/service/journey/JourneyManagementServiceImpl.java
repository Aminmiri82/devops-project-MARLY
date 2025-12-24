package org.marly.mavigo.service.journey;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.repository.JourneyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class JourneyManagementServiceImpl implements JourneyManagementService {

    private final JourneyRepository journeyRepository;

    public JourneyManagementServiceImpl(JourneyRepository journeyRepository) {
        this.journeyRepository = journeyRepository;
    }

    @Override
    public Journey startJourney(UUID journeyId) {
        Journey journey = getJourney(journeyId);
        
        if (journey.getStatus() != JourneyStatus.PLANNED && journey.getStatus() != JourneyStatus.REROUTED) {
             throw new IllegalStateException("Journey cannot be started. Current status: " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.IN_PROGRESS);
        journey.setActualDeparture(OffsetDateTime.now());
        
        Journey savedJourney = journeyRepository.save(journey);
        org.hibernate.Hibernate.initialize(savedJourney.getLegs());
        org.hibernate.Hibernate.initialize(savedJourney.getUser());
        org.hibernate.Hibernate.initialize(savedJourney.getDisruptions());
        return savedJourney;
    }

    @Override
    public Journey completeJourney(UUID journeyId) {
        Journey journey = getJourney(journeyId);
        
        if (journey.getStatus() != JourneyStatus.IN_PROGRESS && journey.getStatus() != JourneyStatus.REROUTED) {
            throw new IllegalStateException("Journey cannot be completed. The journey must be IN_PROGRESS or REROUTED. Current status: " + journey.getStatus());
       }

        journey.setStatus(JourneyStatus.COMPLETED);
        journey.setActualArrival(OffsetDateTime.now());
        
        Journey savedJourney = journeyRepository.save(journey);
        org.hibernate.Hibernate.initialize(savedJourney.getLegs());
        org.hibernate.Hibernate.initialize(savedJourney.getUser());
        org.hibernate.Hibernate.initialize(savedJourney.getDisruptions());
        return savedJourney;
    }

    @Override
    public Journey cancelJourney(UUID journeyId) {
        Journey journey = getJourney(journeyId);
        
        if (journey.getStatus() == JourneyStatus.COMPLETED || journey.getStatus() == JourneyStatus.CANCELLED) {
             throw new IllegalStateException("Journey cannot be cancelled. It is already " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.CANCELLED);
        
        Journey savedJourney = journeyRepository.save(journey);
        org.hibernate.Hibernate.initialize(savedJourney.getLegs());
        org.hibernate.Hibernate.initialize(savedJourney.getUser());
        org.hibernate.Hibernate.initialize(savedJourney.getDisruptions());
        return savedJourney;
    }

    @Override
    public Journey getJourney(UUID journeyId) {
        return journeyRepository.findWithLegsById(journeyId)
                .orElseThrow(() -> new EntityNotFoundException("Journey not found with id: " + journeyId));
    }
}
