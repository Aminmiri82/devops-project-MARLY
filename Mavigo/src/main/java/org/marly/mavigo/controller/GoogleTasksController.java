package org.marly.mavigo.controller;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.client.google.dto.TaskListDto;
import org.marly.mavigo.client.google.dto.TasksListsResponse;
import org.marly.mavigo.client.google.dto.TasksPage;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.user.UserService;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/google/tasks")
public class GoogleTasksController {

    private final WebClient googleApiWebClient;
    private final UserService userService;

    public GoogleTasksController(WebClient googleApiWebClient, UserService userService) {
        this.googleApiWebClient = googleApiWebClient;
        this.userService = userService;
    }

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
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .bodyToMono(TasksListsResponse.class)
                    .block();

            return (resp == null || resp.items() == null) ? List.of() : resp.items();

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

            if (date != null) {
                items = items.stream()
                        .filter(t -> isDueOnDate(t.due(), date))
                        .toList();
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
                    e
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tasks endpoint failed: " + e.getMessage(),
                    e
            );
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
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Google Tasks API error: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        return Map.of(
                "sub", principal.getAttribute("sub"),
                "email", principal.getAttribute("email"),
                "name", principal.getAttribute("name"),
                "issuer", principal.getAttribute("iss")
        );
    }

    @GetMapping("/token")
    public Map<String, Object> token(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        return Map.of(
                "scopes", client.getAccessToken().getScopes(),
                "expiresAt", client.getAccessToken().getExpiresAt()
        );
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
        User linkedUser = userService.linkGoogleAccount(userId, new GoogleAccountLink(subject, email));
        String html = buildLinkSuccessHtml(linkedUser.getDisplayName(), linkedUser.getGoogleAccountEmail());

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private boolean isDueOnDate(String due, LocalDate target) {
        if (due == null || due.isBlank()) {
            return false;
        }
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
        String jsEmail = escapeForJs(escapedEmail);

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
                """.formatted(escapedName, escapedEmail, jsEmail);
    }

    private String escapeForJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}

