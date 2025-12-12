package org.marly.mavigo.service.perturbation;

import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.repository.DisruptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerturbationService {

    private final DisruptionRepository repository;

    public PerturbationService(DisruptionRepository repository) {
        this.repository = repository;
    }

    public List<Disruption> getDisruptions() {
        return repository.findByValidUntilAfter(LocalDateTime.now());
    }

    public Disruption addDisruption(String line, String creator) {
        LocalDateTime now = LocalDateTime.now();
        Disruption disruption = new Disruption(
                line,
                now,
                now.plusHours(1), // default 1h
                creator
        );
        return repository.save(disruption);
    }

    // Soft delete: on définit validUntil = maintenant
    public void softDelete(Long id) {
        repository.findById(id).ifPresent(disruption -> {
            disruption.setValidUntil(LocalDateTime.now());
            repository.save(disruption);
        });
    }
}
