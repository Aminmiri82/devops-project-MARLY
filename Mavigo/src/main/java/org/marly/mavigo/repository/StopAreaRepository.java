package org.marly.mavigo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.stoparea.StopArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopAreaRepository extends JpaRepository<StopArea, UUID> {

    Optional<StopArea> findByExternalId(String externalId);

    List<StopArea> findByNameIgnoreCase(String name);

    Optional<StopArea> findFirstByNameIgnoreCase(String name);
}

