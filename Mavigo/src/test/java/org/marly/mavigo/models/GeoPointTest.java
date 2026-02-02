package org.marly.mavigo.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;

@DisplayName("Tests unitaires - GeoPoint")
class GeoPointTest {

    @Test
    @DisplayName("Un nouveau GeoPoint devrait stocker les coordonnées correctement")
    void newGeoPoint_shouldStoreCoordinatesCorrectly() {
        // Given
        Double latitude = 48.8443;
        Double longitude = 2.3730;

        // When
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        // Then
        assertEquals(latitude, geoPoint.getLatitude());
        assertEquals(longitude, geoPoint.getLongitude());
    }

    @Test
    @DisplayName("isComplete devrait retourner true si les deux coordonnées sont présentes")
    void isComplete_shouldReturnTrueWhenBothCoordinatesPresent() {
        // Given
        GeoPoint geoPoint = new GeoPoint(48.8443, 2.3730);

        // When/Then
        assertTrue(geoPoint.isComplete());
    }

    @Test
    @DisplayName("isComplete devrait retourner false si latitude est nulle")
    void isComplete_shouldReturnFalseWhenLatitudeNull() {
        // Given
        GeoPoint geoPoint = new GeoPoint(null, 2.3730);

        // When/Then
        assertFalse(geoPoint.isComplete());
    }

    @Test
    @DisplayName("isComplete devrait retourner false si longitude est nulle")
    void isComplete_shouldReturnFalseWhenLongitudeNull() {
        // Given
        GeoPoint geoPoint = new GeoPoint(48.8443, null);

        // When/Then
        assertFalse(geoPoint.isComplete());
    }

    @Test
    @DisplayName("isComplete devrait retourner false si les deux coordonnées sont nulles")
    void isComplete_shouldReturnFalseWhenBothCoordinatesNull() {
        // Given
        GeoPoint geoPoint = new GeoPoint(null, null);

        // When/Then
        assertFalse(geoPoint.isComplete());
    }

    @Test
    @DisplayName("setLatitude devrait modifier la latitude")
    void setLatitude_shouldModifyLatitude() {
        // Given
        GeoPoint geoPoint = new GeoPoint(48.8443, 2.3730);

        // When
        geoPoint.setLatitude(48.8584);

        // Then
        assertEquals(48.8584, geoPoint.getLatitude());
    }

    @Test
    @DisplayName("setLongitude devrait modifier la longitude")
    void setLongitude_shouldModifyLongitude() {
        // Given
        GeoPoint geoPoint = new GeoPoint(48.8443, 2.3730);

        // When
        geoPoint.setLongitude(2.3470);

        // Then
        assertEquals(2.3470, geoPoint.getLongitude());
    }

    @Test
    @DisplayName("GeoPoint devrait accepter des valeurs négatives")
    void geoPoint_shouldAcceptNegativeValues() {
        // Given - Coordinates for a point in the Southern and Western hemispheres
        Double latitude = -33.8688; // Sydney
        Double longitude = -151.2093;

        // When
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        // Then
        assertEquals(latitude, geoPoint.getLatitude());
        assertEquals(longitude, geoPoint.getLongitude());
        assertTrue(geoPoint.isComplete());
    }

    @Test
    @DisplayName("GeoPoint devrait accepter des valeurs aux limites")
    void geoPoint_shouldAcceptBoundaryValues() {
        // Given - Boundary coordinates
        GeoPoint northPole = new GeoPoint(90.0, 0.0);
        GeoPoint southPole = new GeoPoint(-90.0, 0.0);
        GeoPoint dateLine = new GeoPoint(0.0, 180.0);
        GeoPoint negativeDateLine = new GeoPoint(0.0, -180.0);

        // When/Then
        assertTrue(northPole.isComplete());
        assertTrue(southPole.isComplete());
        assertTrue(dateLine.isComplete());
        assertTrue(negativeDateLine.isComplete());
    }

    @Test
    @DisplayName("GeoPoint devrait accepter des valeurs à zéro")
    void geoPoint_shouldAcceptZeroValues() {
        // Given - Coordinates at origin (0, 0)
        GeoPoint origin = new GeoPoint(0.0, 0.0);

        // When/Then
        assertEquals(0.0, origin.getLatitude());
        assertEquals(0.0, origin.getLongitude());
        assertTrue(origin.isComplete());
    }
}
