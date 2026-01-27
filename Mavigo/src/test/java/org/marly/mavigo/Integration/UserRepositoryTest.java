package org.marly.mavigo.Integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - UserRepository")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Sauvegarder et récupérer un utilisateur doit fonctionner")
    void testSaveAndFindUser() {
        // Given
        User user = new User("ext-123", "user@test.com", "Test User");
        ComfortProfile profile = new ComfortProfile();
        profile.setWheelchairAccessible(true);
        profile.setMaxNbTransfers(2);
        user.setComfortProfile(profile);

        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> found = userRepository.findById(savedUser.getId());
        assertTrue(found.isPresent(), "L'utilisateur devrait être trouvé");
        assertEquals("user@test.com", found.get().getEmail());
        assertTrue(found.get().getComfortProfile().isWheelchairAccessible(),
                "Le profil wheelchair devrait être persisté");
        assertEquals(2, found.get().getComfortProfile().getMaxNbTransfers());
    }

    @Test
    @DisplayName("Sauvegarder un utilisateur avec ComfortProfile complet")
    void testSaveUser_WithCompleteComfortProfile() {
        // Given
        User user = new User("ext-456", "complete@test.com", "Complete User");
        ComfortProfile profile = new ComfortProfile();
        profile.setWheelchairAccessible(true);
        profile.setRequireAirConditioning(true);
        profile.setMaxNbTransfers(3);
        profile.setMaxWaitingDuration(10);
        profile.setMaxWalkingDuration(15);
        profile.setDirectPath("direct");
        user.setComfortProfile(profile);

        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> found = userRepository.findById(savedUser.getId());
        assertTrue(found.isPresent());
        ComfortProfile foundProfile = found.get().getComfortProfile();
        assertTrue(foundProfile.isWheelchairAccessible());
        assertTrue(foundProfile.getRequireAirConditioning());
        assertEquals(3, foundProfile.getMaxNbTransfers());
        assertEquals(10, foundProfile.getMaxWaitingDuration());
        assertEquals(15, foundProfile.getMaxWalkingDuration());
        assertEquals("direct", foundProfile.getDirectPath());
    }

    @Test
    @DisplayName("Mettre à jour le ComfortProfile d'un utilisateur")
    void testUpdateUserComfortProfile() {
        // Given
        User user = new User("ext-789", "update@test.com", "Update User");
        user = userRepository.save(user);
        entityManager.flush();

        // When
        ComfortProfile profile = user.getComfortProfile();
        profile.setWheelchairAccessible(true);
        user.setComfortProfile(profile);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> found = userRepository.findById(user.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().getComfortProfile().isWheelchairAccessible());
    }

    @Test
    @DisplayName("findByEmail doit retourner l'utilisateur correct")
    void testFindByEmail() {
        // Given
        User user = new User("ext-abc", "unique@test.com", "Unique User");
        userRepository.save(user);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("unique@test.com");

        // Then
        assertTrue(found.isPresent(), "L'utilisateur devrait être trouvé par email");
        assertEquals("Unique User", found.get().getDisplayName());
        assertEquals("ext-abc", found.get().getExternalId());
    }
}
