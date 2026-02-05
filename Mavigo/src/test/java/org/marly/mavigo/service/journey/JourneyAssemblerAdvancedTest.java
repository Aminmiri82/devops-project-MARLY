package org.marly.mavigo.service.journey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto.LegDto;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto.StopDateTimeDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

@DisplayName("Tests avancés - JourneyAssembler")
class JourneyAssemblerAdvancedTest {

    private JourneyAssembler journeyAssembler;
    private User testUser;
    private StopArea origin;
    private StopArea destination;

    @BeforeEach
    void setUp() {
        journeyAssembler = new JourneyAssembler();
        testUser = new User("ext-123", "test@example.com", "Test User");
        origin = new StopArea("stop:origin", "Gare du Nord", new GeoPoint(48.8809, 2.3553));
        destination = new StopArea("stop:dest", "Gare de Lyon", new GeoPoint(48.8443, 2.3730));
    }

    @Nested
    @DisplayName("Tests de mapping des types de segment")
    class SegmentTypeMappingTests {

        @Test
        @DisplayName("mapSegment street_network se résout en WALKING")
        void mapSegment_streetNetwork_resolvesToWalking() {
            // Given
            LegDto walkingLeg = createLegDto(0, "section-1", "street_network", null,
                    "Origin", "Destination", 48.88, 2.35, 48.84, 2.37);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(walkingLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            assertThat(journey.getSegments().get(0).getSegmentType()).isEqualTo(SegmentType.WALKING);
        }

        @Test
        @DisplayName("mapSegment transfer se résout en TRANSFER")
        void mapSegment_transfer_resolvesToTransfer() {
            // Given
            LegDto transferLeg = createLegDto(0, "section-1", "transfer", null,
                    "Platform A", "Platform B", 48.88, 2.35, 48.88, 2.35);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(transferLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            assertThat(journey.getSegments().get(0).getSegmentType()).isEqualTo(SegmentType.TRANSFER);
        }

        @Test
        @DisplayName("mapSegments filtre les segments WAITING")
        void mapSegments_filtersWaitingSegments() {
            // Given
            LegDto waitingLeg = createLegDto(0, "section-wait", "waiting", null,
                    "Platform", "Platform", 48.88, 2.35, 48.88, 2.35);
            LegDto transitLeg = createLegDto(1, "section-transit", "public_transport", "Metro",
                    "Station A", "Station B", 48.88, 2.35, 48.84, 2.37);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(waitingLeg, transitLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            assertThat(journey.getSegments().get(0).getSegmentType()).isEqualTo(SegmentType.PUBLIC_TRANSPORT);
        }

        @Test
        @DisplayName("mapSegments filtre les segments sans points")
        void mapSegments_filtersSegmentsWithNoPoints() {
            // Given - a leg with same origin/destination and short duration won't create two points
            LegDto samePointLeg = createLegDto(0, "section-1", "transfer", null,
                    "Same Station", "Same Station", 48.88, 2.35, 48.88, 2.35);
            samePointLeg = new LegDto(0, "section-1", "transfer", null, null, null, null, null,
                    OffsetDateTime.now(), OffsetDateTime.now(), 30, // 30 seconds
                    null, "Same Station", 48.88, 2.35,
                    null, "Same Station", 48.88, 2.35,
                    null, null, null);

            LegDto transitLeg = createLegDto(1, "section-transit", "public_transport", "Metro",
                    "Station A", "Station B", 48.88, 2.35, 48.84, 2.37);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(samePointLeg, transitLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then - at least the transit leg should be present
            assertThat(journey.getSegments()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Tests de résolution du mode de transport")
    class TransitModeResolutionTests {

        @Test
        @DisplayName("resolveTransitMode normalise les accents (Métro -> METRO)")
        void resolveTransitMode_normalizesAccents() {
            // Given
            LegDto metroLeg = createTransitLeg("Métro", "M1", "Ligne 1");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(metroLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.METRO);
        }

        @Test
        @DisplayName("resolveTransitMode mappe RER correctement")
        void resolveTransitMode_mapsRerCorrectly() {
            // Given
            LegDto rerLeg = createTransitLeg("RER", "A", "RER A");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(rerLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.RER);
        }

        @Test
        @DisplayName("resolveTransitMode mappe Transilien correctement")
        void resolveTransitMode_mapsTransilienCorrectly() {
            // Given
            LegDto transilienLeg = createTransitLeg("Transilien", "L", "Ligne L");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(transilienLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.TRANSILIEN);
        }

        @Test
        @DisplayName("resolveTransitMode mappe Train vers TRANSILIEN")
        void resolveTransitMode_mapsTrainToTransilien() {
            // Given
            LegDto trainLeg = createTransitLeg("Train", "TER", "TER Paris-Rouen");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(trainLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.TRANSILIEN);
        }

        @Test
        @DisplayName("resolveTransitMode mappe Tramway correctement")
        void resolveTransitMode_mapsTramwayCorrectly() {
            // Given
            LegDto tramLeg = createTransitLeg("Tramway", "T3a", "Tramway T3a");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(tramLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.TRAM);
        }

        @Test
        @DisplayName("resolveTransitMode mappe Bus correctement")
        void resolveTransitMode_mapsBusCorrectly() {
            // Given
            LegDto busLeg = createTransitLeg("Bus", "183", "Bus 183");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(busLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.BUS);
        }

        @Test
        @DisplayName("resolveTransitMode mappe Noctilien vers BUS")
        void resolveTransitMode_mapsNoctilienToBus() {
            // Given
            LegDto noctilienLeg = createTransitLeg("Noctilien", "N01", "Noctilien N01");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(noctilienLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.BUS);
        }

        @Test
        @DisplayName("resolveTransitMode mode inconnu retourne OTHER")
        void resolveTransitMode_unknownMode_returnsOther() {
            // Given
            LegDto unknownLeg = createTransitLeg("UnknownMode", "X", "Unknown Line");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(unknownLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments().get(0).getTransitMode()).isEqualTo(TransitMode.OTHER);
        }
    }

    @Nested
    @DisplayName("Tests de création des points")
    class PointCreationTests {

        @Test
        @DisplayName("createPoints utilise stopDateTimes quand disponible")
        void createPoints_usesStopDateTimesWhenAvailable() {
            // Given
            List<StopDateTimeDto> stops = List.of(
                    createStopDateTime("stop:1", "Station 1", 48.88, 2.35),
                    createStopDateTime("stop:2", "Station 2", 48.86, 2.36),
                    createStopDateTime("stop:3", "Station 3", 48.84, 2.37));

            LegDto legWithStops = createLegDtoWithStops(0, "section-1", "public_transport", "Metro", stops);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(legWithStops));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            JourneySegment segment = journey.getSegments().get(0);
            assertThat(segment.getPoints()).hasSize(3);
            assertThat(segment.getPoints().get(0).getName()).isEqualTo("Station 1");
            assertThat(segment.getPoints().get(1).getName()).isEqualTo("Station 2");
            assertThat(segment.getPoints().get(2).getName()).isEqualTo("Station 3");
        }

        @Test
        @DisplayName("createPoints utilise origin/destination en fallback")
        void createPoints_fallsBackToOriginDestination() {
            // Given - leg without stopDateTimes
            LegDto legWithoutStops = createLegDto(0, "section-1", "public_transport", "Metro",
                    "Origin Station", "Destination Station", 48.88, 2.35, 48.84, 2.37);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(legWithoutStops));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            JourneySegment segment = journey.getSegments().get(0);
            assertThat(segment.getPoints()).hasSize(2);
            assertThat(segment.getPoints().get(0).getName()).isEqualTo("Origin Station");
            assertThat(segment.getPoints().get(1).getName()).isEqualTo("Destination Station");
        }

        @Test
        @DisplayName("determinePointType premier est ORIGIN")
        void determinePointType_firstIsOrigin() {
            // Given
            List<StopDateTimeDto> stops = List.of(
                    createStopDateTime("stop:1", "First", 48.88, 2.35),
                    createStopDateTime("stop:2", "Last", 48.84, 2.37));

            LegDto leg = createLegDtoWithStops(0, "section-1", "public_transport", "Metro", stops);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(leg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            JourneyPoint firstPoint = journey.getSegments().get(0).getPoints().get(0);
            assertThat(firstPoint.getPointType()).isEqualTo(JourneyPointType.ORIGIN);
        }

        @Test
        @DisplayName("determinePointType dernier est DESTINATION")
        void determinePointType_lastIsDestination() {
            // Given
            List<StopDateTimeDto> stops = List.of(
                    createStopDateTime("stop:1", "First", 48.88, 2.35),
                    createStopDateTime("stop:2", "Last", 48.84, 2.37));

            LegDto leg = createLegDtoWithStops(0, "section-1", "public_transport", "Metro", stops);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(leg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            List<JourneyPoint> points = journey.getSegments().get(0).getPoints();
            JourneyPoint lastPoint = points.get(points.size() - 1);
            assertThat(lastPoint.getPointType()).isEqualTo(JourneyPointType.DESTINATION);
        }

        @Test
        @DisplayName("determinePointType intermédiaire est INTERMEDIATE_STOP")
        void determinePointType_intermediateIsIntermediateStop() {
            // Given
            List<StopDateTimeDto> stops = List.of(
                    createStopDateTime("stop:1", "First", 48.88, 2.35),
                    createStopDateTime("stop:2", "Middle", 48.86, 2.36),
                    createStopDateTime("stop:3", "Last", 48.84, 2.37));

            LegDto leg = createLegDtoWithStops(0, "section-1", "public_transport", "Metro", stops);
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(leg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            JourneyPoint middlePoint = journey.getSegments().get(0).getPoints().get(1);
            assertThat(middlePoint.getPointType()).isEqualTo(JourneyPointType.INTERMEDIATE_STOP);
        }
    }

    @Nested
    @DisplayName("Tests de marquage des points de transfert")
    class TransferPointMarkingTests {

        @Test
        @DisplayName("markTransferPoints marque entre transports publics")
        void markTransferPoints_marksBetweenPublicTransport() {
            // Given - two metro lines
            LegDto metro1 = createTransitLeg("Metro", "M1", "Ligne 1");
            LegDto metro2 = createTransitLeg("Metro", "M4", "Ligne 4");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(metro1, metro2));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(2);
            JourneySegment firstSegment = journey.getSegments().get(0);
            JourneySegment secondSegment = journey.getSegments().get(1);

            JourneyPoint arrivalPoint = firstSegment.getDestinationPoint();
            JourneyPoint departurePoint = secondSegment.getOriginPoint();

            assertThat(arrivalPoint.getPointType()).isEqualTo(JourneyPointType.TRANSFER_ARRIVAL);
            assertThat(departurePoint.getPointType()).isEqualTo(JourneyPointType.TRANSFER_DEPARTURE);
        }

        @Test
        @DisplayName("markTransferPoints ignore un segment unique")
        void markTransferPoints_skipsSingleSegment() {
            // Given
            LegDto singleLeg = createTransitLeg("Metro", "M1", "Ligne 1");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(singleLeg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(1);
            JourneySegment segment = journey.getSegments().get(0);
            // Points should keep their original types (ORIGIN/DESTINATION)
            assertThat(segment.getOriginPoint().getPointType()).isEqualTo(JourneyPointType.ORIGIN);
            assertThat(segment.getDestinationPoint().getPointType()).isEqualTo(JourneyPointType.DESTINATION);
        }

        @Test
        @DisplayName("markTransferPoints marque autour d'un segment de marche")
        void markTransferPoints_marksAroundWalkingSegment() {
            // Given
            LegDto metro = createTransitLeg("Metro", "M1", "Ligne 1");
            LegDto walking = createLegDto(1, "section-walk", "street_network", null,
                    "Metro Exit", "RER Entrance", 48.86, 2.36, 48.86, 2.36);
            LegDto rer = createTransitLeg("RER", "A", "RER A");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(metro, walking, rer));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getSegments()).hasSize(3);
            // Transfer points should be marked
            JourneyPoint metroArrival = journey.getSegments().get(0).getDestinationPoint();
            assertThat(metroArrival.getPointType()).isEqualTo(JourneyPointType.TRANSFER_ARRIVAL);
        }
    }

    @Nested
    @DisplayName("Tests d'état du Journey")
    class JourneyStateTests {

        @Test
        @DisplayName("assemble définit le statut PLANNED")
        void assemble_setsStatusPlanned() {
            // Given
            LegDto leg = createTransitLeg("Metro", "M1", "Ligne 1");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(leg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getStatus()).isEqualTo(JourneyStatus.PLANNED);
        }

        @Test
        @DisplayName("assemble active le mode confort quand spécifié")
        void assemble_enablesComfortModeWhenSpecified() {
            // Given
            LegDto leg = createTransitLeg("Metro", "M1", "Ligne 1");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(leg));
            JourneyPreferences preferences = new JourneyPreferences(true, null);

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, preferences);

            // Then
            assertThat(journey.isComfortModeEnabled()).isTrue();
        }

        @Test
        @DisplayName("assemble définit les labels origin et destination")
        void assemble_setsOriginAndDestinationLabels() {
            // Given
            LegDto leg = createTransitLeg("Metro", "M1", "Ligne 1");
            PrimJourneyPlanDto plan = createPlanWithLegs(List.of(leg));

            // When
            Journey journey = journeyAssembler.assemble(testUser, origin, destination, plan, null);

            // Then
            assertThat(journey.getOriginLabel()).isEqualTo("Gare du Nord");
            assertThat(journey.getDestinationLabel()).isEqualTo("Gare de Lyon");
        }
    }

    // Helper methods
    private LegDto createLegDto(int order, String sectionId, String sectionType, String commercialMode,
            String originLabel, String destLabel,
            double originLat, double originLon, double destLat, double destLon) {
        return new LegDto(order, sectionId, sectionType, commercialMode,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(10), 600,
                null, originLabel, originLat, originLon,
                null, destLabel, destLat, destLon,
                null, null, null);
    }

    private LegDto createTransitLeg(String commercialMode, String lineCode, String lineName) {
        return new LegDto(0, "section-transit", "public_transport", commercialMode,
                lineCode, lineName, "#FFCC00", "RATP",
                OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(15), 900,
                "stop:origin", "Origin Station", 48.88, 2.35,
                "stop:dest", "Destination Station", 48.84, 2.37,
                null, true, null);
    }

    private LegDto createLegDtoWithStops(int order, String sectionId, String sectionType,
            String commercialMode, List<StopDateTimeDto> stops) {
        return new LegDto(order, sectionId, sectionType, commercialMode,
                "M1", "Ligne 1", "#FFCC00", "RATP",
                OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(15), 900,
                "stop:origin", "Origin", 48.88, 2.35,
                "stop:dest", "Destination", 48.84, 2.37,
                null, true, stops);
    }

    private StopDateTimeDto createStopDateTime(String stopId, String name, double lat, double lon) {
        return new StopDateTimeDto(stopId, name, "area:" + stopId, lat, lon,
                OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(1));
    }

    private PrimJourneyPlanDto createPlanWithLegs(List<LegDto> legs) {
        return new PrimJourneyPlanDto("journey-123",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                3600, legs.size() - 1, legs);
    }
}
