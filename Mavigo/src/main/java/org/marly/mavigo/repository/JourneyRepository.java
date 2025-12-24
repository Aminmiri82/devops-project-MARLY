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

    @org.springframework.data.jpa.repository.Query("SELECT j FROM Journey j LEFT JOIN FETCH j.legs LEFT JOIN FETCH j.disruptions WHERE j.id = :id")
    Optional<Journey> findWithLegsById(@org.springframework.data.repository.query.Param("id") UUID id);
}

