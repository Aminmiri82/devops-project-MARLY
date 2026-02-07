package org.marly.mavigo.service.perturbation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.repository.DisruptionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerturbationServiceTest {

    @Mock
    private DisruptionRepository disruptionRepository;

    @InjectMocks
    private PerturbationService perturbationService;

    @Test
    void getDisruptions_ShouldReturnValidDisruptions() {
        // Given
        Disruption disruption = new Disruption();
        when(disruptionRepository.findByValidUntilAfter(any(LocalDateTime.class)))
                .thenReturn(List.of(disruption));

        // When
        List<Disruption> result = perturbationService.getDisruptions();

        // Then
        assertEquals(1, result.size());
        verify(disruptionRepository).findByValidUntilAfter(any(LocalDateTime.class));
    }

    @Test
    void addDisruption_ShouldSaveDisruption() {
        // Given
        String line = "M1";
        String creator = "Admin";
        Disruption savedDisruption = new Disruption();
        savedDisruption.setEffectedLine(line);
        when(disruptionRepository.save(any(Disruption.class))).thenReturn(savedDisruption);

        // When
        Disruption result = perturbationService.addDisruption(line, creator);

        // Then
        assertNotNull(result);
        assertEquals(line, result.getEffectedLine());
        verify(disruptionRepository).save(any(Disruption.class));
    }

    @Test
    void softDelete_ShouldSetValidUntilToNow() {
        // Given
        Long id = 1L;
        Disruption disruption = new Disruption();
        disruption.setValidUntil(LocalDateTime.now().plusHours(1));
        
        when(disruptionRepository.findById(id)).thenReturn(Optional.of(disruption));

        // When
        perturbationService.softDelete(id);

        // Then
        verify(disruptionRepository).save(disruption);
        assertTrue(disruption.getValidUntil().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void softDelete_ShouldDoNothing_WhenNotFound() {
        // Given
        Long id = 1L;
        when(disruptionRepository.findById(id)).thenReturn(Optional.empty());

        // When
        perturbationService.softDelete(id);

        // Then
        verify(disruptionRepository, never()).save(any());
    }
}
