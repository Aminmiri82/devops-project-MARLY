package org.marly.mavigo.controller.perturbation;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PerturbationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerturbationService service;

    @Test
    public void testGetPerturbations() throws Exception {
        // Ajouter une perturbation pour tester la récupération
        service.addDisruption("10", "TestUser");

        mockMvc.perform(get("/perturbations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    public void testAddPerturbation() throws Exception {
        mockMvc.perform(post("/perturbations")
                        .param("line", "20")
                        .param("creator", "Alice")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Vérifier que la perturbation a été ajoutée
        mockMvc.perform(get("/perturbations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.effectedLine=='20')]").exists());
    }
}