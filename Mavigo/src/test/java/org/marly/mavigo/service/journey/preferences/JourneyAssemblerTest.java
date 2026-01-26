package org.marly.mavigo.service.journey.preferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.journey.JourneyAssembler;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires - JourneyAssembler")
class JourneyAssemblerTest {

    private JourneyAssembler journeyAssembler;
    private User testUser;
    private StopArea origin;
    private StopArea destination;
    private PrimJourneyPlanDto primPlan;

    @BeforeEach
    void setUp() {
        journeyAssembler = new JourneyAssembler();
        testUser = new User("ext-123", "test@example.com", "Test User");
        origin = new StopArea("stop:1", "Gare du Nord", null);
        destination = new StopArea("stop:2", "Tour Eiffel", null);

        // 👇 CORRECTION ICI
        primPlan = new PrimJourneyPlanDto(
                "journey-123",                      // journeyId
                OffsetDateTime.now(),               // departureDateTime
                OffsetDateTime.now().plusHours(1),  // arrivalDateTime
                3600,                               // durationSeconds
                0,            // sections (pas legs)
                Collections.emptyList()             // links (optionnel)
        );
    }

    @Test
    @DisplayName("Assembler avec preferences wheelchair doit marquer le journey accessible")
    void testAssemble_WithWheelchairPreference_ShouldSetWheelchairAccessible() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, true);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertTrue(journey.isWheelchairAccessible(),
                "Le trajet devrait être marqué comme accessible en fauteuil roulant");
    }

    @Test
    @DisplayName("Assembler sans wheelchair preference ne doit pas marquer le journey accessible")
    void testAssemble_WithoutWheelchairPreference_ShouldNotSetWheelchairAccessible() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertFalse(journey.isWheelchairAccessible(),
                "Le trajet ne devrait pas être marqué comme accessible");
    }

    @Test
    @DisplayName("Assembler avec comfort mode doit l'activer sur le journey")
    void testAssemble_WithComfortMode_ShouldEnableComfortMode() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(true, false, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertTrue(journey.isComfortModeEnabled(),
                "Le mode confort devrait être activé");
    }

    @Test
    @DisplayName("Assembler avec touristic mode doit l'activer sur le journey")
    void testAssemble_WithTouristicMode_ShouldEnableTouristicMode() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, true, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertTrue(journey.isTouristicModeEnabled(),
                "Le mode touristique devrait être activé");
    }

    @Test
    @DisplayName("Assembler avec toutes les preferences doit toutes les activer")
    void testAssemble_WithAllPreferences_ShouldEnableAllModes() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(true, true, true);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertTrue(journey.isComfortModeEnabled());
        assertTrue(journey.isTouristicModeEnabled());
        assertTrue(journey.isWheelchairAccessible());
    }

    @Test
    @DisplayName("Assembler sans preferences (null) ne doit activer aucun mode")
    void testAssemble_WithNullPreferences_ShouldNotEnableAnyMode() {
        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, null);

        // Then
        assertFalse(journey.isComfortModeEnabled());
        assertFalse(journey.isTouristicModeEnabled());
        assertFalse(journey.isWheelchairAccessible());
    }

    @Test
    @DisplayName("Assembler doit définir le statut PLANNED")
    void testAssemble_ShouldSetStatusPlanned() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertEquals(JourneyStatus.PLANNED, journey.getStatus());
    }

    @Test
    @DisplayName("Assembler doit définir les labels origin et destination")
    void testAssemble_ShouldSetOriginAndDestinationLabels() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertEquals("Gare du Nord", journey.getOriginLabel());
        assertEquals("Tour Eiffel", journey.getDestinationLabel());
    }

    @Test
    @DisplayName("Assembler doit définir l'utilisateur")
    void testAssemble_ShouldSetUser() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertNotNull(journey.getUser());
        assertEquals(testUser, journey.getUser());
    }

    @Test
    @DisplayName("Assembler doit définir le Prim itinerary ID")
    void testAssemble_ShouldSetPrimItineraryId() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When
        Journey journey = journeyAssembler.assemble(testUser, origin, destination, primPlan, preferences);

        // Then
        assertEquals("journey-123", journey.getPrimItineraryId());
    }

    @Test
    @DisplayName("Assembler avec user null doit lever une exception")
    void testAssemble_WithNullUser_ShouldThrowException() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            journeyAssembler.assemble(null, origin, destination, primPlan, preferences);
        });
    }

    @Test
    @DisplayName("Assembler avec origin null doit lever une exception")
    void testAssemble_WithNullOrigin_ShouldThrowException() {
        // Given
        JourneyPreferences preferences = new JourneyPreferences(false, false, false);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            journeyAssembler.assemble(testUser, null, destination, primPlan, preferences);
        });
    }
}