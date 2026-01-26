package org.marly.mavigo.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NamedComfortSettingRequest(
        @NotBlank(message = "Name is required") String name,

        @NotNull(message = "Comfort profile is required") @Valid ComfortProfileRequest comfortProfile) {
}
