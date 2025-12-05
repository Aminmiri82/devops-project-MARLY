package org.marly.mavigo.controller;

import java.util.List;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.service.perturbation.PerturbationService;
import org.marly.mavigo.service.perturbation.dto.DisruptionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/disruptions")
public class PerturbationController {

    private final PerturbationService perturbationService;

    public PerturbationController(PerturbationService perturbationService) {
        this.perturbationService = perturbationService;
    }

    @PostMapping("/journey")
    public ResponseEntity<List<DisruptionDto>> getDisruptionsForJourney(@RequestBody PrimJourneyPlanDto journey) {
        List<DisruptionDto> disruptions = perturbationService.getDisruptionsForJourney(journey);
        return ResponseEntity.ok(disruptions);
    }

    @PostMapping("/journey/short-term")
    public ResponseEntity<List<DisruptionDto>> getShortTermDisruptionsForJourney(@RequestBody PrimJourneyPlanDto journey) {
        List<DisruptionDto> disruptions = perturbationService.getShortTermDisruptionsForJourney(journey);
        return ResponseEntity.ok(disruptions);
    }

    @GetMapping("/line/{lineIdOrCode}")
    public ResponseEntity<List<DisruptionDto>> getDisruptionsForLine(@PathVariable String lineIdOrCode) {
        List<DisruptionDto> disruptions = perturbationService.getDisruptionsForLine(lineIdOrCode);
        return ResponseEntity.ok(disruptions);
    }

    @GetMapping("/line/{lineIdOrCode}/short-term")
    public ResponseEntity<List<DisruptionDto>> getShortTermDisruptionsForLine(@PathVariable String lineIdOrCode) {
        List<DisruptionDto> disruptions = perturbationService.getShortTermDisruptionsForLine(lineIdOrCode);
        return ResponseEntity.ok(disruptions);
    }
}