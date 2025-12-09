package org.marly.mavigo.controller.perturbation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimDisruption;
import org.marly.mavigo.controller.PerturbationController;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerturbationControllerTest {

    @Mock
    private PerturbationService perturbationService;

    @InjectMocks
    private PerturbationController perturbationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(perturbationController).build();
    }

    @Test
    void testGetPerturbations() throws Exception {

        PrimDisruption disruption = new PrimDisruption(
                "1",
                "Ligne 1",
                "Travaux sur la ligne",
                "Moyenne",
                LocalDateTime.now()
        );


        when(perturbationService.getPerturbations()).thenReturn(List.of(disruption));

        mockMvc.perform(get("/perturbations"))
                .andExpect(status().isOk());
    }
}