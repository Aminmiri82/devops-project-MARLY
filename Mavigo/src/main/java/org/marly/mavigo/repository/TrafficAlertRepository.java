package org.marly.mavigo.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.alert.TrafficAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrafficAlertRepository extends JpaRepository<TrafficAlert, UUID> {

    Optional<TrafficAlert> findBySourceAlertId(String sourceAlertId);

    List<TrafficAlert> findByValidUntilAfterOrValidUntilIsNull(OffsetDateTime instant);
}

