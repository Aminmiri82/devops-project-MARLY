package org.marly.mavigo.service.journey;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointStatus;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.springframework.stereotype.Component;

/**
 * Assembles Journey entities from PRIM API responses.
 * Creates JourneySegment and JourneyPoint entities for the new architecture.
 */
@Component
public class JourneyAssembler {

    /**
     * Assembles a Journey entity from PRIM response data.
     */
    public Journey assemble(User user,
            StopArea origin,
            StopArea destination,
            PrimJourneyPlanDto plan,
            JourneyPreferences preferences) {

        Objects.requireNonNull(user, "User is required when creating a journey");
        Objects.requireNonNull(origin, "Origin stop area is required");
        Objects.requireNonNull(destination, "Destination stop area is required");
        Objects.requireNonNull(plan, "Prim journey plan is required");

        OffsetDateTime departure = resolveDeparture(plan);
        OffsetDateTime arrival = resolveArrival(plan);

        Journey journey = new Journey(
                user,
                origin.getName(),
                destination.getName(),
                departure,
                arrival);

        journey.setOriginCoordinate(resolveCoordinate(origin.getCoordinates(), plan, true));
        journey.setDestinationCoordinate(resolveCoordinate(destination.getCoordinates(), plan, false));
        journey.setComfortModeEnabled(preferences != null && preferences.comfortModeEnabled());
        journey.setEcoModeEnabled(preferences != null && preferences.ecoModeEnabled());
        journey.setPrimItineraryId(plan.journeyId());
        journey.setStatus(JourneyStatus.PLANNED);

        // Create segments and points
        List<JourneySegment> segments = mapSegments(journey, plan.legs());
        for (JourneySegment segment : segments) {
            journey.addSegment(segment);
        }

        // Mark transfer points
        markTransferPoints(journey);

        return journey;
    }

    /**
     * Creates JourneySegment entities from leg DTOs.
     * Filters out WAITING segments (they don't provide navigation value).
     */
    private List<JourneySegment> mapSegments(Journey journey, List<PrimJourneyPlanDto.LegDto> legDtos) {
        if (legDtos == null || legDtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<JourneySegment> segments = new ArrayList<>(legDtos.size());
        int segmentOrder = 0;

        for (PrimJourneyPlanDto.LegDto dto : legDtos) {
            // Skip WAITING segments - they don't provide useful navigation info
            if ("waiting".equalsIgnoreCase(dto.sectionType())) {
                continue;
            }

            JourneySegment segment = mapSegment(journey, dto, segmentOrder++);

            // Skip segments with no points (malformed data)
            if (segment.getPoints().isEmpty()) {
                continue;
            }

            segments.add(segment);
        }

        return segments;
    }

    /**
     * Creates a single JourneySegment from a leg DTO.
     */
    private JourneySegment mapSegment(Journey journey, PrimJourneyPlanDto.LegDto dto, int sequenceOrder) {
        SegmentType segmentType = resolveSegmentType(dto.sectionType());
        TransitMode transitMode = resolveTransitMode(dto);

        JourneySegment segment = new JourneySegment(journey, sequenceOrder, segmentType);
        segment.setTransitMode(transitMode);
        segment.setPrimSectionId(dto.sectionId());
        segment.setLineCode(dto.lineCode());
        segment.setLineName(dto.lineName());
        segment.setLineColor(dto.lineColor());
        segment.setNetworkName(dto.networkName());
        segment.setScheduledDeparture(dto.departureDateTime());
        segment.setScheduledArrival(dto.arrivalDateTime());
        segment.setDurationSeconds(dto.durationSeconds());
        segment.setHasAirConditioning(dto.hasAirConditioning());

        // Calculate distance if coordinates are available
        if (dto.originLatitude() != null && dto.originLongitude() != null &&
                dto.destinationLatitude() != null && dto.destinationLongitude() != null) {
            double distance = calculateDistance(
                    dto.originLatitude(), dto.originLongitude(),
                    dto.destinationLatitude(), dto.destinationLongitude());
            segment.setDistanceMeters((int) Math.round(distance));
        } else {
            segment.setDistanceMeters(0);
        }

        // Create points for the segment
        List<JourneyPoint> points = createPointsForSegment(segment, dto);
        for (JourneyPoint point : points) {
            segment.addPoint(point);
        }

        return segment;
    }

    /**
     * Creates JourneyPoint entities for a segment.
     * Uses stop_date_times if available, otherwise falls back to origin/destination
     * only.
     */
    private List<JourneyPoint> createPointsForSegment(JourneySegment segment, PrimJourneyPlanDto.LegDto dto) {
        List<JourneyPoint> points = new ArrayList<>();

        if (dto.stopDateTimes() != null && !dto.stopDateTimes().isEmpty()) {
            // Use all intermediate stops from stop_date_times
            int pointSequence = 0;
            int totalStops = dto.stopDateTimes().size();

            for (int i = 0; i < totalStops; i++) {
                PrimJourneyPlanDto.StopDateTimeDto sdt = dto.stopDateTimes().get(i);
                JourneyPointType pointType = determinePointType(i, totalStops, segment);

                JourneyPoint point = new JourneyPoint(segment, pointSequence++, pointType,
                        defaultString(sdt.stopPointName(), "Unknown stop"));

                point.setPrimStopPointId(sdt.stopPointId());
                point.setPrimStopAreaId(sdt.stopAreaId());
                point.setCoordinates(toGeoPoint(sdt.latitude(), sdt.longitude()));
                point.setScheduledArrival(sdt.arrivalDateTime());
                point.setScheduledDeparture(sdt.departureDateTime());
                point.setStatus(JourneyPointStatus.NORMAL);

                points.add(point);
            }
        } else {
            // Fallback: create origin and destination points from leg data
            String originName = resolvePointName(dto.originLabel(), dto.lineName(), "Origin");
            String destName = resolvePointName(dto.destinationLabel(), dto.lineName(), "Destination");

            // Always create origin point if we have any data
            JourneyPoint origin = new JourneyPoint(segment, 0, JourneyPointType.ORIGIN, originName);
            origin.setPrimStopPointId(dto.originStopId());
            origin.setCoordinates(toGeoPoint(dto.originLatitude(), dto.originLongitude()));
            origin.setScheduledDeparture(dto.departureDateTime());
            origin.setStatus(JourneyPointStatus.NORMAL);
            points.add(origin);

            // Only add destination if it's different from origin (avoid same-place
            // duplicates)
            boolean samePlace = originName.equals(destName)
                    && (dto.durationSeconds() == null || dto.durationSeconds() < 60);

            if (!samePlace) {
                JourneyPoint destination = new JourneyPoint(segment, 1, JourneyPointType.DESTINATION, destName);
                destination.setPrimStopPointId(dto.destinationStopId());
                destination.setCoordinates(toGeoPoint(dto.destinationLatitude(), dto.destinationLongitude()));
                destination.setScheduledArrival(dto.arrivalDateTime());
                destination.setStatus(JourneyPointStatus.NORMAL);
                points.add(destination);
            }
        }

        return points;
    }

    /**
     * Resolves a point name from available data.
     */
    private String resolvePointName(String label, String lineName, String fallback) {
        if (label != null && !label.isBlank()) {
            return label;
        }
        if (lineName != null && !lineName.isBlank()) {
            return lineName;
        }
        return fallback;
    }

    /**
     * Determines the point type based on position within the segment.
     */
    private JourneyPointType determinePointType(int index, int totalStops, JourneySegment segment) {
        if (index == 0) {
            return JourneyPointType.ORIGIN;
        } else if (index == totalStops - 1) {
            return JourneyPointType.DESTINATION;
        } else if (segment.getSegmentType() == SegmentType.WALKING ||
                segment.getSegmentType() == SegmentType.TRANSFER) {
            return JourneyPointType.WALKING_WAYPOINT;
        } else {
            return JourneyPointType.INTERMEDIATE_STOP;
        }
    }

    /**
     * Marks transfer points where the journey changes from one segment to another.
     * The last point of segment N and first point of segment N+1 at a transfer
     * are marked as TRANSFER_ARRIVAL and TRANSFER_DEPARTURE respectively.
     */
    private void markTransferPoints(Journey journey) {
        List<JourneySegment> segments = journey.getSegments();
        if (segments.size() < 2) {
            return;
        }

        for (int i = 0; i < segments.size() - 1; i++) {
            JourneySegment currentSegment = segments.get(i);
            JourneySegment nextSegment = segments.get(i + 1);

            // Check if this is a transfer (walking/transfer segment followed by another)
            // or a change between public transport lines
            boolean isTransfer = currentSegment.getSegmentType() == SegmentType.TRANSFER
                    || currentSegment.getSegmentType() == SegmentType.WALKING
                    || nextSegment.getSegmentType() == SegmentType.TRANSFER
                    || nextSegment.getSegmentType() == SegmentType.WALKING
                    || (currentSegment.getSegmentType() == SegmentType.PUBLIC_TRANSPORT
                            && nextSegment.getSegmentType() == SegmentType.PUBLIC_TRANSPORT);

            if (isTransfer) {
                JourneyPoint arrivalPoint = currentSegment.getDestinationPoint();
                JourneyPoint departurePoint = nextSegment.getOriginPoint();

                if (arrivalPoint != null) {
                    arrivalPoint.setPointType(JourneyPointType.TRANSFER_ARRIVAL);
                }
                if (departurePoint != null) {
                    departurePoint.setPointType(JourneyPointType.TRANSFER_DEPARTURE);
                }
            }
        }
    }

    private SegmentType resolveSegmentType(String sectionType) {
        if (sectionType == null) {
            return SegmentType.PUBLIC_TRANSPORT;
        }

        return switch (sectionType.toLowerCase(Locale.ROOT)) {
            case "street_network", "walking" -> SegmentType.WALKING;
            case "transfer" -> SegmentType.TRANSFER;
            case "waiting" -> SegmentType.WAITING;
            case "crow_fly" -> SegmentType.CROW_FLY;
            default -> SegmentType.PUBLIC_TRANSPORT;
        };
    }

    private TransitMode resolveTransitMode(PrimJourneyPlanDto.LegDto dto) {
        String candidate = firstNonBlank(dto.commercialMode(), dto.sectionType());
        if (candidate == null) {
            return TransitMode.OTHER;
        }

        String normalized = Normalizer.normalize(candidate, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "metro" -> TransitMode.METRO;
            case "rer" -> TransitMode.RER;
            case "train", "transilien" -> TransitMode.TRANSILIEN;
            case "tramway", "tram" -> TransitMode.TRAM;
            case "bus", "noctilien", "coach" -> TransitMode.BUS;
            case "walk", "walking", "street_network", "transfer" -> TransitMode.WALK;
            case "bike", "cycling" -> TransitMode.BIKE;
            case "taxi" -> TransitMode.TAXI;
            default -> TransitMode.OTHER;
        };
    }

    private GeoPoint resolveCoordinate(GeoPoint stopAreaCoordinate,
            PrimJourneyPlanDto plan,
            boolean origin) {
        if (stopAreaCoordinate != null) {
            return clone(stopAreaCoordinate);
        }

        PrimJourneyPlanDto.LegDto referenceLeg = origin
                ? firstLeg(plan.legs())
                : lastLeg(plan.legs());

        if (referenceLeg == null) {
            return null;
        }

        Double lat = origin ? referenceLeg.originLatitude() : referenceLeg.destinationLatitude();
        Double lon = origin ? referenceLeg.originLongitude() : referenceLeg.destinationLongitude();
        return toGeoPoint(lat, lon);
    }

    private PrimJourneyPlanDto.LegDto firstLeg(List<PrimJourneyPlanDto.LegDto> legs) {
        if (legs == null || legs.isEmpty()) {
            return null;
        }
        return legs.get(0);
    }

    private PrimJourneyPlanDto.LegDto lastLeg(List<PrimJourneyPlanDto.LegDto> legs) {
        if (legs == null || legs.isEmpty()) {
            return null;
        }
        return legs.get(legs.size() - 1);
    }

    private GeoPoint toGeoPoint(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return null;
        }
        return new GeoPoint(lat, lon);
    }

    private GeoPoint clone(GeoPoint geoPoint) {
        if (geoPoint == null) {
            return null;
        }
        return new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    private OffsetDateTime resolveDeparture(PrimJourneyPlanDto plan) {
        if (plan.departureDateTime() != null) {
            return plan.departureDateTime();
        }
        PrimJourneyPlanDto.LegDto firstLeg = firstLeg(plan.legs());
        if (firstLeg != null && firstLeg.departureDateTime() != null) {
            return firstLeg.departureDateTime();
        }
        return OffsetDateTime.now();
    }

    private OffsetDateTime resolveArrival(PrimJourneyPlanDto plan) {
        if (plan.arrivalDateTime() != null) {
            return plan.arrivalDateTime();
        }
        PrimJourneyPlanDto.LegDto lastLeg = lastLeg(plan.legs());
        if (lastLeg != null && lastLeg.arrivalDateTime() != null) {
            return lastLeg.arrivalDateTime();
        }
        return OffsetDateTime.now();
    }

    private String defaultString(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
