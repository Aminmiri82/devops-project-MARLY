package org.marly.mavigo.service.perturbation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.marly.mavigo.client.prim.PrimDisruption;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class PerturbationServiceIntegrationTest {

    @Autowired
    private PerturbationService perturbationService;

    @Test
    void testGetPerturbationsWithRealApi() {
        List<PrimDisruption> disruptions = perturbationService.getPerturbations();


        assertFalse(disruptions.isEmpty(), "La liste de perturbations ne doit pas être vide");

        disruptions.forEach(System.out::println);
    }
}

