package org.marly.mavigo.client.google;

import java.util.List;
import java.util.Optional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.client.google.dto.TasksPage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.security.oauth2.client.web.reactive.function.client
        .ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@RestController
@RequestMapping("/api/google/tasks")
public class GoogleTasksController {

    private final WebClient googleApiWebClient;

    public GoogleTasksController(WebClient googleApiWebClient) {
        this.googleApiWebClient = googleApiWebClient;
    }

    record TaskListDto(String id, String title) {}
    record TaskListsResponse(List<TaskListDto> items, String nextPageToken) {}

    @GetMapping("/me/lists")
    public List<TaskListDto> lists(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String pageToken) {

        try {
            var resp = googleApiWebClient.get()
                    .uri(uri -> uri.path("/users/@me/lists")
                            .queryParam("maxResults", pageSize == null ? 50 : pageSize)
                            .queryParamIfPresent("pageToken", Optional.ofNullable(pageToken))
                            .build())
                    // 👇 force l’utilisation du jeton de l’utilisateur connecté
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .bodyToMono(TaskListsResponse.class)
                    .block();

            return (resp == null || resp.items == null) ? List.of() : resp.items;

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    @GetMapping("/lists/{listId}/tasks")
    public List<TaskDto> tasks(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String listId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean includeCompleted
    ) {
        try {
            // 1) Appel Google SANS dueMin/dueMax (certains tenants de l'API Tasks ne les supportent pas)
            var uri = UriComponentsBuilder.fromPath("/lists/{taskListId}/tasks")
                    .queryParam("maxResults", 100)
                    .queryParam("showHidden", false)
                    .queryParam("showDeleted", false)
                    .queryParam("showCompleted", includeCompleted)
                    .build(listId);

            var resp = googleApiWebClient.get()
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
            var items = (resp == null || resp.items() == null) ? List.<TaskDto>of() : resp.items();

            // 2) Filtrage par date côté Java : on garde uniquement les tâches dont le "due" correspond à la date passée
            if (date != null) {
                items = items.stream()
                        .filter(t -> isDueOnDate(t.due(), date))
                        .toList();
            }

            // 3) Si includeCompleted=false, on s’assure aussi côté Java au cas où
            if (!includeCompleted) {
                items = items.stream()
                        .filter(t -> t.status() == null || !"completed".equalsIgnoreCase(t.status()))
                        .toList();
            }

            return items;

        } catch (WebClientResponseException e) {
            // Renvoyer le code d’erreur précis de Google (401/403/400) au lieu d’un 500 générique
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            // Cas de décodage JSON, URI, NPE, etc. => 500 avec message lisible
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tasks endpoint failed: " + e.getMessage(),
                    e
            );
        }
    }

// ---- Helpers ----

    // Accepte "YYYY-MM-DD" ou "RFC3339" (souvent "YYYY-MM-DDT00:00:00.000Z")
    private boolean isDueOnDate(String due, LocalDate target) {
        if (due == null || due.isBlank()) return false;
        try {
            // Tentative 1 : RFC3339 -> LocalDate(UTC)
            Instant instant = Instant.parse(due);
            LocalDate asUtc = instant.atZone(ZoneOffset.UTC).toLocalDate();
            return asUtc.equals(target);
        } catch (Exception ignore) {
            // Tentative 2 : date simple "YYYY-MM-DD"
            try {
                LocalDate onlyDate = LocalDate.parse(due);
                return onlyDate.equals(target);
            } catch (Exception ignore2) {
                return false;
            }
        }
    }

    @GetMapping("/lists/{listId}/tasks/raw")
    public String tasksRaw(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String listId
    ) {
        try {
            return googleApiWebClient.get()
                    .uri(b -> b.path("/lists/{taskListId}/tasks")
                            .queryParam("maxResults", 5)
                            .queryParam("showHidden", false)
                            .queryParam("showDeleted", false)
                            .build(listId))
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }
}