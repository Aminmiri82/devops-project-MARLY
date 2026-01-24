package org.marly.mavigo.service.disruption.dto;

import org.marly.mavigo.models.journey.TransitMode;

public record LineInfo(
        String lineCode,
        String lineName,
        String lineColor,
        TransitMode mode
) {}
