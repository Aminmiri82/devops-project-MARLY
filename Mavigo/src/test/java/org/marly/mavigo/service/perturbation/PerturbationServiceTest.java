package org.marly.mavigo.service.perturbation;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.disruption.Disruption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PerturbationServiceTest {

    @Autowired
    private PerturbationService service;

    @Test
    public void testAddAndGetDisruption() {
        service.addDisruption("10", "Thomas");
        List<Disruption> disruptions = service.getDisruptions();
        assertFalse(disruptions.isEmpty(), "La liste de perturbations ne doit pas être vide");
    }

    @Test
    public void testSoftDelete() {
        Disruption disruption = service.addDisruption("20", "Alice");
        service.softDelete(disruption.getId());
        List<Disruption> disruptions = service.getDisruptions();
        assertTrue(disruptions.stream()
                .noneMatch(d -> d.getId().equals(disruption.getId())), "La perturbation doit être soft deleted");
    }
}