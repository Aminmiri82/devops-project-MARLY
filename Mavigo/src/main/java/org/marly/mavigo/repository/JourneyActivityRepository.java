package org.marly.mavigo.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.tracking.JourneyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JourneyActivityRepository extends JpaRepository<JourneyActivity, UUID> {

    List<JourneyActivity> findAllByUserIdOrderByRecordedAtDesc(UUID userId);

    @Query("SELECT SUM(ja.co2SavedKg) FROM JourneyActivity ja WHERE ja.userId = :userId")
    Double getTotalCo2SavedByUserId(@Param("userId") UUID userId);

    @Query("SELECT SUM(ja.distanceMeters) FROM JourneyActivity ja WHERE ja.userId = :userId")
    Integer getTotalDistanceByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ja) FROM JourneyActivity ja WHERE ja.userId = :userId")
    Long countByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM JourneyActivity ja WHERE ja.recordedAt < :cutoff")
    void deleteByRecordedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
