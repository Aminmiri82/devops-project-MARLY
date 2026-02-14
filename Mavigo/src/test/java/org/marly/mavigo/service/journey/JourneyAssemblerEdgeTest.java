package org.marly.mavigo.service.journey;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

class JourneyAssemblerEdgeTest {

    private JourneyAssembler assembler;
    private User user;

    @BeforeEach
    void setUp() {
        assembler = new JourneyAssembler();
        user = new User("ext-user", "user@example.com", "User");
    }

    @Test
    void assemble_enablesEcoModeAndResolvesCoordinatesFromLegs() {
        StopArea origin = new StopArea("origin", "Origin", null);
        StopArea destination = new StopArea("destination", "Destination", null);
        PrimJourneyPlanDto plan = plan(
                "plan-1",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(20),
                List.of(leg("section-1", "public_transport", "Metro", "M1",
                        null, null, 48.8001, 2.3001, 48.9002, 2.4002, 1200, null)));

        Journey journey = assembler.assemble(user, origin, destination, plan, new JourneyPreferences(false, true, null));

        assertThat(journey.isEcoModeEnabled()).isTrue();
        assertThat(journey.getOriginCoordinate().getLatitude()).isEqualTo(48.8001);
        assertThat(journey.getDestinationCoordinate().getLongitude()).isEqualTo(2.4002);
    }

    @Test
    void assemble_setsDistanceToZeroWhenCoordinatesAreMissing() {
        StopArea origin = new StopArea("origin", "Origin", null);
        StopArea destination = new StopArea("destination", "Destination", null);
        PrimJourneyPlanDto plan = plan(
                "plan-2",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(10),
                List.of(leg("section-1", "public_transport", "Metro", "M1",
                        "A", "B", null, null, null, null, 600, null)));

        Journey journey = assembler.assemble(user, origin, destination, plan, null);

        assertThat(journey.getSegments()).hasSize(1);
        assertThat(journey.getSegments().get(0).getDistanceMeters()).isZero();
    }

    @Test
    void assemble_marksIntermediateWalkingStopAsWalkingWaypoint() {
        StopArea origin = new StopArea("origin", "Origin", new GeoPoint(48.80, 2.30));
        StopArea destination = new StopArea("destination", "Destination", new GeoPoint(48.90, 2.40));
        OffsetDateTime now = OffsetDateTime.now();
        List<PrimJourneyPlanDto.StopDateTimeDto> stops = List.of(
                stop("sp1", "A", "sa1", 48.80, 2.30, now, now.plusMinutes(1)),
                stop("sp2", "B", "sa2", 48.81, 2.31, now.plusMinutes(2), now.plusMinutes(3)),
                stop("sp3", "C", "sa3", 48.82, 2.32, now.plusMinutes(4), now.plusMinutes(5)));
        PrimJourneyPlanDto plan = plan(
                "plan-3",
                now,
                now.plusMinutes(5),
                List.of(leg("section-1", "walking", "walking", null,
                        "A", "C", 48.80, 2.30, 48.82, 2.32, 300, stops)));

        Journey journey = assembler.assemble(user, origin, destination, plan, null);

        JourneySegment segment = journey.getSegments().get(0);
        assertThat(segment.getSegmentType()).isEqualTo(SegmentType.WALKING);
        assertThat(segment.getPoints().get(1).getPointType()).isEqualTo(JourneyPointType.WALKING_WAYPOINT);
    }

    @Test
    void assemble_resolvesPointNamesFromLineNameAndDefaults() {
        StopArea origin = new StopArea("origin", "Origin", new GeoPoint(48.80, 2.30));
        StopArea destination = new StopArea("destination", "Destination", new GeoPoint(48.90, 2.40));
        OffsetDateTime now = OffsetDateTime.now();

        PrimJourneyPlanDto.LegDto lineNameLeg = leg("section-1", "public_transport", "Metro", "M1",
                null, null, 48.80, 2.30, 48.81, 2.31, 120, null);
        PrimJourneyPlanDto.LegDto fallbackLeg = leg("section-2", "public_transport", "", "M2",
                null, null, 48.81, 2.31, 48.82, 2.32, 120, null);
        PrimJourneyPlanDto plan = plan("plan-4", now, now.plusMinutes(10), List.of(lineNameLeg, fallbackLeg));

        Journey journey = assembler.assemble(user, origin, destination, plan, null);

        assertThat(journey.getSegments().get(0).getPoints().get(0).getName()).isEqualTo("Metro");
        assertThat(journey.getSegments().get(1).getPoints().get(1).getName()).isEqualTo("Destination");
    }

    @Test
    void assemble_handlesSectionTypeAndTransitModeEdges() {
        StopArea origin = new StopArea("origin", "Origin", new GeoPoint(48.80, 2.30));
        StopArea destination = new StopArea("destination", "Destination", new GeoPoint(48.90, 2.40));
        OffsetDateTime now = OffsetDateTime.now();

        PrimJourneyPlanDto.LegDto waiting = leg("w", "waiting", null, null,
                "Wait", "Wait", 48.80, 2.30, 48.80, 2.30, 120, null);
        PrimJourneyPlanDto.LegDto nullType = leg("n", null, null, "L1",
                "A", "B", 48.80, 2.30, 48.81, 2.31, 120, null);
        PrimJourneyPlanDto.LegDto crowFly = leg("c", "crow_fly", null, "L2",
                "B", "C", 48.81, 2.31, 48.82, 2.32, 120, null);
        PrimJourneyPlanDto.LegDto bike = leg("b", "public_transport", "bike", "L3",
                "C", "D", 48.82, 2.32, 48.83, 2.33, 120, null);
        PrimJourneyPlanDto.LegDto taxi = leg("t", "public_transport", "taxi", "L4",
                "D", "E", 48.83, 2.33, 48.84, 2.34, 120, null);

        Journey journey = assembler.assemble(user, origin, destination, plan("plan-5", now, now.plusMinutes(15),
                List.of(waiting, nullType, crowFly, bike, taxi)), null);

        assertThat(journey.getSegments()).hasSize(4);
        assertThat(journey.getSegments()).extracting(JourneySegment::getSegmentType)
                .contains(SegmentType.PUBLIC_TRANSPORT, SegmentType.CROW_FLY);
        assertThat(journey.getSegments()).extracting(JourneySegment::getTransitMode)
                .contains(TransitMode.OTHER, TransitMode.BIKE, TransitMode.TAXI);
    }

    @Test
    void assemble_fallsBackToNowWhenPlanAndLegTimesMissing() {
        StopArea origin = new StopArea("origin", "Origin", null);
        StopArea destination = new StopArea("destination", "Destination", null);
        PrimJourneyPlanDto plan = plan("plan-6", null, null, List.of());
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(2);

        Journey journey = assembler.assemble(user, origin, destination, plan, null);
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(2);

        assertThat(journey.getPlannedDeparture()).isBetween(before, after);
        assertThat(journey.getPlannedArrival()).isBetween(before, after);
        assertThat(Duration.between(journey.getPlannedDeparture(), journey.getPlannedArrival()).abs().toSeconds())
                .isLessThanOrEqualTo(5);
    }

    @Test
    void privateHelpers_handleNullInputs() throws Exception {
        Method clone = JourneyAssembler.class.getDeclaredMethod("clone", GeoPoint.class);
        clone.setAccessible(true);
        Object cloned = clone.invoke(assembler, new Object[] { null });
        assertThat(cloned).isNull();

        Method firstNonBlank = JourneyAssembler.class.getDeclaredMethod("firstNonBlank", String.class, String.class);
        firstNonBlank.setAccessible(true);
        Object value = firstNonBlank.invoke(assembler, " ", null);
        assertThat(value).isNull();
    }

    private PrimJourneyPlanDto plan(String id, OffsetDateTime departure, OffsetDateTime arrival,
            List<PrimJourneyPlanDto.LegDto> legs) {
        return new PrimJourneyPlanDto(id, departure, arrival, 600, 0, legs);
    }

    private PrimJourneyPlanDto.LegDto leg(String sectionId, String sectionType, String commercialMode, String lineCode,
            String originLabel, String destinationLabel,
            Double originLat, Double originLon, Double destinationLat, Double destinationLon,
            Integer durationSeconds,
            List<PrimJourneyPlanDto.StopDateTimeDto> stopDateTimes) {
        OffsetDateTime now = OffsetDateTime.now();
        return new PrimJourneyPlanDto.LegDto(
                1,
                sectionId,
                sectionType,
                commercialMode,
                lineCode,
                commercialMode,
                "#000000",
                "Network",
                now,
                now.plusMinutes(3),
                durationSeconds,
                "origin-stop",
                originLabel,
                originLat,
                originLon,
                "destination-stop",
                destinationLabel,
                destinationLat,
                destinationLon,
                null,
                true,
                stopDateTimes);
    }

    private PrimJourneyPlanDto.StopDateTimeDto stop(String stopPointId, String stopPointName, String stopAreaId,
            Double lat, Double lon, OffsetDateTime arrival, OffsetDateTime departure) {
        return new PrimJourneyPlanDto.StopDateTimeDto(
                stopPointId,
                stopPointName,
                stopAreaId,
                lat,
                lon,
                arrival,
                departure);
    }
}
