package org.marly.mavigo.controller.dto;

import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.service.disruption.dto.LineInfo;

public record LineInfoResponse(
        String lineCode,
        String lineName,
        String lineColor,
        TransitMode mode
) {
    public static LineInfoResponse from(LineInfo info) {
        return new LineInfoResponse(info.lineCode(), info.lineName(), info.lineColor(), info.mode());
    }
}
