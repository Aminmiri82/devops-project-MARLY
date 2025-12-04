package org.marly.mavigo.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.marly.mavigo.models.user.User;

public record UserResponse(
        UUID userId,
        String externalId,
        String email,
        String displayName,
        String homeStationId,
        String workStationId,
        OffsetDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getExternalId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getHomeStationId(),
                user.getWorkStationId(),
                user.getCreatedAt());
    }
}

