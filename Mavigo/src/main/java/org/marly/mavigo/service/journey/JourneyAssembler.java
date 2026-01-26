package org.marly.mavigo.service.journey;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.springframework.stereotype.Component;

@Component
public class JourneyAssembler {

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
        journey.setTouristicModeEnabled(preferences != null && preferences.touristicModeEnabled());
        journey.setWheelchairAccessible(preferences != null && preferences.wheelchairAccessible());
        journey.setPrimItineraryId(plan.journeyId());
        journey.setStatus(JourneyStatus.PLANNED);
        journey.replaceLegs(mapLegs(plan.legs()));

        return journey;
    }

    private List<Leg> mapLegs(List<PrimJourneyPlanDto.LegDto> legDtos) {
        if (legDtos == null || legDtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<Leg> legs = new ArrayList<>(legDtos.size());
        for (PrimJourneyPlanDto.LegDto dto : legDtos) {
            legs.add(mapLeg(dto));
        }
        return legs;
    }

    private Leg mapLeg(PrimJourneyPlanDto.LegDto dto) {
        Leg leg = new Leg();
        leg.setSequenceOrder(dto.sequenceOrder());
        leg.setMode(resolveTransitMode(dto));
        leg.setLineCode(dto.lineCode());
        leg.setOriginLabel(defaultString(dto.originLabel(), "Unknown origin"));
        leg.setDestinationLabel(defaultString(dto.destinationLabel(), "Unknown destination"));
        leg.setOriginCoordinate(toGeoPoint(dto.originLatitude(), dto.originLongitude()));
        leg.setDestinationCoordinate(toGeoPoint(dto.destinationLatitude(), dto.destinationLongitude()));
        leg.setEstimatedDeparture(dto.departureDateTime());
        leg.setEstimatedArrival(dto.arrivalDateTime());
        leg.setDurationSeconds(dto.durationSeconds());
        leg.setNotes(dto.notes());
        return leg;
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
}

