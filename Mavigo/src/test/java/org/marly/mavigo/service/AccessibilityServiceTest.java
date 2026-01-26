package org.marly.mavigo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.service.accessibility.AccessibilityService;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AccessibilityService")
class AccessibilityServiceTest {

    @InjectMocks
    private AccessibilityService accessibilityService;

    @Test
    @DisplayName("Vérifier qu'une station valide est accessible")
    void testIsStationWheelchairAccessible_WithValidStationId_ShouldReturnTrue() {
        // Given
        String stationId = "stop_point:12345";

        // When
        boolean result = accessibilityService.isStationWheelchairAccessible(stationId);

        // Then
        assertTrue(result, "Une station avec un ID valide devrait être accessible par défaut");
    }

    @Test
    @DisplayName("Vérifier qu'une station null n'est pas accessible")
    void testIsStationWheelchairAccessible_WithNullStationId_ShouldReturnFalse() {
        // Given
        String stationId = null;

        // When
        boolean result = accessibilityService.isStationWheelchairAccessible(stationId);

        // Then
        assertFalse(result, "Une station avec un ID null ne devrait pas être accessible");
    }

    @Test
    @DisplayName("Vérifier qu'une station vide n'est pas accessible")
    void testIsStationWheelchairAccessible_WithEmptyStationId_ShouldReturnFalse() {
        // Given
        String stationId = "";

        // When
        boolean result = accessibilityService.isStationWheelchairAccessible(stationId);

        // Then
        assertFalse(result, "Une station avec un ID vide ne devrait pas être accessible");
    }

    @Test
    @DisplayName("Vérifier qu'une ligne valide est accessible")
    void testIsLineWheelchairAccessible_WithValidLineCode_ShouldReturnTrue() {
        // Given
        String lineCode = "M1";

        // When
        boolean result = accessibilityService.isLineWheelchairAccessible(lineCode);

        // Then
        assertTrue(result, "Une ligne avec un code valide devrait être accessible par défaut");
    }

    @Test
    @DisplayName("Vérifier qu'une ligne null n'est pas accessible")
    void testIsLineWheelchairAccessible_WithNullLineCode_ShouldReturnFalse() {
        // Given
        String lineCode = null;

        // When
        boolean result = accessibilityService.isLineWheelchairAccessible(lineCode);

        // Then
        assertFalse(result, "Une ligne avec un code null ne devrait pas être accessible");
    }

    @Test
    @DisplayName("Vérifier qu'une ligne vide n'est pas accessible")
    void testIsLineWheelchairAccessible_WithEmptyLineCode_ShouldReturnFalse() {
        // Given
        String lineCode = "";

        // When
        boolean result = accessibilityService.isLineWheelchairAccessible(lineCode);

        // Then
        assertFalse(result, "Une ligne avec un code vide ne devrait pas être accessible");
    }

    @Test
    @DisplayName("Vérifier la gestion des caractères spéciaux dans l'ID de station")
    void testIsStationWheelchairAccessible_WithSpecialCharacters_ShouldHandle() {
        // Given
        String stationId = "stop_point:Paris-Gare_du_Nord";

        // When
        boolean result = accessibilityService.isStationWheelchairAccessible(stationId);

        // Then
        assertTrue(result, "Une station avec des caractères spéciaux devrait être gérée correctement");
    }

    @Test
    @DisplayName("Vérifier différents codes de ligne (Metro, RER, Tram)")
    void testIsLineWheelchairAccessible_WithDifferentLineCodes_ShouldHandle() {
        // Given
        String[] lineCodes = {"M1", "M14", "RER A", "T3", "Bus 91"};

        // When & Then
        for (String lineCode : lineCodes) {
            boolean result = accessibilityService.isLineWheelchairAccessible(lineCode);
            assertTrue(result, "La ligne " + lineCode + " devrait être accessible");
        }
    }
}
