package org.marly.mavigo.client.prim.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PrimJourneyPlanDto(
        String journeyId,
        OffsetDateTime departureDateTime,
        OffsetDateTime arrivalDateTime,
        Integer durationSeconds,
        Integer transfers,
        List<LegDto> legs) {

    public record LegDto(
            int sequenceOrder,
            String sectionId,
            String sectionType,
            String commercialMode,
            String lineCode,
            String lineName,
            String lineColor,
            String networkName,
            OffsetDateTime departureDateTime,
            OffsetDateTime arrivalDateTime,
            Integer durationSeconds,
            String originStopId,
            String originLabel,
            Double originLatitude,
            Double originLongitude,
            String destinationStopId,
            String destinationLabel,
            Double destinationLatitude,
            Double destinationLongitude,
            String notes,
            Boolean hasAirConditioning,
            List<StopDateTimeDto> stopDateTimes) {
    }

    public record StopDateTimeDto(
            String stopPointId,
            String stopPointName,
            String stopAreaId,
            Double latitude,
            Double longitude,
            OffsetDateTime arrivalDateTime,
            OffsetDateTime departureDateTime) {
    }

    public boolean hasAirConditioningOnAllTransitLegs() {
        if (legs == null || legs.isEmpty()) return false;
        return legs.stream()
                .filter(this::isTransitLeg)
                .allMatch(leg -> Boolean.TRUE.equals(leg.hasAirConditioning()));
    }

    private boolean isTransitLeg(LegDto leg) {
        String type = leg.sectionType();
        return type != null
                && !type.equals("street_network")
                && !type.equals("transfer")
                && !type.equals("waiting")
                && !type.equals("crow_fly");
    }
}
