package org.marly.mavigo.repository;

import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.tracking.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    Optional<Badge> findByName(String name);

    boolean existsByName(String name);
}
