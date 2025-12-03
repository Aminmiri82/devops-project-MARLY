package org.marly.mavigo.scheduler;

import org.marly.mavigo.service.perturbation.PerturbationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PerturbationScheduler {

    private final PerturbationService perturbationService;

    public PerturbationScheduler(PerturbationService service) {
        this.perturbationService = service;
    }


    @Scheduled(fixedRate = 300000)
    public void updatePerturbations() {
        perturbationService.getPerturbations();
        System.out.println("Done perturbations mises à jour automatiquement.");
    }
}
