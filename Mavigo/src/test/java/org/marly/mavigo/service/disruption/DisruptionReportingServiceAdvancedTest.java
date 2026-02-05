package org.marly.mavigo.service.disruption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto.LegDto;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.DisruptionRepository;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.disruption.dto.RerouteResult;
import org.marly.mavigo.service.journey.JourneyAssembler;
import org.marly.mavigo.service.journey.JourneyResultFilter;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Tests avancés - DisruptionReportingService")
class DisruptionReportingServiceAdvancedTest {

    @Mock
    private JourneyRepository journeyRepository;

    @Mock
    private DisruptionRepository disruptionRepository;

    @Mock
    private PrimApiClient primApiClient;

    @Mock
    private StopAreaService stopAreaService;

    @Mock
    private JourneyAssembler journeyAssembler;

    @Mock
    private JourneyResultFilter journeyResultFilter;

    @InjectMocks
    private DisruptionReportingService disruptionReportingService;

    private User user;
    private UUID journeyId;

    @BeforeEach
    void setUp() {
        user = new User("user-1", "test@example.com", "Test User");
        user.setId(UUID.randomUUID());
        journeyId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Tests reportStationDisruption")
    class ReportStationDisruptionTests {

        @Test
        @DisplayName("reportStationDisruption lève une exception quand trajet non trouvé")
        void reportStationDisruption_throwsWhenJourneyNotFound() {
            // Given
            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> disruptionReportingService.reportStationDisruption(journeyId, "S1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Journey not found");
        }

        @Test
        @DisplayName("reportStationDisruption lève une exception quand stop non trouvé")
        void reportStationDisruption_throwsWhenStopNotFound() {
            // Given
            Journey journey = createJourneyWithPoints();

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));

            // When/Then
            assertThatThrownBy(() -> disruptionReportingService.reportStationDisruption(journeyId, "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stop not found");
        }

        @Test
        @DisplayName("reportStationDisruption sans point suivant retourne pas d'alternatives")
        void reportStationDisruption_noNextPointReturnsNoAlternatives() {
            // Given - journey with only one point (the disrupted one is the last)
            Journey journey = createJourneyWithSinglePoint();
            JourneyPoint onlyPoint = journey.getSegments().get(0).getPoints().get(0);
            onlyPoint.setPrimStopPointId("S1");

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
            when(disruptionRepository.save(any(Disruption.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            RerouteResult result = disruptionReportingService.reportStationDisruption(journeyId, "S1");

            // Then
            assertThat(result.alternatives()).isEmpty();
            assertThat(result.disruptedPoint()).isNotNull();
        }

        @Test
        @DisplayName("reportStationDisruption marque le point comme perturbé avec stopAreaId")
        void reportStationDisruption_marksPointAsDisruptedWithStopAreaId() {
            // Given - journey with only one point (the disrupted one is the last, so no rerouting)
            Journey journey = createJourneyWithSinglePoint();
            JourneyPoint point = journey.getSegments().get(0).getPoints().get(0);
            point.setPrimStopPointId("S1");
            point.setPrimStopAreaId("stop-area:S1");

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
            when(disruptionRepository.save(any(Disruption.class))).thenAnswer(i -> i.getArguments()[0]);

            // When - use stopAreaId as fallback
            RerouteResult result = disruptionReportingService.reportStationDisruption(journeyId, "stop-area:S1");

            // Then
            assertThat(result.disruptedPoint()).isNotNull();
            assertThat(result.disruptedPoint().isDisrupted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests reportLineDisruption")
    class ReportLineDisruptionTests {

        @Test
        @DisplayName("reportLineDisruption lève une exception quand trajet non trouvé")
        void reportLineDisruption_throwsWhenJourneyNotFound() {
            // Given
            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> disruptionReportingService.reportLineDisruption(journeyId, "M1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Journey not found");
        }

        @Test
        @DisplayName("reportLineDisruption crée une perturbation")
        void reportLineDisruption_createsDisruption() {
            // Given
            Journey journey = createJourneyWithPoints();

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
            when(disruptionRepository.save(any(Disruption.class))).thenAnswer(i -> i.getArguments()[0]);
            when(stopAreaService.findOrCreateByQuery("Origin"))
                    .thenReturn(new StopArea("stop:origin", "Origin", new GeoPoint(48.88, 2.35)));
            when(stopAreaService.findOrCreateByQuery("Destination"))
                    .thenReturn(new StopArea("stop:dest", "Destination", new GeoPoint(48.84, 2.37)));
            when(primApiClient.calculateJourneyPlans(any())).thenReturn(List.of());
            when(journeyResultFilter.filterByComfortProfile(any(), any(), anyBoolean())).thenReturn(List.of());

            // When
            RerouteResult result = disruptionReportingService.reportLineDisruption(journeyId, "M1");

            // Then
            assertThat(result.disruption()).isNotNull();
        }

        @Test
        @DisplayName("reportLineDisruption retourne liste vide si aucune alternative")
        void reportLineDisruption_returnsEmptyIfNoAlternatives() {
            // Given
            Journey journey = createJourneyWithPoints();

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
            when(disruptionRepository.save(any(Disruption.class))).thenAnswer(i -> i.getArguments()[0]);
            when(stopAreaService.findOrCreateByQuery("Origin"))
                    .thenReturn(new StopArea("stop:origin", "Origin", new GeoPoint(48.88, 2.35)));
            when(stopAreaService.findOrCreateByQuery("Destination"))
                    .thenReturn(new StopArea("stop:dest", "Destination", new GeoPoint(48.84, 2.37)));
            when(primApiClient.calculateJourneyPlans(any())).thenReturn(List.of());
            when(journeyResultFilter.filterByComfortProfile(any(), any(), anyBoolean())).thenReturn(List.of());

            // When
            RerouteResult result = disruptionReportingService.reportLineDisruption(journeyId, "M1");

            // Then
            assertThat(result.alternatives()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests getLinesForJourney")
    class GetLinesForJourneyTests {

        @Test
        @DisplayName("getLinesForJourney retourne les lignes uniques")
        void getLinesForJourney_returnsUniqueLines() {
            // Given
            Journey journey = createJourneyWithMultipleSegments();

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));

            // When
            var lines = disruptionReportingService.getLinesForJourney(journeyId);

            // Then
            assertThat(lines).hasSize(2);
            assertThat(lines).extracting("lineCode").containsExactlyInAnyOrder("M1", "M4");
        }

        @Test
        @DisplayName("getLinesForJourney exclut les segments de marche")
        void getLinesForJourney_excludesWalkingSegments() {
            // Given
            Journey journey = createJourneyWithWalkingSegment();

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));

            // When
            var lines = disruptionReportingService.getLinesForJourney(journeyId);

            // Then
            assertThat(lines).hasSize(1);
            assertThat(lines.get(0).lineCode()).isEqualTo("M1");
        }
    }

    @Nested
    @DisplayName("Tests getStopsForJourney")
    class GetStopsForJourneyTests {

        @Test
        @DisplayName("getStopsForJourney retourne les arrêts des segments de transport")
        void getStopsForJourney_returnsStopsFromPublicTransport() {
            // Given
            Journey journey = createJourneyWithPoints();

            when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));

            // When
            var stops = disruptionReportingService.getStopsForJourney(journeyId);

            // Then
            assertThat(stops).hasSize(2);
        }
    }

    // Helper methods
    private Journey createJourneyWithPoints() {
        Journey journey = new Journey(user, "Origin", "Destination",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment segment = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");
        segment.setLineName("Ligne 1");

        JourneyPoint p1 = new JourneyPoint(segment, 0, JourneyPointType.ORIGIN, "Stop 1");
        p1.setPrimStopPointId("S1");
        p1.setCoordinates(new GeoPoint(48.88, 2.35));
        segment.addPoint(p1);

        JourneyPoint p2 = new JourneyPoint(segment, 1, JourneyPointType.DESTINATION, "Stop 2");
        p2.setPrimStopPointId("S2");
        p2.setCoordinates(new GeoPoint(48.84, 2.37));
        segment.addPoint(p2);

        journey.addSegment(segment);

        return journey;
    }

    private Journey createJourneyWithSinglePoint() {
        Journey journey = new Journey(user, "Origin", "Destination",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment segment = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");

        JourneyPoint p1 = new JourneyPoint(segment, 0, JourneyPointType.DESTINATION, "Last Stop");
        p1.setPrimStopPointId("S1");
        segment.addPoint(p1);

        journey.addSegment(segment);

        return journey;
    }

    private Journey createJourneyWithMultipleSegments() {
        Journey journey = new Journey(user, "Origin", "Destination",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment segment1 = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        segment1.setLineCode("M1");
        segment1.setLineName("Ligne 1");
        segment1.setLineColor("#FFCC00");
        journey.addSegment(segment1);

        JourneySegment segment2 = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment2.setLineCode("M4");
        segment2.setLineName("Ligne 4");
        segment2.setLineColor("#FF00FF");
        journey.addSegment(segment2);

        // Duplicate line M1 should not appear twice
        JourneySegment segment3 = new JourneySegment(journey, 2, SegmentType.PUBLIC_TRANSPORT);
        segment3.setLineCode("M1");
        segment3.setLineName("Ligne 1");
        journey.addSegment(segment3);

        return journey;
    }

    private Journey createJourneyWithWalkingSegment() {
        Journey journey = new Journey(user, "Origin", "Destination",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment walkingSegment = new JourneySegment(journey, 0, SegmentType.WALKING);
        journey.addSegment(walkingSegment);

        JourneySegment transportSegment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        transportSegment.setLineCode("M1");
        transportSegment.setLineName("Ligne 1");
        journey.addSegment(transportSegment);

        return journey;
    }
}
