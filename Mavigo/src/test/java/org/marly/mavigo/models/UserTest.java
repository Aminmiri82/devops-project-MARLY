package org.marly.mavigo.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;

@DisplayName("Tests unitaires - User")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("ext-123", "test@example.com", "Test User");
    }

    @Test
    @DisplayName("Un nouveau User devrait avoir les valeurs de base initialisées")
    void newUser_shouldHaveBasicValuesInitialized() {
        assertEquals("ext-123", user.getExternalId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getDisplayName());
        assertNotNull(user.getComfortProfile());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Un nouveau User devrait avoir un ComfortProfile par défaut")
    void newUser_shouldHaveDefaultComfortProfile() {
        assertNotNull(user.getComfortProfile());
        assertFalse(user.getComfortProfile().isWheelchairAccessible());
    }

    @Test
    @DisplayName("setComfortProfile devrait remplacer le profil de confort")
    void setComfortProfile_shouldReplaceComfortProfile() {
        // Given
        ComfortProfile newProfile = new ComfortProfile();
        newProfile.setWheelchairAccessible(true);
        newProfile.setMaxNbTransfers(2);

        // When
        user.setComfortProfile(newProfile);

        // Then
        assertEquals(newProfile, user.getComfortProfile());
        assertTrue(user.getComfortProfile().isWheelchairAccessible());
        assertEquals(2, user.getComfortProfile().getMaxNbTransfers());
    }

    @Test
    @DisplayName("addJourney devrait ajouter un trajet et établir la relation bidirectionnelle")
    void addJourney_shouldAddJourneyAndSetBidirectionalRelation() {
        // Given
        Journey journey = new Journey(
                user,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));

        // When
        user.addJourney(journey);

        // Then
        assertEquals(1, user.getJourneys().size());
        assertEquals(user, journey.getUser());
    }

    @Test
    @DisplayName("removeJourney devrait retirer un trajet")
    void removeJourney_shouldRemoveJourney() {
        // Given
        Journey journey = new Journey(
                user,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        user.addJourney(journey);
        assertEquals(1, user.getJourneys().size());

        // When
        user.removeJourney(journey);

        // Then
        assertEquals(0, user.getJourneys().size());
    }

    @Test
    @DisplayName("addNamedComfortSetting devrait ajouter un paramètre de confort nommé")
    void addNamedComfortSetting_shouldAddNamedComfortSetting() {
        // Given
        ComfortProfile profile = new ComfortProfile();
        NamedComfortSetting setting = new NamedComfortSetting("Travail", profile, user);

        // When
        user.addNamedComfortSetting(setting);

        // Then
        assertEquals(1, user.getNamedComfortSettings().size());
        assertEquals(user, setting.getUser());
    }

    @Test
    @DisplayName("removeNamedComfortSetting devrait retirer un paramètre de confort")
    void removeNamedComfortSetting_shouldRemoveNamedComfortSetting() {
        // Given
        ComfortProfile profile = new ComfortProfile();
        NamedComfortSetting setting = new NamedComfortSetting("Travail", profile, user);
        user.addNamedComfortSetting(setting);
        assertEquals(1, user.getNamedComfortSettings().size());

        // When
        user.removeNamedComfortSetting(setting);

        // Then
        assertEquals(0, user.getNamedComfortSettings().size());
    }

    @Test
    @DisplayName("setters et getters pour Google Account devraient fonctionner")
    void googleAccountSettersAndGetters_shouldWork() {
        // Given
        String googleSubject = "google-12345";
        String googleEmail = "google@example.com";
        OffsetDateTime linkedAt = OffsetDateTime.now();

        // When
        user.setGoogleAccountSubject(googleSubject);
        user.setGoogleAccountEmail(googleEmail);
        user.setGoogleLinkedAt(linkedAt);

        // Then
        assertEquals(googleSubject, user.getGoogleAccountSubject());
        assertEquals(googleEmail, user.getGoogleAccountEmail());
        assertEquals(linkedAt, user.getGoogleLinkedAt());
    }

    @Test
    @DisplayName("setters et getters pour stations devraient fonctionner")
    void stationSettersAndGetters_shouldWork() {
        // Given
        String homeStation = "stop:home";
        String workStation = "stop:work";

        // When
        user.setHomeStationId(homeStation);
        user.setWorkStationId(workStation);

        // Then
        assertEquals(homeStation, user.getHomeStationId());
        assertEquals(workStation, user.getWorkStationId());
    }

    @Test
    @DisplayName("hasSeenComfortPrompt devrait retourner false par défaut")
    void hasSeenComfortPrompt_shouldReturnFalseByDefault() {
        assertFalse(user.getHasSeenComfortPrompt());
    }

    @Test
    @DisplayName("setHasSeenComfortPrompt devrait mettre à jour la valeur")
    void setHasSeenComfortPrompt_shouldUpdateValue() {
        // When
        user.setHasSeenComfortPrompt(true);

        // Then
        assertTrue(user.getHasSeenComfortPrompt());
    }

    @Test
    @DisplayName("setId devrait définir l'ID")
    void setId_shouldSetId() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        user.setId(id);

        // Then
        assertEquals(id, user.getId());
    }

    @Test
    @DisplayName("Les listes de journeys et settings devraient être non modifiables")
    void lists_shouldBeUnmodifiable() {
        // When/Then
        assertThrows(UnsupportedOperationException.class, () -> user.getJourneys().add(null));
        assertThrows(UnsupportedOperationException.class, () -> user.getNamedComfortSettings().add(null));
    }
}
