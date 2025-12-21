package org.marly.mavigo.controller;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.client.google.dto.TaskListDto;
import org.marly.mavigo.client.google.dto.TasksListsResponse;
import org.marly.mavigo.client.google.dto.TasksPage;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimPlace;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.user.UserService;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/google/tasks")
public class GoogleTasksController {

    private final WebClient googleApiWebClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    private final UserService userService;
    private final UserTaskRepository userTaskRepository;
    private final PrimApiClient primApiClient;

    public GoogleTasksController(
            WebClient googleApiWebClient,
            OAuth2AuthorizedClientService authorizedClientService,
            UserService userService,
            UserRepository userRepository,
            UserTaskRepository userTaskRepository,
            PrimApiClient primApiClient) {
        this.googleApiWebClient = googleApiWebClient;
        this.authorizedClientService = authorizedClientService;
        this.userService = userService;
        this.userTaskRepository = userTaskRepository;
        this.primApiClient = primApiClient;
    }

    // -----------------------------
    // ME (debug)
    // -----------------------------
    @GetMapping("/me/lists")
    public List<TaskListDto> listsForMe(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String pageToken) {
        return fetchLists(authorizedClient, pageSize, pageToken);
    }

    @GetMapping("/me/lists/{listId}/tasks")
    public List<TaskDto> tasksForMe(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String listId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean includeCompleted) {
        return fetchTasks(authorizedClient, listId, date, includeCompleted);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Google principal");
        }
        return Map.of(
                "sub", principal.getAttribute("sub"),
                "email", principal.getAttribute("email"),
                "name", principal.getAttribute("name"),
                "issuer", principal.getAttribute("iss"));
    }

    @GetMapping("/token")
    public Map<String, Object> token(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Google authorized client");
        }
        return Map.of(
                "scopes", client.getAccessToken().getScopes(),
                "expiresAt", client.getAccessToken().getExpiresAt());
    }

    // -----------------------------
    // USER endpoints (used by your front)
    // -----------------------------
    @GetMapping("/users/{userId}/lists")
    public List<TaskListDto> listsForUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String pageToken) {
        OAuth2AuthorizedClient client = requireAuthorizedClientForUser(userId);
        return fetchLists(client, pageSize, pageToken);
    }

    /**
     * ✅ New: "one list" UX.
     * Returns the default list used by the UI.
     */
    @GetMapping("/users/{userId}/default-list")
    public Map<String, Object> defaultListForUser(@PathVariable UUID userId) {
        OAuth2AuthorizedClient client = requireAuthorizedClientForUser(userId);
        List<TaskListDto> lists = fetchLists(client, 50, null);

        if (lists == null || lists.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No task list found on Google account");
        }

        TaskListDto chosen = lists.get(0); // simplest: first list as default
        return Map.of("id", chosen.id(), "title", chosen.title());
    }

    @GetMapping("/users/{userId}/lists/{listId}/tasks")
    public List<TaskDto> tasksForUser(
            @PathVariable UUID userId,
            @PathVariable String listId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean includeCompleted) {
        OAuth2AuthorizedClient client = requireAuthorizedClientForUser(userId);
        return fetchTasks(client, listId, date, includeCompleted);
    }

    @GetMapping("/users/{userId}/local")
    public List<Map<String, Object>> localTasks(@PathVariable UUID userId) {
        return userTaskRepository.findByUser_Id(userId)
                .stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("source", t.getSource());
                    m.put("googleTaskId", t.getSourceTaskId());
                    m.put("title", t.getTitle());
                    m.put("notes", t.getNotes());
                    m.put("dueAt", t.getDueAt());
                    m.put("completed", t.isCompleted());

                    if (t.getLocationHint() != null) {
                        m.put("locationHint", Map.of(
                                "lat", t.getLocationHint().getLatitude(),
                                "lng", t.getLocationHint().getLongitude()));
                    } else {
                        m.put("locationHint", null);
                    }
                    return m;
                })
                .toList();
    }

    @GetMapping(value = "/link", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> linkGoogleAccount(
            @RequestParam UUID userId,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Google principal");
        }

        String subject = principal.getAttribute("sub");
        if (!StringUtils.hasText(subject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google subject not present");
        }

        String email = principal.getAttribute("email");

        try {
            User linkedUser = userService.linkGoogleAccount(userId, new GoogleAccountLink(subject, email));
            String html = buildLinkSuccessHtml(linkedUser.getDisplayName(), linkedUser.getGoogleAccountEmail());
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            String html = buildLinkErrorHtml(e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.TEXT_HTML).body(html);
        }
    }

    @PostMapping("/users/{userId}/lists/{listId}/tasks")
    public Map<String, Object> createTaskForUser(
            @PathVariable UUID userId,
            @PathVariable String listId,
            @RequestBody CreateTaskRequest request) {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (!StringUtils.hasText(request.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }

        User user = userService.getUser(userId);
        if (!StringUtils.hasText(user.getGoogleAccountSubject())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User has no Google account linked. Use /api/google/tasks/link first.");
        }

        OAuth2AuthorizedClient client = requireAuthorizedClientForUser(userId);

        // ✅ PRIM best-effort
        GeoPoint locationHint = null;
        String locationWarning = null;

        if (StringUtils.hasText(request.locationQuery())) {
            try {
                locationHint = resolveGeoPointFromQuery(request.locationQuery());
            } catch (ResponseStatusException ex) {
                locationWarning = ex.getReason();
            } catch (Exception ex) {
                locationWarning = "Location lookup failed: " + ex.getMessage();
            }
        }

        try {
            Map<String, Object> taskBody = new java.util.HashMap<>();
            taskBody.put("title", request.title());

            if (StringUtils.hasText(request.notes())) {
                taskBody.put("notes", request.notes());
            }
            if (request.due() != null) {
                taskBody.put("due", request.due().atStartOfDay(ZoneOffset.UTC).toInstant().toString());
            }

            Map<String, Object> response = googleApiWebClient.post()
                    .uri(b -> b.path("/lists/{taskListId}/tasks").build(listId))
                    .attributes(oauth2AuthorizedClient(client))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(taskBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Google Tasks returned empty response");
            }

            String googleTaskId = response.get("id") == null ? null : String.valueOf(response.get("id"));
            if (!StringUtils.hasText(googleTaskId)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Google task id missing in response");
            }

            UserTask ut = new UserTask(user, googleTaskId, TaskSource.GOOGLE_TASKS, request.title());
            ut.setNotes(request.notes());
            if (request.due() != null) {
                OffsetDateTime dueAt = request.due().atStartOfDay().atOffset(ZoneOffset.UTC);
                ut.setDueAt(dueAt);
            }
            if (locationHint != null && locationHint.isComplete()) {
                ut.setLocationHint(locationHint);
            } else {
                ut.setLocationHint(null);
            }

            UserTask saved = userTaskRepository.save(ut);

            Map<String, Object> enriched = new LinkedHashMap<>(response);
            enriched.put("localTaskId", saved.getId());
            enriched.put("locationResolved", locationHint != null);
            enriched.put("locationWarning", locationWarning);

            if (locationHint != null) {
                enriched.put("locationLat", locationHint.getLatitude());
                enriched.put("locationLng", locationHint.getLongitude());
            }

            return enriched;

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e);
        }
    }

    /**
     * ✅ New: mark completed
     */
    @PatchMapping("/users/{userId}/lists/{listId}/tasks/{taskId}/complete")
    public Map<String, Object> completeTaskForUser(
            @PathVariable UUID userId,
            @PathVariable String listId,
            @PathVariable String taskId) {

        OAuth2AuthorizedClient client = requireAuthorizedClientForUser(userId);

        try {
            Map<String, Object> patch = new java.util.HashMap<>();
            patch.put("status", "completed");
            patch.put("completed", Instant.now().toString());

            Map<String, Object> response = googleApiWebClient.patch()
                    .uri(b -> b.path("/lists/{taskListId}/tasks/{taskId}").build(listId, taskId))
                    .attributes(oauth2AuthorizedClient(client))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(patch)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            // sync local
            userTaskRepository
                    .findByUser_IdAndSourceAndSourceTaskId(userId, TaskSource.GOOGLE_TASKS, taskId)
                    .ifPresent(t -> {
                        t.setCompleted(true);
                        t.setLastSyncedAt(OffsetDateTime.now());
                        userTaskRepository.save(t);
                    });

            return response == null ? Map.of("ok", true) : response;

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e);
        }
    }

    /**
     * ✅ New: delete task
     */
    @DeleteMapping("/users/{userId}/lists/{listId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTaskForUser(
            @PathVariable UUID userId,
            @PathVariable String listId,
            @PathVariable String taskId) {

        OAuth2AuthorizedClient client = requireAuthorizedClientForUser(userId);

        try {
            googleApiWebClient.delete()
                    .uri(b -> b.path("/lists/{taskListId}/tasks/{taskId}").build(listId, taskId))
                    .attributes(oauth2AuthorizedClient(client))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            userTaskRepository
                    .findByUser_IdAndSourceAndSourceTaskId(userId, TaskSource.GOOGLE_TASKS, taskId)
                    .ifPresent(userTaskRepository::delete);

            return ResponseEntity.noContent().build();

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e);
        }
    }

    // -----------------------------
    // Internal helpers
    // -----------------------------
    private List<TaskListDto> fetchLists(OAuth2AuthorizedClient authorizedClient, Integer pageSize, String pageToken) {
        try {
            TasksListsResponse resp = googleApiWebClient.get()
                    .uri(uri -> uri.path("/users/@me/lists")
                            .queryParam("maxResults", pageSize == null ? 50 : pageSize)
                            .queryParamIfPresent("pageToken", Optional.ofNullable(pageToken))
                            .build())
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .bodyToMono(TasksListsResponse.class)
                    .block();

            return (resp == null || resp.items() == null) ? List.of() : resp.items();

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e);
        }
    }

    private List<TaskDto> fetchTasks(OAuth2AuthorizedClient authorizedClient, String listId, LocalDate date,
            boolean includeCompleted) {
        try {
            TasksPage resp = googleApiWebClient.get()
                    .uri(b -> b.path("/lists/{taskListId}/tasks")
                            .queryParam("maxResults", 100)
                            .queryParam("showHidden", false)
                            .queryParam("showDeleted", false)
                            .queryParam("showCompleted", includeCompleted)
                            .build(listId))
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .bodyToMono(TasksPage.class)
                    .block();

            List<TaskDto> items = (resp == null || resp.items() == null) ? List.of() : resp.items();

            if (date != null) {
                items = items.stream().filter(t -> isDueOnDate(t.due(), date)).toList();
            }
            if (!includeCompleted) {
                items = items.stream()
                        .filter(t -> t.status() == null || !"completed".equalsIgnoreCase(t.status()))
                        .toList();
            }

            return items;

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tasks endpoint failed: " + e.getMessage(),
                    e);
        }
    }

    private OAuth2AuthorizedClient requireAuthorizedClientForUser(UUID userId) {
        User user = userService.getUser(userId);

        String subject = user.getGoogleAccountSubject();
        if (!StringUtils.hasText(subject)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User has no Google account linked. Use /api/google/tasks/link first.");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", subject);
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No Google OAuth2 client stored for this user. Re-link Google Tasks.");
        }

        return client;
    }

    private GeoPoint resolveGeoPointFromQuery(String query) {
        try {
            List<PrimPlace> places = primApiClient.searchPlaces(query);
            if (places == null || places.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No location found for: " + query);
            }

            for (PrimPlace p : places) {
                if (p == null || p.stopArea() == null || p.stopArea().coordinates() == null)
                    continue;
                var c = p.stopArea().coordinates();
                if (c.latitude() != null && c.longitude() != null) {
                    return new GeoPoint(c.latitude(), c.longitude());
                }
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No usable coordinates for: " + query);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "PRIM unauthorized (check PRIM API credentials / header)", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PRIM error: " + e.getResponseBodyAsString(), e);
        }
    }

    public record CreateTaskRequest(
            String title,
            String notes,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate due,
            String locationQuery) {
    }

    private boolean isDueOnDate(String due, LocalDate target) {
        if (due == null || due.isBlank())
            return false;

        try {
            Instant instant = Instant.parse(due);
            LocalDate asUtc = instant.atZone(ZoneOffset.UTC).toLocalDate();
            return asUtc.equals(target);
        } catch (Exception ignore) {
            try {
                LocalDate onlyDate = LocalDate.parse(due);
                return onlyDate.equals(target);
            } catch (Exception ignore2) {
                return false;
            }
        }
    }

    private String buildLinkSuccessHtml(String displayName, String email) {
        String escapedEmail = HtmlUtils.htmlEscape(email == null ? "unknown" : email);
        String escapedName = HtmlUtils.htmlEscape(displayName == null ? "Unknown user" : displayName);
        String jsEmail = escapeForJs(email == null ? "" : email);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <title>Google Tasks Linked</title>
                    <style>
                        body { font-family: Arial, sans-serif; padding: 20px; background: #f7f7f7; }
                        .card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
                        button { padding: 8px 16px; font-size: 1rem; cursor: pointer; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>Google Tasks linked</h2>
                        <p>User <strong>%s</strong> is now connected to <strong>%s</strong>.</p>
                        <p>You can close this window.</p>
                        <button onclick="window.close()">Close</button>
                    </div>
                    <script>
                        if (window.opener) {
                            window.opener.postMessage({ type: 'GOOGLE_TASKS_LINKED', email: '%s' }, window.location.origin);
                        }
                    </script>
                </body>
                </html>
                """
                .formatted(escapedName, escapedEmail, jsEmail);
    }

    private String escapeForJs(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String buildLinkErrorHtml(String errorMessage) {
        String escapedError = HtmlUtils.htmlEscape(errorMessage == null ? "Unknown error" : errorMessage);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <title>Google Tasks Link Failed</title>
                    <style>
                        body { font-family: Arial, sans-serif; padding: 20px; background: #f7f7f7; }
                        .card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
                        .error { color: #c0392b; }
                        button { padding: 8px 16px; font-size: 1rem; cursor: pointer; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2 class="error">Link Failed</h2>
                        <p>%s</p>
                        <p>This Google account may already be linked to another user.</p>
                        <button onclick="window.close()">Close</button>
                    </div>
                </body>
                </html>
                """
                .formatted(escapedError);
    }
}