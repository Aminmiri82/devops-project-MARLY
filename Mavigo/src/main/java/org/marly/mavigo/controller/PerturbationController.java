package org.marly.mavigo.controller;

import org.marly.mavigo.client.prim.PrimDisruption;
import org.marly.mavigo.service.PerturbationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PerturbationController {

    private final PerturbationService perturbationService;

    public PerturbationController(PerturbationService service) {
        this.perturbationService = service;
    }

    @GetMapping("/perturbations")
    public List<PrimDisruption> getPerturbations() {
        return perturbationService.getPerturbations();
    }
}
