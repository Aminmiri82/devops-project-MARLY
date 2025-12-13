package org.marly.mavigo.repository;

import org.marly.mavigo.models.disruption.Disruption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
public interface DisruptionRepository extends JpaRepository<Disruption, Long> {
    List<Disruption> findByValidUntilAfter(LocalDateTime now);
}
