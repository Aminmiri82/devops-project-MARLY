package org.marly.mavigo.Integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - StopAreaRepository")
class StopAreaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StopAreaRepository stopAreaRepository;

    @Test
    @DisplayName("Sauvegarder et récupérer une zone d'arrêt devrait fonctionner")
    void testSaveAndFindStopArea() {
        // Given
        StopArea stopArea = new StopArea("stop:123", "Gare de Lyon", new GeoPoint(48.8443, 2.3730));

        // When
        StopArea saved = stopAreaRepository.save(stopArea);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<StopArea> found = stopAreaRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Gare de Lyon", found.get().getName());
        assertEquals("stop:123", found.get().getExternalId());
    }

    @Test
    @DisplayName("findByExternalId devrait trouver une zone par son ID externe")
    void testFindByExternalId() {
        // Given
        StopArea stopArea = new StopArea("stop:chatelet", "Châtelet", new GeoPoint(48.8584, 2.3470));
        stopAreaRepository.save(stopArea);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<StopArea> found = stopAreaRepository.findByExternalId("stop:chatelet");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Châtelet", found.get().getName());
    }

    @Test
    @DisplayName("findByExternalId devrait retourner vide si non trouvé")
    void testFindByExternalId_NotFound() {
        // When
        Optional<StopArea> found = stopAreaRepository.findByExternalId("non-existent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("findByNameIgnoreCase devrait trouver par nom (insensible à la casse)")
    void testFindByNameIgnoreCase() {
        // Given
        StopArea stopArea = new StopArea("stop:nation", "Nation", new GeoPoint(48.8483, 2.3952));
        stopAreaRepository.save(stopArea);
        entityManager.flush();
        entityManager.clear();

        // When
        List<StopArea> found = stopAreaRepository.findByNameIgnoreCase("NATION");

        // Then
        assertEquals(1, found.size());
        assertEquals("Nation", found.get(0).getName());
    }

    @Test
    @DisplayName("findByNameIgnoreCase devrait trouver avec casse différente")
    void testFindByNameIgnoreCase_DifferentCase() {
        // Given
        StopArea stopArea = new StopArea("stop:gare", "Gare du Nord", new GeoPoint(48.8809, 2.3553));
        stopAreaRepository.save(stopArea);
        entityManager.flush();
        entityManager.clear();

        // When
        List<StopArea> lowerCase = stopAreaRepository.findByNameIgnoreCase("gare du nord");
        List<StopArea> upperCase = stopAreaRepository.findByNameIgnoreCase("GARE DU NORD");
        List<StopArea> mixedCase = stopAreaRepository.findByNameIgnoreCase("Gare Du Nord");

        // Then
        assertEquals(1, lowerCase.size());
        assertEquals(1, upperCase.size());
        assertEquals(1, mixedCase.size());
    }

    @Test
    @DisplayName("findFirstByNameIgnoreCase devrait retourner le premier résultat")
    void testFindFirstByNameIgnoreCase() {
        // Given
        StopArea stopArea = new StopArea("stop:opera", "Opéra", new GeoPoint(48.8706, 2.3319));
        stopAreaRepository.save(stopArea);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<StopArea> found = stopAreaRepository.findFirstByNameIgnoreCase("opéra");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Opéra", found.get().getName());
    }

    @Test
    @DisplayName("findFirstByNameIgnoreCase devrait retourner vide si non trouvé")
    void testFindFirstByNameIgnoreCase_NotFound() {
        // When
        Optional<StopArea> found = stopAreaRepository.findFirstByNameIgnoreCase("Non Existent Station");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Sauvegarder une zone sans coordonnées devrait fonctionner")
    void testSaveStopAreaWithoutCoordinates() {
        // Given
        StopArea stopArea = new StopArea("stop:no-coords", "Station Sans Coordonnées", null);

        // When
        StopArea saved = stopAreaRepository.save(stopArea);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<StopArea> found = stopAreaRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getCoordinates());
    }

    @Test
    @DisplayName("findAll devrait retourner toutes les zones d'arrêt")
    void testFindAll() {
        // Given
        stopAreaRepository.save(new StopArea("stop:1", "Station 1", new GeoPoint(48.0, 2.0)));
        stopAreaRepository.save(new StopArea("stop:2", "Station 2", new GeoPoint(48.1, 2.1)));
        stopAreaRepository.save(new StopArea("stop:3", "Station 3", new GeoPoint(48.2, 2.2)));
        entityManager.flush();

        // When
        List<StopArea> all = stopAreaRepository.findAll();

        // Then
        assertEquals(3, all.size());
    }
}
