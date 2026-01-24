package org.marly.mavigo.controller.dto;

import java.util.List;

import org.marly.mavigo.models.disruption.DisruptionType;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.service.disruption.dto.RerouteResult;

public record RerouteResponse(
        Long disruptionId,
        DisruptionType disruptionType,
        PointInfo disruptedPoint,
        PointInfo newOrigin,
        List<JourneyResponse> alternatives
) {
    public static RerouteResponse from(RerouteResult result) {
        PointInfo disruptedInfo = result.disruptedPoint() != null
                ? PointInfo.from(result.disruptedPoint())
                : null;

        PointInfo newOriginInfo = result.newOriginPoint() != null
                ? PointInfo.from(result.newOriginPoint())
                : null;

        List<JourneyResponse> alts = result.alternatives() != null
                ? result.alternatives().stream().map(JourneyResponse::from).toList()
                : List.of();

        return new RerouteResponse(
                result.disruption().getId(),
                result.disruption().getDisruptionType(),
                disruptedInfo,
                newOriginInfo,
                alts);
    }

    public record PointInfo(String name, String stopAreaId, String stopPointId) {
        static PointInfo from(JourneyPoint p) {
            return new PointInfo(p.getName(), p.getPrimStopAreaId(), p.getPrimStopPointId());
        }
    }
}
