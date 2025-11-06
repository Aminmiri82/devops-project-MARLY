package org.marly.mavigo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.marly.mavigo.models.poi.PointOfInterest;
import org.marly.mavigo.models.poi.PointOfInterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, UUID> {

    List<PointOfInterest> findTop10ByCategory(PointOfInterestCategory category);

    Optional<PointOfInterest> findByExternalId(String externalId);
}

