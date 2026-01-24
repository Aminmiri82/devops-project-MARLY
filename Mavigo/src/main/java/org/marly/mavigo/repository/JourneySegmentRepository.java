package org.marly.mavigo.repository;

import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JourneySegmentRepository extends JpaRepository<JourneySegment, UUID> {

    List<JourneySegment> findByJourneyIdOrderBySequenceOrder(UUID journeyId);

    List<JourneySegment> findByLineCode(String lineCode);

    List<JourneySegment> findBySegmentType(SegmentType segmentType);

    @Query("SELECT s FROM JourneySegment s LEFT JOIN FETCH s.points WHERE s.journey.id = :journeyId ORDER BY s.sequenceOrder")
    List<JourneySegment> findByJourneyIdWithPoints(@Param("journeyId") UUID journeyId);

    @Query("SELECT DISTINCT s.lineCode FROM JourneySegment s WHERE s.journey.id = :journeyId AND s.lineCode IS NOT NULL")
    List<String> findDistinctLineCodesByJourneyId(@Param("journeyId") UUID journeyId);
}
