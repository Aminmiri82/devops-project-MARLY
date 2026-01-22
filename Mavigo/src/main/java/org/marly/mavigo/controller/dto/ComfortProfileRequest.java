package org.marly.mavigo.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record ComfortProfileRequest(
        @Pattern(regexp = "indifferent|none|only|only_with_alternatives", message = "Invalid direct path value")
        String directPath,

        Boolean requireAirConditioning,

        @Min(0)
        @Max(10)
        Integer maxNbTransfers,

        @Min(0)
        @Max(7200)
        Integer maxWaitingDuration,

        @Min(0)
        @Max(7200)
        Integer maxWalkingDuration) {
}
