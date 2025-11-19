package org.marly.mavigo.controller;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimJourneyRequest;
import org.marly.mavigo.client.prim.PrimJourneyResponse;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class JourneyController {

    private final StopAreaService stopAreaService;
    private final PrimApiClient primApiClient;

    public JourneyController(StopAreaService stopAreaService, PrimApiClient primApiClient) {
        this.stopAreaService = stopAreaService;
        this.primApiClient = primApiClient;
    }

    @GetMapping("/api/journeys")
    public PrimJourneyResponse getJourneys(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String departure // optional: ISO datetime
    ) {
        // Chercher ou créer les stop areas à partir des noms ou IDs fournis
        StopArea stopAreaFrom = stopAreaService.findOrCreateByQuery(from);
        StopArea stopAreaTo = stopAreaService.findOrCreateByQuery(to);

        // Déterminer l'heure de départ
        LocalDateTime departureTime = departure != null ?
                LocalDateTime.parse(departure) : LocalDateTime.now();

        // Construire la requête pour Prim
        PrimJourneyRequest journeyRequest = new PrimJourneyRequest(
                stopAreaFrom.getExternalId(),
                stopAreaTo.getExternalId(),
                departureTime
        );

        // Appeler PrimApiClient pour récupérer les journeys
        return primApiClient.getJourney(journeyRequest);
    }
}
