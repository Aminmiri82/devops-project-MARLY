package org.marly.mavigo.controller.dto;

import org.marly.mavigo.models.user.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String externalId,
        @NotBlank
        @Email
        String email,
        @NotBlank String displayName,
        String homeAddress) {

    public User toUser() {
        User user = new User(externalId, email, displayName);
        user.setHomeAddress(homeAddress);
        return user;
    }
}
