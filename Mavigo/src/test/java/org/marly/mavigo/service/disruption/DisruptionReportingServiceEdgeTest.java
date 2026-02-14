package org.marly.mavigo.service.disruption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
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

@ExtendWith(MockitoExtension.class)
class DisruptionReportingServiceEdgeTest {

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
    private DisruptionReportingService service;

    private UUID journeyId;
    private User user;

    @BeforeEach
    void setUp() {
        journeyId = UUID.randomUUID();
        user = new User("ext-user", "user@example.com", "User");
        user.setId(UUID.randomUUID());
    }

    @Test
    void reportStationDisruption_returnsNoAlternativesWhenNextPointHasNoIdOrCoordinates() throws Exception {
        Journey journey = journeyWithTwoPoints(user);
        JourneyPoint disrupted = journey.getSegments().get(0).getPoints().get(0);
        JourneyPoint next = journey.getSegments().get(0).getPoints().get(1);
        disrupted.setPrimStopPointId("sp-disrupted");
        next.setPrimStopPointId(null);
        next.setPrimStopAreaId(null);
        next.setCoordinates(null);

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
        when(disruptionRepository.save(any(Disruption.class))).thenAnswer(inv -> inv.getArgument(0));

        RerouteResult result = service.reportStationDisruption(journeyId, "sp-disrupted");

        assertThat(result.newOriginPoint()).isEqualTo(next);
        assertThat(result.alternatives()).isEmpty();
    }

    @Test
    void reportStationDisruption_setsOriginCoordinatesWhenOriginHasNone() throws Exception {
        Journey journey = journeyWithTwoPoints(user);
        JourneyPoint disrupted = journey.getSegments().get(0).getPoints().get(0);
        JourneyPoint next = journey.getSegments().get(0).getPoints().get(1);
        disrupted.setPrimStopPointId("sp-disrupted");
        next.setPrimStopPointId(null);
        next.setPrimStopAreaId(null);
        next.setCoordinates(new GeoPoint(48.8566, 2.3522));

        StopArea originStop = new StopArea("origin-ext", "Origin Fallback", null);
        StopArea destinationStop = new StopArea("dest-ext", "Destination", new GeoPoint(48.86, 2.36));
        PrimJourneyPlanDto option = plan("option-1", "M2");
        Journey assembled = new Journey(user, "Origin Fallback", "Destination",
                OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30));

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
        when(disruptionRepository.save(any(Disruption.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stopAreaService.findOrCreateByQuery(next.getName())).thenReturn(originStop);
        when(stopAreaService.findOrCreateByQuery(journey.getDestinationLabel())).thenReturn(destinationStop);
        when(primApiClient.calculateJourneyPlans(any())).thenReturn(List.of(option));
        when(journeyResultFilter.filterByComfortProfile(any(), any(), anyBoolean())).thenReturn(List.of(option));
        when(journeyAssembler.assemble(any(), any(), any(), any(), any())).thenReturn(assembled);
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        RerouteResult result = service.reportStationDisruption(journeyId, "sp-disrupted");

        assertThat(originStop.getCoordinates()).isEqualTo(next.getCoordinates());
        assertThat(result.alternatives()).hasSize(1);
    }

    @Test
    void reportLineDisruption_excludesPlansUsingImpactedLine() throws Exception {
        Journey journey = journeyWithTwoPoints(user);
        StopArea origin = new StopArea("origin-ext", "Origin", new GeoPoint(48.88, 2.35));
        StopArea destination = new StopArea("dest-ext", "Destination", new GeoPoint(48.84, 2.37));
        PrimJourneyPlanDto excluded = plan("excluded", "M1");
        PrimJourneyPlanDto kept = plan("kept", "M2");
        Journey assembled = new Journey(user, "Origin", "Destination", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(40));

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
        when(disruptionRepository.save(any(Disruption.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stopAreaService.findOrCreateByQuery(journey.getOriginLabel())).thenReturn(origin);
        when(stopAreaService.findOrCreateByQuery(journey.getDestinationLabel())).thenReturn(destination);
        when(primApiClient.calculateJourneyPlans(any())).thenReturn(List.of(excluded, kept));
        when(journeyResultFilter.filterByComfortProfile(any(), any(), anyBoolean())).thenReturn(List.of(excluded, kept));
        when(journeyAssembler.assemble(any(), any(), any(), any(), any())).thenReturn(assembled);
        when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

        RerouteResult result = service.reportLineDisruption(journeyId, "M1");

        assertThat(result.alternatives()).hasSize(1);
        verify(journeyAssembler, times(1)).assemble(any(), any(), any(), any(), any());
    }

    @Test
    void reportLineDisruption_returnsEmptyAlternativesWhenCalculationFails() throws Exception {
        Journey journey = journeyWithTwoPoints(user);
        StopArea origin = new StopArea("origin-ext", "Origin", new GeoPoint(48.88, 2.35));
        StopArea destination = new StopArea("dest-ext", "Destination", new GeoPoint(48.84, 2.37));

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
        when(disruptionRepository.save(any(Disruption.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stopAreaService.findOrCreateByQuery(journey.getOriginLabel())).thenReturn(origin);
        when(stopAreaService.findOrCreateByQuery(journey.getDestinationLabel())).thenReturn(destination);
        when(primApiClient.calculateJourneyPlans(any())).thenThrow(new RuntimeException("boom"));

        RerouteResult result = service.reportLineDisruption(journeyId, "M9");

        assertThat(result.alternatives()).isEmpty();
    }

    private Journey journeyWithTwoPoints(User owner) throws Exception {
        Journey journey = new Journey(owner, "Origin", "Destination",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment segment = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        JourneyPoint p1 = new JourneyPoint(segment, 0, JourneyPointType.ORIGIN, "Disrupted Point");
        JourneyPoint p2 = new JourneyPoint(segment, 1, JourneyPointType.DESTINATION, "New Origin Point");

        setPointId(p1, UUID.randomUUID());
        setPointId(p2, UUID.randomUUID());

        segment.addPoint(p1);
        segment.addPoint(p2);
        journey.addSegment(segment);
        return journey;
    }

    private PrimJourneyPlanDto plan(String id, String lineCode) {
        OffsetDateTime dep = OffsetDateTime.now();
        OffsetDateTime arr = dep.plusMinutes(20);
        PrimJourneyPlanDto.LegDto leg = new PrimJourneyPlanDto.LegDto(
                1, "section-" + id, "public_transport", "Metro",
                lineCode, "Line " + lineCode, "#FF0000", "Network",
                dep, arr, 1200,
                "sp-a", "A", 48.85, 2.34,
                "sp-b", "B", 48.86, 2.35,
                null, true, null);
        return new PrimJourneyPlanDto(id, dep, arr, 1200, 0, List.of(leg));
    }

    private void setPointId(JourneyPoint point, UUID id) throws Exception {
        Field field = JourneyPoint.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(point, id);
    }
}
