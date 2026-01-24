package org.marly.mavigo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JourneyPointRepository extends JpaRepository<JourneyPoint, UUID> {

    List<JourneyPoint> findByPrimStopAreaId(String stopAreaId);

    List<JourneyPoint> findByPrimStopPointId(String stopPointId);

    List<JourneyPoint> findByStatus(JourneyPointStatus status);

    @Query("SELECT p FROM JourneyPoint p WHERE p.segment.journey.id = :journeyId ORDER BY p.segment.sequenceOrder, p.sequenceInSegment")
    List<JourneyPoint> findAllByJourneyIdOrdered(@Param("journeyId") UUID journeyId);

    @Query("SELECT p FROM JourneyPoint p WHERE p.segment.journey.id = :journeyId AND p.status = :status")
    List<JourneyPoint> findByJourneyIdAndStatus(@Param("journeyId") UUID journeyId, @Param("status") JourneyPointStatus status);

    @Query("SELECT p FROM JourneyPoint p WHERE p.primStopAreaId = :stopAreaId AND p.segment.journey.id = :journeyId")
    Optional<JourneyPoint> findByStopAreaIdAndJourneyId(@Param("stopAreaId") String stopAreaId, @Param("journeyId") UUID journeyId);
}
