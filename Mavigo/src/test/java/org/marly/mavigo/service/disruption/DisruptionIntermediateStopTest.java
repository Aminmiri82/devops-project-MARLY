package org.marly.mavigo.service.disruption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.DisruptionRepository;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.journey.JourneyAssembler;
import org.marly.mavigo.service.journey.JourneyResultFilter;
import org.marly.mavigo.service.stoparea.StopAreaService;

class DisruptionIntermediateStopTest {

        private JourneyRepository journeyRepository;
        private DisruptionRepository disruptionRepository;
        private PrimApiClient primApiClient;
        private StopAreaService stopAreaService;
        private JourneyAssembler journeyAssembler;
        private JourneyResultFilter journeyResultFilter;
        private DisruptionReportingService disruptionService;

        @BeforeEach
        void setUp() {
                journeyRepository = mock(JourneyRepository.class);
                disruptionRepository = mock(DisruptionRepository.class);
                primApiClient = mock(PrimApiClient.class);
                stopAreaService = mock(StopAreaService.class);
                journeyAssembler = mock(JourneyAssembler.class);
                journeyResultFilter = mock(JourneyResultFilter.class);

                disruptionService = new DisruptionReportingService(
                                journeyRepository, disruptionRepository, primApiClient,
                                stopAreaService, journeyAssembler, journeyResultFilter);

                when(journeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void testStationDisruptionWithPendingIntermediateStop() {
                UUID journeyId = UUID.randomUUID();
                User user = new User("ext-1", "user@example.com", "Test User");
                user.setId(UUID.randomUUID());

                Journey original = new Journey(user, "A", "C", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
                original.setIntermediateQuery("B");

                JourneySegment seg = new JourneySegment(original, 0, SegmentType.PUBLIC_TRANSPORT);
                JourneyPoint pA = new JourneyPoint(seg, 0, JourneyPointType.ORIGIN, "A");
                pA.setPrimStopPointId("SP-A");

                JourneyPoint pDisrupted = new JourneyPoint(seg, 1, JourneyPointType.INTERMEDIATE_STOP, "D");
                pDisrupted.setPrimStopPointId("SP-D");

                JourneyPoint pNext = new JourneyPoint(seg, 2, JourneyPointType.INTERMEDIATE_STOP, "E");
                pNext.setPrimStopPointId("SP-E");

                // Use reflection to set IDs for getNextPointAfter
                setInternalId(pA, UUID.randomUUID());
                setInternalId(pDisrupted, UUID.randomUUID());
                setInternalId(pNext, UUID.randomUUID());

                seg.addPoint(pA);
                seg.addPoint(pDisrupted);
                seg.addPoint(pNext);
                original.addSegment(seg);

                when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(original));
                when(disruptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                StopArea areaE = new StopArea("area-e", "area-e", null);
                StopArea areaB = new StopArea("area-b", "area-b", null);
                StopArea areaC = new StopArea("area-c", "area-c", null);

                when(stopAreaService.findOrCreateByQuery("E")).thenReturn(areaE);
                when(stopAreaService.findOrCreateByQuery("B")).thenReturn(areaB);
                when(stopAreaService.findOrCreateByQuery("C")).thenReturn(areaC);

                // Mock PRIM calls (two legs: E -> B and B -> C)
                PrimJourneyPlanDto leg1 = mock(PrimJourneyPlanDto.class);
                when(leg1.durationSeconds()).thenReturn(1800);
                when(primApiClient.calculateJourneyPlans(any(PrimJourneyRequest.class)))
                                .thenReturn(List.of(leg1)) // E -> B
                                .thenReturn(List.of(mock(PrimJourneyPlanDto.class))); // B -> C

                // Mock assembly
                Journey leg1J = new Journey(user, "E", "B", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30));
                Journey leg2J = new Journey(user, "B", "C", OffsetDateTime.now().plusMinutes(35),
                                OffsetDateTime.now().plusHours(1));
                when(journeyAssembler.assemble(any(), any(), any(), any(), any()))
                                .thenReturn(leg1J)
                                .thenReturn(leg2J);

                var result = disruptionService.reportStationDisruption(journeyId, "SP-D");

                assertNotNull(result);
                assertNotNull(result.alternatives());
                assertEquals(1, result.alternatives().size());
                Journey rerouted = result.alternatives().get(0);
                assertEquals("B", rerouted.getIntermediateQuery());
                assertEquals("C", rerouted.getDestinationLabel());
        }

        @Test
        void testLineDisruptionWithPendingIntermediateStop() {
                UUID journeyId = UUID.randomUUID();
                User user = new User("ext-1", "user@example.com", "Test User");
                user.setId(UUID.randomUUID());

                Journey original = new Journey(user, "A", "C", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2));
                original.setIntermediateQuery("B");

                JourneySegment seg = new JourneySegment(original, 0, SegmentType.PUBLIC_TRANSPORT);
                JourneyPoint pA = new JourneyPoint(seg, 0, JourneyPointType.ORIGIN, "A");
                pA.setPrimStopPointId("SP-A");
                seg.addPoint(pA);
                original.addSegment(seg);

                when(journeyRepository.findWithSegmentsById(journeyId)).thenReturn(Optional.of(original));
                when(disruptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                StopArea areaB = new StopArea("area-b", "area-b", null);
                StopArea areaC = new StopArea("area-c", "area-c", null);
                when(stopAreaService.findOrCreateByQuery("A")).thenReturn(new StopArea("area-a", "area-a", null));
                when(stopAreaService.findOrCreateByQuery("B")).thenReturn(areaB);
                when(stopAreaService.findOrCreateByQuery("C")).thenReturn(areaC);

                // Mock PRIM calls
                PrimJourneyPlanDto leg1 = mock(PrimJourneyPlanDto.class);
                when(leg1.durationSeconds()).thenReturn(1800);
                when(primApiClient.calculateJourneyPlans(any()))
                                .thenReturn(List.of(leg1));

                // Mock assembly
                Journey leg1J = new Journey(user, "A", "B", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30));
                Journey leg2J = new Journey(user, "B", "C", OffsetDateTime.now().plusMinutes(35),
                                OffsetDateTime.now().plusHours(1));
                when(journeyAssembler.assemble(any(), any(), any(), any(), any()))
                                .thenReturn(leg1J)
                                .thenReturn(leg2J);

                var result = disruptionService.reportLineDisruption(journeyId, "LINE-M1");

                assertNotNull(result);
                assertEquals(1, result.alternatives().size());
                Journey rerouted = result.alternatives().get(0);
                assertEquals("B", rerouted.getIntermediateQuery());
        }

        private void setInternalId(Object target, UUID id) {
                try {
                        java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(target, id);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
        }
}
