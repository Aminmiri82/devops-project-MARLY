package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.controller.dto.JourneyResponse;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.journey.JourneyPlanningServiceImpl;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerturbationControllerTest {

    @Mock
    private PerturbationService perturbationService;

    @Mock
    private JourneyPlanningServiceImpl journeyPlanningService;

    @Mock
    private JourneyRepository journeyRepository;

    private PerturbationController perturbationController;

    @BeforeEach
    void setUp() {
        perturbationController = new PerturbationController(
            perturbationService, 
            journeyPlanningService, 
            journeyRepository
        );
    }

    @Test
    void getPerturbations_ShouldReturnList() {
        // Given
        when(perturbationService.getDisruptions()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<Disruption>> response = perturbationController.getPerturbations();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void addPerturbation_ShouldReturnCreatedDisruption() {
        // Given
        String line = "M1";
        String creator = "Admin";
        Disruption disruption = new Disruption();
        disruption.setEffectedLine(line);
        when(perturbationService.addDisruption(line, creator)).thenReturn(disruption);

        // When
        ResponseEntity<Disruption> response = perturbationController.addPerturbation(line, creator);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(line, response.getBody().getEffectedLine());
    }

    @Test
    void softDeleteDisruption_ShouldReturnNoContent() {
        // Given
        Long id = 1L;

        // When
        ResponseEntity<Void> response = perturbationController.softDeleteDisruption(id);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(perturbationService).softDelete(id);
    }

    @Test
    void applyPerturbation_ShouldReturnUpdatedJourneys() {
        // Given
        UUID journeyId = UUID.randomUUID();
        String line = "M1";
        String creator = "User";
        
        Disruption disruption = new Disruption();
        when(perturbationService.addDisruption(line, creator)).thenReturn(disruption);
        
        List<Journey> journeys = new ArrayList<>();
        when(journeyPlanningService.updateJourneyWithDisruption(eq(journeyId), eq(disruption), any(), any(), any()))
            .thenReturn(journeys);

        // When
        ResponseEntity<List<JourneyResponse>> response = perturbationController.applyPerturbation(
            journeyId, line, creator, null, null, null
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
