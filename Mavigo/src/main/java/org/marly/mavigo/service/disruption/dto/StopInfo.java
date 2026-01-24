package org.marly.mavigo.service.disruption.dto;

public record StopInfo(
        String primStopAreaId,
        String primStopPointId,
        String name,
        int sequenceInJourney,
        String onLineCode
) {}
