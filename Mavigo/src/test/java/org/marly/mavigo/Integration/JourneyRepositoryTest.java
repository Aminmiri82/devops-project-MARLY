package org.marly.mavigo.Integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - JourneyRepository")
class JourneyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Sauvegarder et récupérer un journey doit fonctionner")
    void testSaveAndFindJourney() {
        // Given
        User user = new User("ext-test", "test@example.com", "Test User");
        user = userRepository.save(user);

        Journey journey = new Journey(
                user,
                "Paris Gare du Nord",
                "Lyon Part-Dieu",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2)
        );
        journey.setStatus(JourneyStatus.PLANNED);

        // When
        Journey savedJourney = journeyRepository.save(journey);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Journey> found = journeyRepository.findById(savedJourney.getId());
        assertTrue(found.isPresent(), "Le trajet devrait être trouvé");
        assertEquals("Paris Gare du Nord", found.get().getOriginLabel());
        assertEquals("Lyon Part-Dieu", found.get().getDestinationLabel());
    }

    @Test
    @DisplayName("findWithLegsById doit retourner le journey avec ses legs")
    void testFindWithLegsById() {
        // Given
        User user = new User("ext-test2", "test2@example.com", "Test User 2");
        user = userRepository.save(user);

        Journey journey = new Journey(
                user,
                "Marseille",
                "Nice",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(3)
        );
        journey.setStatus(JourneyStatus.PLANNED);
        journey = journeyRepository.save(journey);
        entityManager.flush();

        // When
        Optional<Journey> found = journeyRepository.findWithSegmentsById(journey.getId());

        // Then
        assertTrue(found.isPresent());
        assertNotNull(found.get().getSegments(), "Les segments ne devraient pas être null");
    }

    @Test
    @DisplayName("Sauvegarder un journey avec comfort mode doit le persister")
    void testSaveJourney_WithComfortMode_ShouldPersist() {
        // Given
        User user = new User("ext-test3", "test3@example.com", "Test User 3");
        user = userRepository.save(user);

        Journey journey = new Journey(
                user,
                "Bordeaux",
                "Toulouse",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1)
        );
        journey.setComfortModeEnabled(true);
        journey.setStatus(JourneyStatus.PLANNED);

        // When
        Journey savedJourney = journeyRepository.save(journey);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Journey> found = journeyRepository.findById(savedJourney.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().isComfortModeEnabled(),
                "Le mode confort devrait être persisté");
    }

    @Test
    @DisplayName("Mettre à jour le statut d'un journey doit fonctionner")
    void testUpdateJourneyStatus() {
        // Given
        User user = new User("ext-test5", "test5@example.com", "Test User 5");
        user = userRepository.save(user);

        Journey journey = new Journey(
                user,
                "Nantes",
                "Rennes",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1)
        );
        journey.setStatus(JourneyStatus.PLANNED);
        journey = journeyRepository.save(journey);
        entityManager.flush();

        // When
        journey.setStatus(JourneyStatus.IN_PROGRESS);
        journey.setActualDeparture(OffsetDateTime.now());
        journeyRepository.save(journey);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Journey> found = journeyRepository.findById(journey.getId());
        assertTrue(found.isPresent());
        assertEquals(JourneyStatus.IN_PROGRESS, found.get().getStatus());
        assertNotNull(found.get().getActualDeparture());
    }
}
