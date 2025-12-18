package org.marly.mavigo.controller.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanJourneyRequest(
        @NotNull UUID userId,
        @NotBlank String originQuery,
        @NotBlank String destinationQuery,
        @NotBlank String departureTime // ISO string, ex: 2025-12-14T18:00, 2025-12-14T18:00:00, ou 2025-12-14T18:00:00+01:00
) {}