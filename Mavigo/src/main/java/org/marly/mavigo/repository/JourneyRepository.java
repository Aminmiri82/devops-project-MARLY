package org.marly.mavigo.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    List<Journey> findByUserIdAndStatusIn(UUID userId, List<JourneyStatus> statuses);

    List<Journey> findByPlannedDepartureBetween(OffsetDateTime start, OffsetDateTime end);

    Optional<Journey> findByPrimItineraryId(String primItineraryId);

    /**
     * Fetches a journey with user and segments eagerly loaded.
     * Points must be initialized separately to avoid MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT j FROM Journey j " +
           "LEFT JOIN FETCH j.user " +
           "LEFT JOIN FETCH j.segments " +
           "WHERE j.id = :id")
    Optional<Journey> findWithSegmentsById(@Param("id") UUID id);

    /**
     * Finds journeys that pass through a specific stop area.
     */
    @Query("SELECT DISTINCT j FROM Journey j " +
           "JOIN j.segments s " +
           "JOIN s.points p " +
           "WHERE p.primStopAreaId = :stopAreaId " +
           "AND j.plannedDeparture >= :fromTime")
    List<Journey> findJourneysAffectedByStopArea(@Param("stopAreaId") String stopAreaId,
                                                  @Param("fromTime") OffsetDateTime fromTime);

    /**
     * Finds journeys that use a specific line.
     */
    @Query("SELECT DISTINCT j FROM Journey j " +
           "JOIN j.segments s " +
           "WHERE s.lineCode = :lineCode " +
           "AND j.plannedDeparture >= :fromTime")
    List<Journey> findJourneysUsingLine(@Param("lineCode") String lineCode,
                                        @Param("fromTime") OffsetDateTime fromTime);

    /**
     * Finds journeys with disrupted points.
     */
    @Query("SELECT DISTINCT j FROM Journey j " +
           "JOIN j.segments s " +
           "JOIN s.points p " +
           "WHERE p.status = 'DISRUPTED' " +
           "AND j.status IN :statuses")
    List<Journey> findJourneysWithDisruptedPoints(@Param("statuses") List<JourneyStatus> statuses);
}
