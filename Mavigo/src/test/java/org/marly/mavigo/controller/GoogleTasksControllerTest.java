package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleTasksControllerTest {

    private WebClient googleApiWebClient;
    private OAuth2AuthorizedClientService authorizedClientService;
    private UserService userService;
    private UserRepository userRepository;
    private UserTaskRepository userTaskRepository;
    private PrimApiClient primApiClient;

    private GoogleTasksController controller;

    @BeforeEach
    void setup() {
        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();

            if ("POST".equals(request.method().name()) && path.contains("/tasks")) {
                String json = "{\"id\":\"g-task-123\",\"title\":\"dummy\"}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }

            if ("GET".equals(request.method().name()) && path.contains("/lists") && !path.contains("/tasks")) {
                String json = "{\"items\":[{\"id\":\"list-1\",\"title\":\"My Tasks\"}]}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }

            if ("DELETE".equals(request.method().name())) {
                return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
            }

            if ("PATCH".equals(request.method().name())) {
                String json = "{\"id\":\"g-task-123\",\"status\":\"completed\"}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }

            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };

        this.googleApiWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        this.authorizedClientService = mock(OAuth2AuthorizedClientService.class);
        this.userService = mock(UserService.class);
        this.userRepository = mock(UserRepository.class);
        this.userTaskRepository = mock(UserTaskRepository.class);
        this.primApiClient = mock(PrimApiClient.class);

        this.controller = new GoogleTasksController(
                googleApiWebClient,
                authorizedClientService,
                userService,
                userRepository,
                userTaskRepository,
                primApiClient
        );
    }

    @Test
    void controllerShouldInstantiate() {
        assertNotNull(controller);
    }

    @Test
    void createTaskRequestRecordShouldWork() {
        var req = new GoogleTasksController.CreateTaskRequest("title", "notes", null, "gare de lyon");
        assertEquals("title", req.title());
        assertEquals("notes", req.notes());
        assertNull(req.due());
        assertEquals("gare de lyon", req.locationQuery());
    }

    @Test
    void createTaskRequestWithDueDateShouldWork() {
        LocalDate due = LocalDate.of(2026, 2, 15);
        var req = new GoogleTasksController.CreateTaskRequest("Buy groceries", "Milk and eggs", due, "supermarket");
        assertEquals("Buy groceries", req.title());
        assertEquals("Milk and eggs", req.notes());
        assertEquals(due, req.due());
        assertEquals("supermarket", req.locationQuery());
    }

    // ========== /me endpoint tests ==========

    @Test
    void me_ShouldReturnPrincipalInfo() {
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute("sub")).thenReturn("google-sub-123");
        when(principal.getAttribute("email")).thenReturn("test@gmail.com");
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("iss")).thenReturn("https://accounts.google.com");

        Map<String, Object> result = controller.me(principal);

        assertEquals("google-sub-123", result.get("sub"));
        assertEquals("test@gmail.com", result.get("email"));
        assertEquals("Test User", result.get("name"));
        assertEquals("https://accounts.google.com", result.get("issuer"));
    }

    @Test
    void me_ShouldThrowWhenPrincipalIsNull() {
        assertThrows(ResponseStatusException.class, () -> controller.me(null));
    }

    // ========== /token endpoint tests ==========

    @Test
    void token_ShouldReturnTokenInfo() {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(client.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getScopes()).thenReturn(Set.of("openid", "email"));
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(accessToken.getExpiresAt()).thenReturn(expiresAt);

        Map<String, Object> result = controller.token(client);

        assertEquals(Set.of("openid", "email"), result.get("scopes"));
        assertEquals(expiresAt, result.get("expiresAt"));
    }

    @Test
    void token_ShouldThrowWhenClientIsNull() {
        assertThrows(ResponseStatusException.class, () -> controller.token(null));
    }

    @Test
    void token_ShouldThrowWhenAccessTokenIsNull() {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> controller.token(client));
    }

    // ========== /users/{userId}/local endpoint tests ==========

    @Test
    void localTasks_ShouldReturnUserTasks() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        
        UserTask task1 = new UserTask(user, "g-task-1", TaskSource.GOOGLE_TASKS, "Task 1");
        task1.setNotes("Notes for task 1");
        task1.setLocationQuery("Gare de Lyon");
        task1.setLocationHint(new GeoPoint(48.8448, 2.3735));
        
        UserTask task2 = new UserTask(user, "g-task-2", TaskSource.GOOGLE_TASKS, "Task 2");
        task2.setCompleted(true);

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(task1, task2));

        List<Map<String, Object>> result = controller.localTasks(userId);

        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).get("title"));
        assertEquals("Gare de Lyon", result.get(0).get("locationQuery"));
        assertNotNull(result.get(0).get("locationHint"));
        assertEquals(true, result.get(1).get("completed"));
    }

    @Test
    void localTasks_ShouldReturnEmptyListForNoTasks() {
        UUID userId = UUID.randomUUID();
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());

        List<Map<String, Object>> result = controller.localTasks(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void localTasks_ShouldHandleNullLocationHint() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        
        UserTask task = new UserTask(user, "g-task-1", TaskSource.GOOGLE_TASKS, "Task 1");
        task.setLocationHint(null);

        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of(task));

        List<Map<String, Object>> result = controller.localTasks(userId);

        assertEquals(1, result.size());
        assertNull(result.get(0).get("locationHint"));
    }

    // ========== /link endpoint tests ==========

    @Test
    void linkGoogleAccount_ShouldThrowWhenPrincipalIsNull() {
        UUID userId = UUID.randomUUID();
        assertThrows(ResponseStatusException.class, () -> controller.linkGoogleAccount(userId, null));
    }

    @Test
    void linkGoogleAccount_ShouldThrowWhenSubjectMissing() {
        UUID userId = UUID.randomUUID();
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute("sub")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> controller.linkGoogleAccount(userId, principal));
    }

    @Test
    void linkGoogleAccount_ShouldSuccessfullyLink() {
        UUID userId = UUID.randomUUID();
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute("sub")).thenReturn("google-sub-123");
        when(principal.getAttribute("email")).thenReturn("test@gmail.com");

        User linkedUser = new User("ext-1", "test@test.com", "Test User");
        linkedUser.setGoogleAccountEmail("test@gmail.com");
        linkedUser.setGoogleAccountSubject("google-sub-123");

        when(userService.linkGoogleAccount(eq(userId), any())).thenReturn(linkedUser);

        ResponseEntity<String> result = controller.linkGoogleAccount(userId, principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().contains("Google Tasks linked"));
        assertTrue(result.getBody().contains("test@gmail.com"));
    }

    @Test
    void linkGoogleAccount_ShouldReturnErrorHtmlOnException() {
        UUID userId = UUID.randomUUID();
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute("sub")).thenReturn("google-sub-123");
        when(principal.getAttribute("email")).thenReturn("test@gmail.com");

        when(userService.linkGoogleAccount(eq(userId), any()))
            .thenThrow(new RuntimeException("Account already linked"));

        ResponseEntity<String> result = controller.linkGoogleAccount(userId, principal);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertTrue(result.getBody().contains("Link Failed"));
    }

    // ========== Default list endpoint tests ==========

    @Test
    void defaultListForUser_ShouldThrowWhenNoGoogleAccount() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        user.setGoogleAccountSubject(null);

        when(userService.getUser(userId)).thenReturn(user);

        assertThrows(ResponseStatusException.class, () -> controller.defaultListForUser(userId));
    }

    // ========== Create task validation tests ==========

    @Test
    void createTaskForUser_ShouldThrowWhenRequestIsNull() {
        UUID userId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class, 
            () -> controller.createTaskForUser(userId, "list-1", null));
    }

    @Test
    void createTaskForUser_ShouldThrowWhenTitleIsBlank() {
        UUID userId = UUID.randomUUID();
        var request = new GoogleTasksController.CreateTaskRequest("", "notes", null, null);

        assertThrows(ResponseStatusException.class, 
            () -> controller.createTaskForUser(userId, "list-1", request));
    }

    @Test
    void createTaskForUser_ShouldThrowWhenUserHasNoGoogleAccount() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        user.setGoogleAccountSubject(null);

        when(userService.getUser(userId)).thenReturn(user);

        var request = new GoogleTasksController.CreateTaskRequest("Task Title", "notes", null, null);

        assertThrows(ResponseStatusException.class, 
            () -> controller.createTaskForUser(userId, "list-1", request));
    }
}

