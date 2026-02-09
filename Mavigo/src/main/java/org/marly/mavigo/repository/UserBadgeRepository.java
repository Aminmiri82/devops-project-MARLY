package org.marly.mavigo.repository;

import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.tracking.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    List<UserBadge> findAllByUserId(UUID userId);

    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);
}
