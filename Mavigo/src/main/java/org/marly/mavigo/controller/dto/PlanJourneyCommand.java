package org.marly.mavigo.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PlanJourneyCommand(
        @NotNull
        @Valid
        PlanJourneyRequest journey,
        JourneyPreferencesRequest preferences) {
}

