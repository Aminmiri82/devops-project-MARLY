package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleTasksControllerTest {

    private WebClient googleApiWebClient;
    private OAuth2AuthorizedClientService authorizedClientService;
    private UserService userService;
    private UserTaskRepository userTaskRepository;
    private PrimApiClient primApiClient;

    private GoogleTasksController controller;

    @BeforeEach
    void setup() {
        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();

            if ("GET".equals(request.method().name()) && "/users/@me/lists".equals(path)) {
                String json = "{\"items\":[{\"id\":\"list-1\",\"title\":\"Default\"}]}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }

            if ("GET".equals(request.method().name()) && path.contains("/lists/") && path.endsWith("/tasks")) {
                String json = """
                        {
                          "items": [
                            {
                              "id": "task-1",
                              "title": "Buy shoes",
                              "notes": "Remember this\\n#mavigo: Chatelet",
                              "status": "needsAction",
                              "due": "2025-01-02T00:00:00Z"
                            },
                            {
                              "id": "task-2",
                              "title": "No location tag",
                              "notes": "Just a task",
                              "status": "needsAction",
                              "due": "2025-01-02T00:00:00Z"
                            },
                            {
                              "id": "task-3",
                              "title": "Other day",
                              "notes": "#mavigo: Nation",
                              "status": "needsAction",
                              "due": "2025-01-03T00:00:00Z"
                            }
                          ]
                        }
                        """;
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }

            if ("POST".equals(request.method().name()) && path.contains("/tasks")) {
                String json = "{\"id\":\"g-task-123\",\"title\":\"dummy\"}";
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
        this.userTaskRepository = mock(UserTaskRepository.class);
        this.primApiClient = mock(PrimApiClient.class);

        this.controller = new GoogleTasksController(
                googleApiWebClient,
                authorizedClientService,
                userService,
                userTaskRepository,
                primApiClient);
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
    void suggestionsForUser_returnsLocationTasksForDate() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@example.com", "Test");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-123");

        when(userService.getUser(userId)).thenReturn(user);
        when(authorizedClientService.loadAuthorizedClient("google", "sub-123"))
                .thenReturn(buildAuthorizedClient("sub-123"));
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());
        when(userTaskRepository.save(any(UserTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrimPlace place = new PrimPlace("place-1", "Chatelet", null, null, null, new PrimCoordinates(48.858, 2.347));
        when(primApiClient.searchPlaces(anyString())).thenReturn(List.of(place));

        LocalDate target = LocalDate.of(2025, 1, 2);
        List<Map<String, Object>> suggestions = controller.suggestionsForUser(userId, target);

        assertEquals(1, suggestions.size());
        assertEquals("Chatelet", suggestions.get(0).get("locationQuery"));
    }

    private OAuth2AuthorizedClient buildAuthorizedClient(String subject) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("sub")
                .clientName("google")
                .build();

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        return new OAuth2AuthorizedClient(registration, subject, token);
    }
}
