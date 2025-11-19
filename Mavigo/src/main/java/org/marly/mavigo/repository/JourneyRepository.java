package org.marly.mavigo.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    List<Journey> findByUserIdAndStatusIn(UUID userId, List<JourneyStatus> statuses);

    List<Journey> findByPlannedDepartureBetween(OffsetDateTime start, OffsetDateTime end);

    Optional<Journey> findByPrimItineraryId(String primItineraryId);
}

