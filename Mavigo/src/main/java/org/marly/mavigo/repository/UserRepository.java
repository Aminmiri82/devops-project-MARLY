package org.marly.mavigo.repository;

import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByExternalId(String externalId);

    Optional<User> findByEmail(String email);
}

