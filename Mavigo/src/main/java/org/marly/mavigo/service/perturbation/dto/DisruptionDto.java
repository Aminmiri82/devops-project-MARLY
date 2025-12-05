package org.marly.mavigo.service.perturbation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DisruptionDto(
        String id,
        String lineId,
        String lineCode,
        String lineName,
        String status,
        String severity,
        String effect,
        Integer priority,
        String category,
        String cause,
        LocalDateTime updatedAt,
        List<String> tags,
        List<String> messages,
        List<DisruptionPeriodDto> applicationPeriods
) {

    public record DisruptionPeriodDto(LocalDateTime begin, LocalDateTime end) {
    }
}

