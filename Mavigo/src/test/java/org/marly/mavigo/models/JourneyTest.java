package org.marly.mavigo.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneyPointType;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.poi.PointOfInterest;
import org.marly.mavigo.models.poi.PointOfInterestCategory;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;

@DisplayName("Tests unitaires - Journey")
class JourneyTest {

    private User testUser;
    private Journey journey;

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
    }

    @Test
    @DisplayName("Un nouveau Journey devrait avoir le statut PLANNED par défaut")
    void newJourney_shouldHavePlannedStatusByDefault() {
        assertEquals(JourneyStatus.PLANNED, journey.getStatus());
    }

    @Test
    @DisplayName("Un nouveau Journey devrait avoir les valeurs de base initialisées")
    void newJourney_shouldHaveBasicValuesInitialized() {
        assertEquals("Gare de Lyon", journey.getOriginLabel());
        assertEquals("Châtelet", journey.getDestinationLabel());
        assertNotNull(journey.getPlannedDeparture());
        assertNotNull(journey.getPlannedArrival());
        assertEquals(testUser, journey.getUser());
    }

    @Test
    @DisplayName("addSegment devrait ajouter un segment et établir la relation bidirectionnelle")
    void addSegment_shouldAddSegmentAndSetBidirectionalRelation() {
        // Given
        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");

        // When
        journey.addSegment(segment);

        // Then
        assertEquals(1, journey.getSegments().size());
        assertEquals(journey, segment.getJourney());
    }

    @Test
    @DisplayName("replaceSegments devrait remplacer tous les segments")
    void replaceSegments_shouldReplaceAllSegments() {
        // Given
        JourneySegment oldSegment = new JourneySegment(journey, 1, SegmentType.WALKING);
        journey.addSegment(oldSegment);

        JourneySegment newSegment1 = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        JourneySegment newSegment2 = new JourneySegment(journey, 2, SegmentType.WALKING);

        // When
        journey.replaceSegments(List.of(newSegment1, newSegment2));

        // Then
        assertEquals(2, journey.getSegments().size());
    }

    @Test
    @DisplayName("getAllLineCodes devrait retourner tous les codes de lignes")
    void getAllLineCodes_shouldReturnAllLineCodes() {
        // Given
        JourneySegment segment1 = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment1.setLineCode("M1");
        JourneySegment segment2 = new JourneySegment(journey, 2, SegmentType.PUBLIC_TRANSPORT);
        segment2.setLineCode("RER-A");
        JourneySegment segment3 = new JourneySegment(journey, 3, SegmentType.WALKING);
        // No line code for walking

        journey.addSegment(segment1);
        journey.addSegment(segment2);
        journey.addSegment(segment3);

        // When
        Set<String> lineCodes = journey.getAllLineCodes();

        // Then
        assertEquals(2, lineCodes.size());
        assertTrue(lineCodes.contains("M1"));
        assertTrue(lineCodes.contains("RER-A"));
    }

    @Test
    @DisplayName("isLineUsed devrait retourner true si la ligne est utilisée")
    void isLineUsed_shouldReturnTrueWhenLineIsUsed() {
        // Given
        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");
        journey.addSegment(segment);

        // When/Then
        assertTrue(journey.isLineUsed("M1"));
        assertFalse(journey.isLineUsed("M4"));
    }

    @Test
    @DisplayName("addDisruption devrait ajouter une perturbation")
    void addDisruption_shouldAddDisruption() {
        // Given
        Disruption disruption = Disruption.lineDisruption(journey, "M1", testUser);

        // When
        journey.addDisruption(disruption);

        // Then
        assertEquals(1, journey.getDisruptions().size());
        assertEquals("M1", journey.getDisruptions().get(0).getAffectedLineCode());
    }

    @Test
    @DisplayName("getPublicTransportSegments devrait retourner uniquement les segments de transport public")
    void getPublicTransportSegments_shouldReturnOnlyPublicTransportSegments() {
        // Given
        JourneySegment walkingSegment = new JourneySegment(journey, 1, SegmentType.WALKING);
        JourneySegment transitSegment = new JourneySegment(journey, 2, SegmentType.PUBLIC_TRANSPORT);
        JourneySegment anotherWalking = new JourneySegment(journey, 3, SegmentType.WALKING);

        journey.addSegment(walkingSegment);
        journey.addSegment(transitSegment);
        journey.addSegment(anotherWalking);

        // When
        List<JourneySegment> publicTransportSegments = journey.getPublicTransportSegments();

        // Then
        assertEquals(1, publicTransportSegments.size());
        assertEquals(SegmentType.PUBLIC_TRANSPORT, publicTransportSegments.get(0).getSegmentType());
    }

    @Test
    @DisplayName("setters et getters devraient fonctionner correctement")
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        GeoPoint origin = new GeoPoint(48.8443, 2.3730);
        GeoPoint destination = new GeoPoint(48.8584, 2.3470);
        OffsetDateTime actualDeparture = OffsetDateTime.now();
        OffsetDateTime actualArrival = OffsetDateTime.now().plusMinutes(45);

        // When
        journey.setOriginCoordinate(origin);
        journey.setDestinationCoordinate(destination);
        journey.setActualDeparture(actualDeparture);
        journey.setActualArrival(actualArrival);
        journey.setStatus(JourneyStatus.IN_PROGRESS);
        journey.setComfortModeEnabled(true);
        journey.setPrimItineraryId("prim-123");

        // Then
        assertEquals(origin, journey.getOriginCoordinate());
        assertEquals(destination, journey.getDestinationCoordinate());
        assertEquals(actualDeparture, journey.getActualDeparture());
        assertEquals(actualArrival, journey.getActualArrival());
        assertEquals(JourneyStatus.IN_PROGRESS, journey.getStatus());
        assertTrue(journey.isComfortModeEnabled());
        assertEquals("prim-123", journey.getPrimItineraryId());
    }

    @Test
    @DisplayName("addPointOfInterest devrait ajouter un POI")
    void addPointOfInterest_shouldAddPoi() {
        // Given
        PointOfInterest poi = new PointOfInterest("poi-123", "Louvre Museum", PointOfInterestCategory.MUSEUM);

        // When
        journey.addPointOfInterest(poi);

        // Then
        assertEquals(1, journey.getPointOfInterests().size());
    }

    @Test
    @DisplayName("recalculateDisruptionSummary devrait compter les points perturbés")
    void recalculateDisruptionSummary_shouldCountDisruptedPoints() {
        // Given
        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        journey.addSegment(segment);

        // Initially no disrupted points
        journey.recalculateDisruptionSummary();

        // Then
        assertEquals(0, journey.getDisruptionCount());
    }
}
