package org.marly.mavigo.service.disruption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.DisruptionRepository;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.disruption.dto.LineInfo;
import org.marly.mavigo.service.disruption.dto.RerouteResult;
import org.marly.mavigo.service.disruption.dto.StopInfo;
import org.marly.mavigo.service.journey.JourneyAssembler;
import org.marly.mavigo.service.journey.JourneyResultFilter;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisruptionReportingServiceTest {

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
        journeyId = UUID.randomUUID();
    }

    @Test
    void getLinesForJourneyReturnsDistinctLines() {
        Journey journey = new Journey(user, "A", "B", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment seg1 = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        seg1.setLineCode("M1");
        seg1.setLineName("Line 1");

        JourneySegment seg2 = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        seg2.setLineCode("M1"); // Duplicate line

        JourneySegment seg3 = new JourneySegment(journey, 2, SegmentType.PUBLIC_TRANSPORT);
        seg3.setLineCode("RER A");
        seg3.setLineName("RER A");

        journey.addSegment(seg1);
        journey.addSegment(seg2);
        journey.addSegment(seg3);

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));

        List<LineInfo> lines = disruptionReportingService.getLinesForJourney(journeyId);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).lineCode()).isEqualTo("M1");
        assertThat(lines.get(1).lineCode()).isEqualTo("RER A");
    }

    @Test
    void getStopsForJourneyReturnsAllStopsAcrossSegments() {
        Journey journey = new Journey(user, "A", "B", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        JourneySegment seg1 = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        seg1.setLineCode("M1");
        JourneyPoint p1 = new JourneyPoint(seg1, 0, JourneyPointType.ORIGIN, "Stop 1");
        p1.setPrimStopPointId("S1");
        seg1.addPoint(p1);
        JourneyPoint p2 = new JourneyPoint(seg1, 1, JourneyPointType.DESTINATION, "Stop 2");
        p2.setPrimStopPointId("S2");
        seg1.addPoint(p2);

        journey.addSegment(seg1);

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));

        List<StopInfo> stops = disruptionReportingService.getStopsForJourney(journeyId);

        assertThat(stops).hasSize(2);
        assertThat(stops.get(0).primStopPointId()).isEqualTo("S1");
        assertThat(stops.get(1).primStopPointId()).isEqualTo("S2");
    }

    @Test
    void reportStationDisruptionMarksPointAsDisrupted() {
        Journey journey = new Journey(user, "A", "B", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        JourneySegment seg1 = new JourneySegment(journey, 0, SegmentType.PUBLIC_TRANSPORT);
        JourneyPoint p1 = mock(JourneyPoint.class);
        when(p1.getPrimStopPointId()).thenReturn("S1");
        seg1.addPoint(p1);
        journey.addSegment(seg1);

        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
        when(disruptionRepository.save(any(Disruption.class))).thenAnswer(i -> i.getArguments()[0]);

        RerouteResult result = disruptionReportingService.reportStationDisruption(journeyId, "S1");

        assertThat(result.disruptedPoint()).isEqualTo(p1);
        verify(p1).markDisrupted();
        verify(disruptionRepository).save(any(Disruption.class));
    }

    @Test
    void reportLineDisruptionSavesDisruption() {
        Journey journey = new Journey(user, "A", "B", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(journey));
        when(disruptionRepository.save(any(Disruption.class))).thenAnswer(i -> i.getArguments()[0]);

        RerouteResult result = disruptionReportingService.reportLineDisruption(journeyId, "M1");

        assertThat(result.disruption()).isNotNull();
        assertThat(journey.getDisruptions()).hasSize(1);
        verify(disruptionRepository).save(any(Disruption.class));
    }
}
