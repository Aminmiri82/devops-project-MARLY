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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyPlanningService journeyPlanningService;

    public JourneyController(JourneyPlanningService journeyPlanningService) {
        this.journeyPlanningService = journeyPlanningService;
    }

    @PostMapping
    public ResponseEntity<JourneyResponse> planJourney(@Valid @RequestBody PlanJourneyCommand command) {
        PlanJourneyRequest request = command.journey();
        JourneyPreferences preferences = mapPreferences(command.preferences());

        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                request.userId(),
                request.originQuery(),
                request.destinationQuery(),
                request.departureTime(),
                preferences);

        Journey journey = journeyPlanningService.planAndPersist(parameters);
        return ResponseEntity.status(HttpStatus.CREATED).body(JourneyResponse.from(journey));
    }

    private JourneyPreferences mapPreferences(JourneyPreferencesRequest preferencesRequest) {
        if (preferencesRequest == null) {
            return JourneyPreferences.disabled();
        }
        return new JourneyPreferences(preferencesRequest.comfortMode(), preferencesRequest.touristicMode());
    }
}
