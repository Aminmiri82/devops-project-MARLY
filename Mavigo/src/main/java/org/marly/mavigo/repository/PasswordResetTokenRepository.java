package org.marly.mavigo.repository;

import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.user.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser_Id(UUID userId);
}
