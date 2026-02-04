package org.marly.mavigo.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.marly.mavigo.models.user.User;

public record UpdateUserRequest(
        @NotBlank String externalId,
        @NotBlank
        @Email
        String email,
        @NotBlank String displayName,
        String homeAddress) {

    public void apply(User user) {
        user.setExternalId(externalId);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setHomeAddress(homeAddress);
    }
}
