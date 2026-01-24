package org.marly.mavigo.controller.dto;

import org.marly.mavigo.service.disruption.dto.StopInfo;

public record StopInfoResponse(
        String stopAreaId,
        String stopPointId,
        String name,
        int sequenceInJourney,
        String onLineCode
) {
    public static StopInfoResponse from(StopInfo info) {
        return new StopInfoResponse(
                info.primStopAreaId(),
                info.primStopPointId(),
                info.name(),
                info.sequenceInJourney(),
                info.onLineCode());
    }
}
