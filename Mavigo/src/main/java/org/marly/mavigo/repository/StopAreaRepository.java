package org.marly.mavigo.repository;

import java.util.Optional;
import java.util.UUID;
import org.marly.mavigo.models.stoparea.StopArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopAreaRepository extends JpaRepository<StopArea, UUID> {

    Optional<StopArea> findByExternalId(String externalId);
    
    Optional<StopArea> findByNameIgnoreCase(String name);
}

