package org.marly.mavigo.controller;

import org.marly.mavigo.controller.dto.JourneyPreferencesRequest;
import org.marly.mavigo.controller.dto.JourneyResponse;
import org.marly.mavigo.controller.dto.PlanJourneyCommand;
import org.marly.mavigo.controller.dto.PlanJourneyRequest;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.marly.mavigo.service.journey.JourneyManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyPlanningService journeyPlanningService;
    private final JourneyManagementService journeyManagementService;

    public JourneyController(JourneyPlanningService journeyPlanningService, JourneyManagementService journeyManagementService) {
        this.journeyPlanningService = journeyPlanningService;
        this.journeyManagementService = journeyManagementService;
    }

    @PostMapping
    public ResponseEntity<java.util.List<JourneyResponse>> planJourney(@Valid @RequestBody PlanJourneyCommand command) {
        PlanJourneyRequest request = command.journey();
        JourneyPreferences preferences = mapPreferences(command.preferences());

        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                request.userId(),
                request.originQuery(),
                request.destinationQuery(),
                request.departureTime(),
                preferences);

        java.util.List<Journey> journeys = journeyPlanningService.planAndPersist(parameters);
        java.util.List<JourneyResponse> responses = journeys.stream()
                .map(JourneyResponse::from)
                .toList();
                
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<JourneyResponse> startJourney(@PathVariable java.util.UUID id) {
        Journey journey = journeyManagementService.startJourney(id);
        return ResponseEntity.ok(JourneyResponse.from(journey));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<JourneyResponse> completeJourney(@PathVariable java.util.UUID id) {
        Journey journey = journeyManagementService.completeJourney(id);
        return ResponseEntity.ok(JourneyResponse.from(journey));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<JourneyResponse> cancelJourney(@PathVariable java.util.UUID id) {
        Journey journey = journeyManagementService.cancelJourney(id);
        return ResponseEntity.ok(JourneyResponse.from(journey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JourneyResponse> getJourney(@PathVariable java.util.UUID id) {
        Journey journey = journeyManagementService.getJourney(id);
        return ResponseEntity.ok(JourneyResponse.from(journey));
    }

    private JourneyPreferences mapPreferences(JourneyPreferencesRequest preferencesRequest) {
        if (preferencesRequest == null) {
            return JourneyPreferences.disabled();
        }
        return new JourneyPreferences(preferencesRequest.comfortMode(), preferencesRequest.touristicMode());
    }
}
