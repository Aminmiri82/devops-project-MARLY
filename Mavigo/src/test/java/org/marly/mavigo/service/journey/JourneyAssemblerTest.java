package org.marly.mavigo.service.journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JourneyAssemblerTest {

    private JourneyAssembler assembler;
    private User user;
    private StopArea origin;
    private StopArea destination;

    @BeforeEach
    void setUp() {
        assembler = new JourneyAssembler();
        user = new User("ext-1", "test@test.com", "Test");
        origin = new StopArea("id-1", "Origin", new GeoPoint(10.0, 10.0));
        destination = new StopArea("id-2", "Destination", new GeoPoint(20.0, 20.0));
    }

    @Test
    void assemble_ShouldCreateJourney_WithCorrectFields() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        // Updated LegDto constructor to match definition
        PrimJourneyPlanDto.LegDto leg = new PrimJourneyPlanDto.LegDto(
                1, 
                "section-1",
                "public_transport",
                "metro", 
                "M1", 
                now, 
                now.plusMinutes(10), 
                600, 
                "stop-1",
                 "O", 
                10.0, 
                10.0, 
                "stop-2",
                "D", 
                10.1, 
                10.1, 
                "Notes",
                null
        );
        // Updated PrimJourneyPlanDto constructor to match definition (added transfers=0)
        PrimJourneyPlanDto plan = new PrimJourneyPlanDto(
                "journey-1", now, now.plusMinutes(10), 600, 0, List.of(leg)
        );
        JourneyPreferences prefs = new JourneyPreferences(true, false);

        // When
        Journey journey = assembler.assemble(user, origin, destination, plan, prefs);

        // Then
        assertNotNull(journey);
        assertEquals(user, journey.getUser());
        assertEquals("Origin", journey.getOriginLabel());
        assertEquals("Destination", journey.getDestinationLabel());
        // Updated method names
        assertEquals(now, journey.getPlannedDeparture());
        assertEquals(now.plusMinutes(10), journey.getPlannedArrival());
        assertTrue(journey.isComfortModeEnabled());
        assertFalse(journey.isTouristicModeEnabled());
        assertEquals("journey-1", journey.getPrimItineraryId());
        assertEquals(1, journey.getLegs().size());
        assertEquals(TransitMode.METRO, journey.getLegs().get(0).getMode());
    }

    @Test
    void assemble_ShouldHandleNullPreferences() {
        PrimJourneyPlanDto plan = new PrimJourneyPlanDto(
                "j1", OffsetDateTime.now(), OffsetDateTime.now(), 0, 0, Collections.emptyList()
        );

        Journey journey = assembler.assemble(user, origin, destination, plan, null);

        assertNotNull(journey);
        assertFalse(journey.isComfortModeEnabled());
    }

    @Test
    void assemble_ShouldResolveCoordinatesFromLegs_WhenStopCoordinatesMissing() {
        StopArea unknownOrigin = new StopArea("o", "O", null);
        StopArea unknownDest = new StopArea("d", "D", null);
        OffsetDateTime now = OffsetDateTime.now();
        
        PrimJourneyPlanDto.LegDto leg = new PrimJourneyPlanDto.LegDto(
                1, 
                "section-1",
                "public_transport",
                "walk", 
                null, 
                now, 
                now.plusMinutes(10), 
                600, 
                "stop-1",
                 "O", 
                12.0, 
                13.0, 
                "stop-2",
                "D", 
                14.0, 
                15.0, 
                null,
                null
        );
        PrimJourneyPlanDto plan = new PrimJourneyPlanDto(
                "j1", now, now.plusMinutes(10), 0, 0, List.of(leg)
        );

        Journey journey = assembler.assemble(user, unknownOrigin, unknownDest, plan, null);

        assertNotNull(journey.getOriginCoordinate());
        assertEquals(12.0, journey.getOriginCoordinate().getLatitude());
        assertEquals(13.0, journey.getOriginCoordinate().getLongitude());
        
        assertNotNull(journey.getDestinationCoordinate());
        assertEquals(14.0, journey.getDestinationCoordinate().getLatitude());
        assertEquals(15.0, journey.getDestinationCoordinate().getLongitude());
    }
}
