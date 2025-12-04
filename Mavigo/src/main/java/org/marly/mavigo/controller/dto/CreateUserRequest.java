package org.marly.mavigo.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.marly.mavigo.models.user.User;

public record CreateUserRequest(
        @NotBlank String externalId,
        @NotBlank
        @Email
        String email,
        @NotBlank String displayName,
        String homeStationId,
        String workStationId) {

    public User toUser() {
        User user = new User(externalId, email, displayName);
        user.setHomeStationId(homeStationId);
        user.setWorkStationId(workStationId);
        return user;
    }
}

