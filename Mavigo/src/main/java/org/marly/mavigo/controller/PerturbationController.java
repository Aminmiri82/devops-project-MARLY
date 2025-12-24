package org.marly.mavigo.controller;

import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.journey.JourneyPlanningServiceImpl;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/perturbations")
public class PerturbationController {

    private final PerturbationService perturbationService;
    private final JourneyPlanningServiceImpl journeyPlanningService;
    private final JourneyRepository journeyRepository;

    public PerturbationController(PerturbationService perturbationService,
                                  JourneyPlanningServiceImpl journeyPlanningService,
                                  JourneyRepository journeyRepository) {
        this.perturbationService = perturbationService;
        this.journeyPlanningService = journeyPlanningService;
        this.journeyRepository = journeyRepository;
    }

    @GetMapping
    public ResponseEntity<List<Disruption>> getPerturbations() {
        return ResponseEntity.ok(perturbationService.getDisruptions());
    }

    @PostMapping
    public ResponseEntity<Disruption> addPerturbation(
            @RequestParam String line,
            @RequestParam String creator
    ) {
        return ResponseEntity.ok(perturbationService.addDisruption(line, creator));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteDisruption(@PathVariable Long id) {
        perturbationService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint pour appliquer une perturbation à un trajet existant.
     * Recalcule le trajet si la ligne est impactée.
     */
    @PostMapping("/apply")
    public ResponseEntity<List<org.marly.mavigo.controller.dto.JourneyResponse>> applyPerturbation(
            @RequestParam UUID journeyId,
            @RequestParam(required = false) String line,
            @RequestParam String creator,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng,
            @RequestParam(required = false) String newOrigin
    ) {
        // Créer la perturbation (generic if line is null)
        String lineCode = line != null ? line : "General Disruption";
        Disruption disruption = perturbationService.addDisruption(lineCode, creator);

        // Recalculer le trajet si nécessaire (retourne une liste d'options)
        // Pass ID directly so service can handle transaction and lazy loading
        List<Journey> updatedJourneys = journeyPlanningService.updateJourneyWithDisruption(journeyId, disruption, userLat, userLng, newOrigin);

        List<org.marly.mavigo.controller.dto.JourneyResponse> response = updatedJourneys.stream()
                .map(org.marly.mavigo.controller.dto.JourneyResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}
