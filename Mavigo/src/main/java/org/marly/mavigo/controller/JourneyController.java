package org.marly.mavigo.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.marly.mavigo.controller.dto.JourneyPreferencesRequest;
import org.marly.mavigo.controller.dto.JourneyResponse;
import org.marly.mavigo.controller.dto.PlanJourneyCommand;
import org.marly.mavigo.controller.dto.PlanJourneyRequest;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.TaskOnRouteService;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.marly.mavigo.service.journey.JourneyManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyPlanningService journeyPlanningService;
    private final UserTaskRepository userTaskRepository;
    private final UserRepository userRepository;
    private final TaskOnRouteService taskOnRouteService;
    private final JourneyManagementService journeyManagementService;

    public JourneyController(
            JourneyPlanningService journeyPlanningService,
            UserTaskRepository userTaskRepository,
            UserRepository userRepository,
            TaskOnRouteService taskOnRouteService,
            JourneyManagementService journeyManagementService) {
        this.journeyPlanningService = journeyPlanningService;
        this.userTaskRepository = userTaskRepository;
        this.userRepository = userRepository;
        this.taskOnRouteService = taskOnRouteService;
        this.journeyManagementService = journeyManagementService;
    }

    @PostMapping
    public ResponseEntity<java.util.List<JourneyResponse>> planJourney(@Valid @RequestBody PlanJourneyCommand command) {
        PlanJourneyRequest request = command.journey();
        JourneyPreferences preferences = mapPreferences(command.preferences());

        LocalDateTime departure = parseDepartureTime(request.departureTime());

        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                request.userId(),
                request.originQuery(),
                request.destinationQuery(),
                departure,
                preferences);

        // 1) Planifier + persister les trajets (reroutage-task returns List<Journey>)
        java.util.List<Journey> journeys = journeyPlanningService.planAndPersist(parameters);

        // 2) Calculer les tâches "sur le chemin" pour chaque trajet (polyline
        // densifiée)
        final double BASE_RADIUS_METERS = 300.0;

        java.util.List<JourneyResponse> responses = journeys.stream()
                .map(journey -> {
                    List<JourneyResponse.TaskOnRouteResponse> tasksOnRoute = List.of();

                    UUID userId = (journey.getUser() != null) ? journey.getUser().getId() : null;
                    if (userId != null) {
                        var tasks = userTaskRepository.findByUser_Id(userId);

                        // Points bruts (souvent pas assez denses)
                        var baseRoutePoints = taskOnRouteService.extractRoutePoints(journey);

                        // Densification: on ajoute des points intermédiaires tous les ~200m
                        var polyline = taskOnRouteService.densify(baseRoutePoints, 200);

                        // Si on n'a quasi pas de points, on élargit le rayon pour éviter les faux
                        // négatifs
                        double radius = (polyline == null || polyline.size() <= 3) ? 900.0 : BASE_RADIUS_METERS;

                        tasksOnRoute = tasks.stream()
                                .filter(t -> t != null && !t.isCompleted())
                                .filter(t -> t.getLocationHint() != null)
                                .map(t -> {
                                    double d = taskOnRouteService.minDistanceMetersToPolyline(t.getLocationHint(),
                                            polyline);
                                    return (d <= radius) ? JourneyResponse.fromTask(t, d) : null;
                                })
                                .filter(Objects::nonNull)
                                .toList();
                    }

                    return JourneyResponse.from(journey, tasksOnRoute);
                })
                .toList();

        // 3) Retourner la réponse enrichie
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

    private static LocalDateTime parseDepartureTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departureTime is required");
        }

        String s = raw.trim();

        // 1) cas ISO avec offset: 2025-12-14T18:00:00+01:00
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        // 2) cas ISO local avec secondes: 2025-12-14T18:00:00
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
        }

        // 3) cas ISO local sans secondes: 2025-12-14T18:00
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (DateTimeParseException ignored) {
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "departureTime must be ISO datetime like '2025-12-14T18:00' or '2025-12-14T18:00:00' or '2025-12-14T18:00:00+01:00'");
    }

    // -------------------------
    // DEBUG ENDPOINTS
    // -------------------------

    @GetMapping("/debug/user-tasks")
    public Map<String, Object> debugUserTasks(@RequestParam UUID userId) {
        var tasks = userTaskRepository.findByUser_Id(userId);

        var mapped = tasks.stream().map(t -> Map.<String, Object>of(
                "id", t.getId(),
                "googleTaskId", t.getSourceTaskId(),
                "title", t.getTitle(),
                "completed", t.isCompleted(),
                "source", t.getSource() == null ? null : t.getSource().name(),
                "lat", t.getLocationHint() == null ? null : t.getLocationHint().getLatitude(),
                "lng", t.getLocationHint() == null ? null : t.getLocationHint().getLongitude())).toList();

        return Map.of(
                "userId", userId.toString(),
                "taskCount", tasks.size(),
                "tasks", mapped);
    }

    @PostMapping("/debug/seed-task-near-gare-de-lyon")
    public Map<String, Object> seedTaskNearGareDeLyon(@RequestBody SeedTaskRequest request) {
        if (request == null || request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        String title = (request.title() == null || request.title().isBlank())
                ? "Acheter du lait"
                : request.title().trim();

        User user = userRepository.findById(request.userId())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + request.userId()));

        UserTask task = new UserTask(
                user,
                "seed-" + UUID.randomUUID(),
                TaskSource.GOOGLE_TASKS,
                title);
        task.setCompleted(false);
        task.setLocationHint(new GeoPoint(48.8443, 2.3730)); // Gare de Lyon approx

        UserTask saved = userTaskRepository.save(task);

        return Map.of(
                "seeded", true,
                "userId", user.getId().toString(),
                "taskId", saved.getId(),
                "title", saved.getTitle(),
                "lat", saved.getLocationHint().getLatitude(),
                "lng", saved.getLocationHint().getLongitude());
    }

    public record SeedTaskRequest(UUID userId, String title) {
    }

    private JourneyPreferences mapPreferences(JourneyPreferencesRequest preferencesRequest) {
        if (preferencesRequest == null) {
            return JourneyPreferences.disabled();
        }
        return new JourneyPreferences(
                preferencesRequest.comfortMode(),
                preferencesRequest.touristicMode(),
                preferencesRequest.namedComfortSettingId());
    }
}
