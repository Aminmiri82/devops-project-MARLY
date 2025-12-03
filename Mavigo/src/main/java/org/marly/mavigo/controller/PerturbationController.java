package org.marly.mavigo.controller;

import org.marly.mavigo.client.prim.PrimDisruption;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PerturbationController {

    private final PerturbationService perturbationService;

    @Autowired
    public PerturbationController(PerturbationService perturbationService) {
        this.perturbationService = perturbationService;
    }

    @GetMapping("/perturbations")
    public ResponseEntity<List<PrimDisruption>> getPerturbations() {
        try {
            List<PrimDisruption> disruptions = perturbationService.getPerturbations();
            return ResponseEntity.ok(disruptions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}