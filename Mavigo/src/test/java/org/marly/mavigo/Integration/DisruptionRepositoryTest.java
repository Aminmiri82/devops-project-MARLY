package org.marly.mavigo.Integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.disruption.DisruptionType;
import org.marly.mavigo.repository.DisruptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - DisruptionRepository")
class DisruptionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DisruptionRepository disruptionRepository;

    @Test
    @DisplayName("Sauvegarder et récupérer une disruption devrait fonctionner")
    void testSaveAndFindDisruption() {
        // Given
        Disruption disruption = new Disruption();
        disruption.setDisruptionType(DisruptionType.LINE);
        disruption.setAffectedLineCode("M1");
        disruption.setEffectedLine("M1");
        disruption.setCreatedAt(LocalDateTime.now());
        disruption.setValidUntil(LocalDateTime.now().plusHours(2));

        // When
        Disruption saved = disruptionRepository.save(disruption);
        entityManager.flush();
        entityManager.clear();

        // Then
        Disruption found = disruptionRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(DisruptionType.LINE, found.getDisruptionType());
        assertEquals("M1", found.getAffectedLineCode());
    }

    @Test
    @DisplayName("findByValidUntilAfter devrait retourner les disruptions actives")
    void testFindByValidUntilAfter() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        Disruption activeDisruption = new Disruption();
        activeDisruption.setDisruptionType(DisruptionType.LINE);
        activeDisruption.setAffectedLineCode("M1");
        activeDisruption.setCreatedAt(now);
        activeDisruption.setValidUntil(now.plusHours(2)); // Still valid

        Disruption expiredDisruption = new Disruption();
        expiredDisruption.setDisruptionType(DisruptionType.STATION);
        expiredDisruption.setAffectedStopAreaId("stop:123");
        expiredDisruption.setCreatedAt(now.minusHours(3));
        expiredDisruption.setValidUntil(now.minusHours(1)); // Expired

        disruptionRepository.save(activeDisruption);
        disruptionRepository.save(expiredDisruption);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Disruption> activeDisruptions = disruptionRepository.findByValidUntilAfter(now);

        // Then
        assertEquals(1, activeDisruptions.size());
        assertEquals("M1", activeDisruptions.get(0).getAffectedLineCode());
    }

    @Test
    @DisplayName("Sauvegarder une disruption de station devrait fonctionner")
    void testSaveStationDisruption() {
        // Given
        Disruption disruption = new Disruption();
        disruption.setDisruptionType(DisruptionType.STATION);
        disruption.setAffectedStopAreaId("stop:nation:123");
        disruption.setCreatedAt(LocalDateTime.now());
        disruption.setValidUntil(LocalDateTime.now().plusHours(1));

        // When
        Disruption saved = disruptionRepository.save(disruption);
        entityManager.flush();
        entityManager.clear();

        // Then
        Disruption found = disruptionRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(DisruptionType.STATION, found.getDisruptionType());
        assertEquals("stop:nation:123", found.getAffectedStopAreaId());
        assertTrue(found.isStationDisruption());
        assertFalse(found.isLineDisruption());
    }

    @Test
    @DisplayName("Sauvegarder une disruption avec créateur devrait fonctionner")
    void testSaveDisruptionWithCreator() {
        // Given
        Disruption disruption = new Disruption();
        disruption.setDisruptionType(DisruptionType.LINE);
        disruption.setAffectedLineCode("RER-A");
        disruption.setCreatedAt(LocalDateTime.now());
        disruption.setCreator("SYSTEM");

        // When
        Disruption saved = disruptionRepository.save(disruption);
        entityManager.flush();
        entityManager.clear();

        // Then
        Disruption found = disruptionRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("SYSTEM", found.getCreator());
    }

    @Test
    @DisplayName("findAll devrait retourner toutes les disruptions")
    void testFindAll() {
        // Given
        Disruption disruption1 = new Disruption();
        disruption1.setDisruptionType(DisruptionType.LINE);
        disruption1.setAffectedLineCode("M1");
        disruption1.setCreatedAt(LocalDateTime.now());

        Disruption disruption2 = new Disruption();
        disruption2.setDisruptionType(DisruptionType.LINE);
        disruption2.setAffectedLineCode("M4");
        disruption2.setCreatedAt(LocalDateTime.now());

        disruptionRepository.save(disruption1);
        disruptionRepository.save(disruption2);
        entityManager.flush();

        // When
        List<Disruption> allDisruptions = disruptionRepository.findAll();

        // Then
        assertEquals(2, allDisruptions.size());
    }
}
