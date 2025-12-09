package org.marly.mavigo.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanJourneyRequest(
        @NotNull UUID userId,
        @NotBlank String originQuery,
        @NotBlank String destinationQuery,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime departureTime) {
}

