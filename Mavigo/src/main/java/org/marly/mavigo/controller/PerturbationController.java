package org.marly.mavigo.controller;

import java.util.List;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perturbations")
public class PerturbationController {

    private final PerturbationService service;

    public PerturbationController(PerturbationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Disruption>> getPerturbations() {
        return ResponseEntity.ok(service.getDisruptions());
    }

    @PostMapping
    public ResponseEntity<Disruption> addPerturbation(
            @RequestParam String line,
            @RequestParam String creator
    ) {
        return ResponseEntity.ok(service.addDisruption(line, creator));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteDisruption(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
