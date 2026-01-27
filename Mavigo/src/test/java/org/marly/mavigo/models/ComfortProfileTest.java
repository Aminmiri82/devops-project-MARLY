package org.marly.mavigo.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.marly.mavigo.models.user.ComfortProfile;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires - ComfortProfile")
class ComfortProfileTest {

    private ComfortProfile comfortProfile;

    @BeforeEach
    void setUp() {
        comfortProfile = new ComfortProfile();
    }

    @Test
    @DisplayName("Un nouveau ComfortProfile doit avoir des valeurs par défaut")
    void testNewComfortProfile_ShouldHaveDefaultValues() {
        // Then
        assertNull(comfortProfile.getDirectPath());
        assertNull(comfortProfile.getRequireAirConditioning());
        assertNull(comfortProfile.getMaxNbTransfers());
        assertNull(comfortProfile.getMaxWaitingDuration());
        assertNull(comfortProfile.getMaxWalkingDuration());
        assertFalse(comfortProfile.isWheelchairAccessible());
    }

    @Test
    @DisplayName("Setter wheelchair accessible doit fonctionner")
    void testSetWheelchairAccessible_ShouldUpdateValue() {
        // When
        comfortProfile.setWheelchairAccessible(true);

        // Then
        assertTrue(comfortProfile.isWheelchairAccessible());
    }

    @Test
    @DisplayName("Setter air conditioning doit fonctionner")
    void testSetRequireAirConditioning_ShouldUpdateValue() {
        // When
        comfortProfile.setRequireAirConditioning(true);

        // Then
        assertTrue(comfortProfile.getRequireAirConditioning());
    }

    @Test
    @DisplayName("Setter max transfers doit fonctionner")
    void testSetMaxNbTransfers_ShouldUpdateValue() {
        // When
        comfortProfile.setMaxNbTransfers(3);

        // Then
        assertEquals(3, comfortProfile.getMaxNbTransfers());
    }

    @Test
    @DisplayName("hasSettings doit retourner false si aucun paramètre n'est défini")
    void testHasSettings_WithNoSettings_ShouldReturnFalse() {
        // When
        boolean result = comfortProfile.hasSettings();

        // Then
        assertFalse(result, "hasSettings devrait retourner false pour un profil vide");
    }

    @Test
    @DisplayName("hasSettings doit retourner true si wheelchair est activé")
    void testHasSettings_WithWheelchairEnabled_ShouldReturnTrue() {
        // When
        comfortProfile.setWheelchairAccessible(true);

        // Then
        assertTrue(comfortProfile.hasSettings(),
                "hasSettings devrait retourner true si wheelchair est activé");
    }

    @Test
    @DisplayName("hasSettings doit retourner true si air conditioning est défini")
    void testHasSettings_WithAirConditioningSet_ShouldReturnTrue() {
        // When
        comfortProfile.setRequireAirConditioning(true);

        // Then
        assertTrue(comfortProfile.hasSettings());
    }

    @Test
    @DisplayName("hasSettings doit retourner true si max transfers est défini")
    void testHasSettings_WithMaxTransfersSet_ShouldReturnTrue() {
        // When
        comfortProfile.setMaxNbTransfers(2);

        // Then
        assertTrue(comfortProfile.hasSettings());
    }

    @Test
    @DisplayName("hasSettings doit retourner true si max waiting duration est défini")
    void testHasSettings_WithMaxWaitingDurationSet_ShouldReturnTrue() {
        // When
        comfortProfile.setMaxWaitingDuration(15);

        // Then
        assertTrue(comfortProfile.hasSettings());
    }

    @Test
    @DisplayName("hasSettings doit retourner true si max walking duration est défini")
    void testHasSettings_WithMaxWalkingDurationSet_ShouldReturnTrue() {
        // When
        comfortProfile.setMaxWalkingDuration(20);

        // Then
        assertTrue(comfortProfile.hasSettings());
    }

    @Test
    @DisplayName("hasSettings doit retourner true si direct path est défini")
    void testHasSettings_WithDirectPathSet_ShouldReturnTrue() {
        // When
        comfortProfile.setDirectPath("some_path");

        // Then
        assertTrue(comfortProfile.hasSettings());
    }

    @Test
    @DisplayName("ComfortProfile complet doit avoir tous les paramètres")
    void testCompleteComfortProfile_ShouldHaveAllSettings() {
        // When
        comfortProfile.setWheelchairAccessible(true);
        comfortProfile.setRequireAirConditioning(true);
        comfortProfile.setMaxNbTransfers(2);
        comfortProfile.setMaxWaitingDuration(10);
        comfortProfile.setMaxWalkingDuration(15);
        comfortProfile.setDirectPath("direct");

        // Then
        assertTrue(comfortProfile.isWheelchairAccessible());
        assertTrue(comfortProfile.getRequireAirConditioning());
        assertEquals(2, comfortProfile.getMaxNbTransfers());
        assertEquals(10, comfortProfile.getMaxWaitingDuration());
        assertEquals(15, comfortProfile.getMaxWalkingDuration());
        assertEquals("direct", comfortProfile.getDirectPath());
        assertTrue(comfortProfile.hasSettings());
    }
}
