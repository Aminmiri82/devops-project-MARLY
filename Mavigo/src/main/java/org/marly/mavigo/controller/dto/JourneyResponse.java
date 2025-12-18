package org.marly.mavigo.controller.dto;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.UserTask;

public record JourneyResponse(
        UUID journeyId,
        UUID userId,
        String originLabel,
        String destinationLabel,
        OffsetDateTime plannedDeparture,
        OffsetDateTime plannedArrival,
        boolean comfortModeEnabled,
        boolean touristicModeEnabled,
        String primItineraryId,
        List<LegResponse> legs,
        List<TaskOnRouteResponse> tasksOnRoute
) {

    public static JourneyResponse from(Journey journey) {
        List<Leg> journeyLegs = journey.getLegs();
        List<LegResponse> legResponses = journeyLegs.isEmpty()
                ? Collections.emptyList()
                : journeyLegs.stream().map(JourneyResponse::fromLeg).toList();

        return new JourneyResponse(
                journey.getId(),
                journey.getUser() != null ? journey.getUser().getId() : null,
                journey.getOriginLabel(),
                journey.getDestinationLabel(),
                journey.getPlannedDeparture(),
                journey.getPlannedArrival(),
                journey.isComfortModeEnabled(),
                journey.isTouristicModeEnabled(),
                journey.getPrimItineraryId(),
                legResponses,
                Collections.emptyList()
        );
    }

    public static JourneyResponse from(Journey journey, List<TaskOnRouteResponse> tasksOnRoute) {
        JourneyResponse base = from(journey);
        return new JourneyResponse(
                base.journeyId(),
                base.userId(),
                base.originLabel(),
                base.destinationLabel(),
                base.plannedDeparture(),
                base.plannedArrival(),
                base.comfortModeEnabled(),
                base.touristicModeEnabled(),
                base.primItineraryId(),
                base.legs(),
                tasksOnRoute == null ? Collections.emptyList() : tasksOnRoute
        );
    }

    public static TaskOnRouteResponse fromTask(UserTask task, double distanceMeters) {
        GeoPoint p = task.getLocationHint();
        return new TaskOnRouteResponse(
                task.getId(),
                task.getTitle(),
                task.getNotes(),
                latitude(p),
                longitude(p),
                distanceMeters
        );
    }

    private static LegResponse fromLeg(Leg leg) {
        return new LegResponse(
                leg.getSequenceOrder(),
                leg.getMode(),
                leg.getLineCode(),
                leg.getOriginLabel(),
                leg.getDestinationLabel(),
                leg.getEstimatedDeparture(),
                leg.getEstimatedArrival(),
                leg.getDurationSeconds(),
                leg.getDistanceMeters(),
                leg.getNotes(),
                latitude(leg.getOriginCoordinate()),
                longitude(leg.getOriginCoordinate()),
                latitude(leg.getDestinationCoordinate()),
                longitude(leg.getDestinationCoordinate()));
    }

    private static Double latitude(GeoPoint geoPoint) {
        return geoPoint != null ? geoPoint.getLatitude() : null;
    }

    private static Double longitude(GeoPoint geoPoint) {
        return geoPoint != null ? geoPoint.getLongitude() : null;
    }

    public record LegResponse(
            int sequenceOrder,
            TransitMode mode,
            String lineCode,
            String originLabel,
            String destinationLabel,
            OffsetDateTime estimatedDeparture,
            OffsetDateTime estimatedArrival,
            Integer durationSeconds,
            Integer distanceMeters,
            String notes,
            Double originLat,
            Double originLng,
            Double destinationLat,
            Double destinationLng
    ) {}

    public record TaskOnRouteResponse(
            UUID taskId,
            String title,
            String notes,
            Double locationLat,
            Double locationLng,
            Double distanceMeters
    ) {}
}