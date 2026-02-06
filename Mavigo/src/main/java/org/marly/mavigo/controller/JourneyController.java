package org.marly.mavigo.controller;

import java.nio.charset.StandardCharsets;
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
import org.marly.mavigo.service.journey.JourneyOptimizationService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JourneyController.class);

    private final JourneyPlanningService journeyPlanningService;
    private final UserTaskRepository userTaskRepository;
    private final UserRepository userRepository;
    private final TaskOnRouteService taskOnRouteService;
    private final JourneyManagementService journeyManagementService;
    private final JourneyOptimizationService journeyOptimizationService;

    public JourneyController(
            JourneyPlanningService journeyPlanningService,
            UserTaskRepository userTaskRepository,
            UserRepository userRepository,
            TaskOnRouteService taskOnRouteService,
            JourneyManagementService journeyManagementService,
            JourneyOptimizationService journeyOptimizationService) {
        this.journeyPlanningService = journeyPlanningService;
        this.userTaskRepository = userTaskRepository;
        this.userRepository = userRepository;
        this.taskOnRouteService = taskOnRouteService;
        this.journeyManagementService = journeyManagementService;
        this.journeyOptimizationService = journeyOptimizationService;
    }

    @PostMapping
    public ResponseEntity<java.util.List<JourneyResponse>> planJourney(@Valid @RequestBody PlanJourneyCommand command) {
        PlanJourneyRequest request = command.journey();
        boolean ecoModeEnabled = Boolean.TRUE.equals(request.ecoModeEnabled());
        JourneyPreferences preferences = mapPreferences(command.preferences(), ecoModeEnabled);

        LocalDateTime departure = parseDepartureTime(request.departureTime());

        JourneyPlanningParameters parameters = new JourneyPlanningParameters(
                request.userId(),
                request.originQuery(),
                request.destinationQuery(),
                departure,
                preferences,
                ecoModeEnabled);

        java.util.List<JourneyResponse> responses;
        boolean useTaskOptimization = (request.taskDetails() != null && !request.taskDetails().isEmpty())
                || (request.taskIds() != null && !request.taskIds().isEmpty());

        if (useTaskOptimization) {
            var optimizedResults = (request.taskDetails() != null && !request.taskDetails().isEmpty())
                    ? journeyOptimizationService.planOptimizedJourneyWithTaskDetails(parameters, request.taskDetails())
                    : journeyOptimizationService.planOptimizedJourneyWithTasks(parameters,
                            request.taskIds() != null ? request.taskIds() : List.of());

            if (optimizedResults.isEmpty()) {
                LOGGER.warn("Optimization failed, falling back to normal journey");
                java.util.List<Journey> normalJourneys = journeyPlanningService.planAndPersist(parameters);
                responses = normalJourneys.stream()
                        .map(j -> JourneyResponse.from(j, calculateTasksOnRoute(j)))
                        .toList();
            } else {
                responses = optimizedResults.stream()
                        .map(result -> {
                            List<JourneyResponse.TaskOnRouteResponse> tasksOnRoute = calculateTasksOnRoute(
                                    result.journey());
                            long baseAdd = result.totalDurationSeconds() - result.baseDurationSeconds();
                            List<JourneyResponse.IncludedTaskResponse> includedTasks = result.includedTasks().stream()
                                    .map(t -> new JourneyResponse.IncludedTaskResponse(
                                            toUuid(t.id()),
                                            t.title(),
                                            t.locationQuery(),
                                            result.includedTasks().size() > 1 ? baseAdd / result.includedTasks().size()
                                                    : baseAdd,
                                            t.id()))
                                    .toList();
                            return JourneyResponse.fromOptimized(
                                    result.journey(), tasksOnRoute, includedTasks, result.baseDurationSeconds());
                        })
                        .toList();
            }
        } else {
            // Trajet normal sans optimisation
            java.util.List<Journey> journeys = journeyPlanningService.planAndPersist(parameters);
            responses = journeys.stream()
                    .map(journey -> JourneyResponse.from(journey, calculateTasksOnRoute(journey)))
                    .toList();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Calcule les tâches "sur le chemin" pour un trajet donné.
     */
    private List<JourneyResponse.TaskOnRouteResponse> calculateTasksOnRoute(Journey journey) {
        final double BASE_RADIUS_METERS = 300.0;
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
                        double d = taskOnRouteService.minDistanceMetersToPolyline(t.getLocationHint(), polyline);
                        return (d <= radius) ? JourneyResponse.fromTask(t, d) : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
        return tasksOnRoute;
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<JourneyResponse> startJourney(@PathVariable java.util.UUID id) {
        org.marly.mavigo.service.journey.JourneyActionResult result = journeyManagementService.startJourney(id);
        return ResponseEntity.ok(JourneyResponse.from(result.journey(), null, result.newBadges()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<JourneyResponse> completeJourney(@PathVariable java.util.UUID id) {
        org.marly.mavigo.service.journey.JourneyActionResult result = journeyManagementService.completeJourney(id);
        return ResponseEntity.ok(JourneyResponse.from(result.journey(), null, result.newBadges()));
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

    @org.springframework.web.bind.annotation.DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllJourneys() {
        LOGGER.info("Request to delete all journeys and activity data");
        journeyManagementService.clearAllData();
        return ResponseEntity.noContent().build();
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

    private static UUID toUuid(String id) {
        if (id == null || id.isBlank())
            return null;
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
        }
    }

    private JourneyPreferences mapPreferences(JourneyPreferencesRequest preferencesRequest, boolean ecoModeEnabled) {
        if (preferencesRequest == null) {
            return new JourneyPreferences(false, ecoModeEnabled, null);
        }
        return new JourneyPreferences(
                preferencesRequest.comfortMode(),
                ecoModeEnabled,
                preferencesRequest.namedComfortSettingId());
    }
}
