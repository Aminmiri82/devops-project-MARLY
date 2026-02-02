package org.marly.mavigo.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.journey.TransitMode;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;

@DisplayName("Tests unitaires - JourneySegment")
class JourneySegmentTest {

    private User testUser;
    private Journey journey;
    private JourneySegment segment;

    @BeforeEach
    void setUp() {
        testUser = new User("ext-123", "test@example.com", "Test User");
        testUser.setId(UUID.randomUUID());

        journey = new Journey(
                testUser,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));

        segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
    }

    @Test
    @DisplayName("Un nouveau JourneySegment devrait avoir les valeurs de base")
    void newSegment_shouldHaveBasicValues() {
        assertEquals(journey, segment.getJourney());
        assertEquals(1, segment.getSequenceOrder());
        assertEquals(SegmentType.PUBLIC_TRANSPORT, segment.getSegmentType());
    }

    @Test
    @DisplayName("addPoint devrait ajouter un point et établir la relation bidirectionnelle")
    void addPoint_shouldAddPointAndSetBidirectionalRelation() {
        // Given
        JourneyPoint point = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Gare de Lyon");

        // When
        segment.addPoint(point);

        // Then
        assertEquals(1, segment.getPoints().size());
        assertEquals(segment, point.getSegment());
    }

    @Test
    @DisplayName("getOriginPoint devrait retourner le premier point")
    void getOriginPoint_shouldReturnFirstPoint() {
        // Given
        JourneyPoint originPoint = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Origin");
        JourneyPoint destinationPoint = new JourneyPoint(segment, 2, JourneyPointType.DESTINATION, "Destination");

        segment.addPoint(originPoint);
        segment.addPoint(destinationPoint);

        // When
        JourneyPoint result = segment.getOriginPoint();

        // Then
        assertEquals("Origin", result.getName());
    }

    @Test
    @DisplayName("getOriginPoint devrait retourner null si pas de points")
    void getOriginPoint_shouldReturnNullWhenNoPoints() {
        assertNull(segment.getOriginPoint());
    }

    @Test
    @DisplayName("getDestinationPoint devrait retourner le dernier point")
    void getDestinationPoint_shouldReturnLastPoint() {
        // Given
        JourneyPoint originPoint = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Origin");
        JourneyPoint destinationPoint = new JourneyPoint(segment, 2, JourneyPointType.DESTINATION, "Destination");

        segment.addPoint(originPoint);
        segment.addPoint(destinationPoint);

        // When
        JourneyPoint result = segment.getDestinationPoint();

        // Then
        assertEquals("Destination", result.getName());
    }

    @Test
    @DisplayName("getDestinationPoint devrait retourner null si pas de points")
    void getDestinationPoint_shouldReturnNullWhenNoPoints() {
        assertNull(segment.getDestinationPoint());
    }

    @Test
    @DisplayName("getIntermediatePoints devrait retourner les points intermédiaires")
    void getIntermediatePoints_shouldReturnIntermediatePoints() {
        // Given
        JourneyPoint originPoint = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Origin");
        JourneyPoint intermediatePoint = new JourneyPoint(segment, 2, JourneyPointType.INTERMEDIATE_STOP, "Intermediate");
        JourneyPoint destinationPoint = new JourneyPoint(segment, 3, JourneyPointType.DESTINATION, "Destination");

        segment.addPoint(originPoint);
        segment.addPoint(intermediatePoint);
        segment.addPoint(destinationPoint);

        // When
        List<JourneyPoint> intermediatePoints = segment.getIntermediatePoints();

        // Then
        assertEquals(1, intermediatePoints.size());
        assertEquals("Intermediate", intermediatePoints.get(0).getName());
    }

    @Test
    @DisplayName("getIntermediatePoints devrait retourner une liste vide si seulement 2 points")
    void getIntermediatePoints_shouldReturnEmptyListWhenOnly2Points() {
        // Given
        JourneyPoint originPoint = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Origin");
        JourneyPoint destinationPoint = new JourneyPoint(segment, 2, JourneyPointType.DESTINATION, "Destination");

        segment.addPoint(originPoint);
        segment.addPoint(destinationPoint);

        // When
        List<JourneyPoint> intermediatePoints = segment.getIntermediatePoints();

        // Then
        assertTrue(intermediatePoints.isEmpty());
    }

    @Test
    @DisplayName("hasDisruptedPoints devrait retourner false par défaut")
    void hasDisruptedPoints_shouldReturnFalseByDefault() {
        // Given
        JourneyPoint point = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Origin");
        segment.addPoint(point);

        // When/Then
        assertFalse(segment.hasDisruptedPoints());
    }

    @Test
    @DisplayName("setters pour informations de ligne devraient fonctionner")
    void lineInfoSetters_shouldWork() {
        // When
        segment.setLineCode("M1");
        segment.setLineName("Métro 1");
        segment.setLineColor("#FFBE00");
        segment.setNetworkName("RATP");

        // Then
        assertEquals("M1", segment.getLineCode());
        assertEquals("Métro 1", segment.getLineName());
        assertEquals("#FFBE00", segment.getLineColor());
        assertEquals("RATP", segment.getNetworkName());
    }

    @Test
    @DisplayName("setters pour horaires devraient fonctionner")
    void scheduleSetters_shouldWork() {
        // Given
        OffsetDateTime departure = OffsetDateTime.now();
        OffsetDateTime arrival = departure.plusMinutes(30);

        // When
        segment.setScheduledDeparture(departure);
        segment.setScheduledArrival(arrival);
        segment.setDurationSeconds(1800);

        // Then
        assertEquals(departure, segment.getScheduledDeparture());
        assertEquals(arrival, segment.getScheduledArrival());
        assertEquals(1800, segment.getDurationSeconds());
    }

    @Test
    @DisplayName("setters pour mode de transport devraient fonctionner")
    void transitModeSetters_shouldWork() {
        // When
        segment.setTransitMode(TransitMode.METRO);
        segment.setPrimSectionId("section-123");

        // Then
        assertEquals(TransitMode.METRO, segment.getTransitMode());
        assertEquals("section-123", segment.getPrimSectionId());
    }

    @Test
    @DisplayName("setters pour distance et climatisation devraient fonctionner")
    void additionalSetters_shouldWork() {
        // When
        segment.setDistanceMeters(5000);
        segment.setHasAirConditioning(true);

        // Then
        assertEquals(5000, segment.getDistanceMeters());
        assertTrue(segment.getHasAirConditioning());
    }

    @Test
    @DisplayName("La liste de points devrait être non modifiable")
    void points_shouldBeUnmodifiable() {
        JourneyPoint point = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Origin");
        segment.addPoint(point);

        assertThrows(UnsupportedOperationException.class, () -> segment.getPoints().add(null));
    }
}
