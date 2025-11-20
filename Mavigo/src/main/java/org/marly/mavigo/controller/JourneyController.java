package org.marly.mavigo.controller;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimJourneyRequest;
import org.marly.mavigo.client.prim.PrimJourneyResponse;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JourneyController {

    @Autowired
    private StopAreaService stopAreaService;

    @Autowired
    private PrimApiClient primApiClient;

    @GetMapping("/journeys")
    public ResponseEntity<?> getJourneys(
            @RequestParam String from,
            @RequestParam String to
    ) {
        try {
            // 1. Find or create stop areas
            StopArea stopAreaFrom = stopAreaService.findOrCreateByQuery(from);
            StopArea stopAreaTo = stopAreaService.findOrCreateByQuery(to);

            // 2. Set departure time (tomorrow 9:00)
            LocalDateTime departureTime = LocalDateTime.now().plusDays(1)
                    .withHour(9).withMinute(0).withSecond(0).withNano(0);

            // 3. Build journey request
            PrimJourneyRequest journeyRequest = new PrimJourneyRequest(
                    stopAreaFrom.getExternalId(),
                    stopAreaTo.getExternalId(),
                    departureTime
            );

            // 4. Call Prim API
            PrimJourneyResponse journeyResponse = primApiClient.getJourney(journeyRequest);

            if (journeyResponse != null && journeyResponse.journeys() != null) {
                List<?> journeys = journeyResponse.journeys();
                return ResponseEntity.ok(journeys);
            } else {
                return ResponseEntity.ok(List.of());
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
