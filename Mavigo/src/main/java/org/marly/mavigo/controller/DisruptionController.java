package org.marly.mavigo.controller;

import java.util.List;
import java.util.UUID;

import org.marly.mavigo.controller.dto.LineDisruptionRequest;
import org.marly.mavigo.controller.dto.LineInfoResponse;
import org.marly.mavigo.controller.dto.RerouteResponse;
import org.marly.mavigo.controller.dto.StationDisruptionRequest;
import org.marly.mavigo.controller.dto.StopInfoResponse;
import org.marly.mavigo.service.disruption.DisruptionReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journeys/{journeyId}")
public class DisruptionController {

    private final DisruptionReportingService disruptionService;

    public DisruptionController(DisruptionReportingService disruptionService) {
        this.disruptionService = disruptionService;
    }

    @GetMapping("/lines")
    public ResponseEntity<List<LineInfoResponse>> getLines(@PathVariable UUID journeyId) {
        var lines = disruptionService.getLinesForJourney(journeyId).stream()
                .map(LineInfoResponse::from)
                .toList();
        return ResponseEntity.ok(lines);
    }

    @GetMapping("/stops")
    public ResponseEntity<List<StopInfoResponse>> getStops(@PathVariable UUID journeyId) {
        var stops = disruptionService.getStopsForJourney(journeyId).stream()
                .map(StopInfoResponse::from)
                .toList();
        return ResponseEntity.ok(stops);
    }

    @PostMapping("/disruptions/station")
    public ResponseEntity<RerouteResponse> reportStation(
            @PathVariable UUID journeyId,
            @RequestBody StationDisruptionRequest request) {
        if (request.stopPointId() == null || request.stopPointId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var result = disruptionService.reportStationDisruption(journeyId, request.stopPointId());
        return ResponseEntity.ok(RerouteResponse.from(result));
    }

    @PostMapping("/disruptions/line")
    public ResponseEntity<RerouteResponse> reportLine(
            @PathVariable UUID journeyId,
            @RequestBody LineDisruptionRequest request) {
        if (request.lineCode() == null || request.lineCode().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var result = disruptionService.reportLineDisruption(journeyId, request.lineCode());
        return ResponseEntity.ok(RerouteResponse.from(result));
    }
}
