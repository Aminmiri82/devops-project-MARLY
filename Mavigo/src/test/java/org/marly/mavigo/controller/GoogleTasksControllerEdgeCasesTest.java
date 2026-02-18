package org.marly.mavigo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.user.UserService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class GoogleTasksControllerEdgeCasesTest {

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;
    @Mock
    private UserService userService;
    @Mock
    private UserTaskRepository userTaskRepository;
    @Mock
    private PrimApiClient primApiClient;
    @Mock
    private OAuth2AuthenticatedPrincipal principal;

    private GoogleTasksController controller;

    @BeforeEach
    void setUp() {
        ExchangeFunction exchange = request -> {
            String path = request.url().getPath();
            if ("GET".equals(request.method().name()) && "/users/@me/lists".equals(path)) {
                String json = """
                        {"items":[{"id":"list-1","title":"Default"}]}
                        """;
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            if ("GET".equals(request.method().name()) && path.contains("/lists/") && path.endsWith("/tasks")) {
                String json = """
                        {
                          "items": [
                            {"id":"t1","title":"Instant due","status":"needsAction","due":"2026-02-15T00:00:00Z"},
                            {"id":"t2","title":"Date due completed","status":"completed","due":"2026-02-15"},
                            {"id":"t3","title":"Invalid due","status":"needsAction","due":"not-a-date"},
                            {"id":"t4","title":"Date due open","status":"needsAction","due":"2026-02-15"}
                          ]
                        }
                        """;
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };

        WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();
        controller = new GoogleTasksController(
                webClient,
                authorizedClientService,
                userService,
                userTaskRepository,
                primApiClient);
    }

    @Test
    void listsForUser_throwsConflictWhenNoGoogleSubject() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext", "u@example.com", "User");
        user.setId(userId);
        user.setGoogleAccountSubject(null);
        when(userService.getUser(userId)).thenReturn(user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.listsForUser(userId, null, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void listsForUser_throwsUnauthorizedWhenAuthorizedClientMissing() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext", "u@example.com", "User");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-123");
        when(userService.getUser(userId)).thenReturn(user);
        when(authorizedClientService.loadAuthorizedClient("google", "sub-123")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.listsForUser(userId, null, null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void listsForUser_usesGoogleEmailFallbackWhenSubjectLookupMisses() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext", "u@example.com", "User");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-123");
        user.setGoogleAccountEmail("google@example.com");
        when(userService.getUser(userId)).thenReturn(user);
        when(authorizedClientService.loadAuthorizedClient("google", "sub-123")).thenReturn(null);
        when(authorizedClientService.loadAuthorizedClient("google", "google@example.com"))
                .thenReturn(buildAuthorizedClient("google@example.com"));

        List<?> lists = controller.listsForUser(userId, null, null);

        assertEquals(1, lists.size());
    }

    @Test
    void tasksForUser_filtersByDateAndExcludesCompletedWhenRequested() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext", "u@example.com", "User");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-123");
        when(userService.getUser(userId)).thenReturn(user);
        when(authorizedClientService.loadAuthorizedClient("google", "sub-123"))
                .thenReturn(buildAuthorizedClient("sub-123"));

        List<Map<String, Object>> tasks = controller.tasksForUser(userId, "list-1", LocalDate.of(2026, 2, 15), false);

        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> "t1".equals(t.get("id"))));
        assertTrue(tasks.stream().anyMatch(t -> "t4".equals(t.get("id"))));
    }

    @Test
    void token_throwsUnauthorizedWhenClientMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.token(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void linkGoogleAccount_throwsBadRequestWhenSubjectMissing() {
        UUID userId = UUID.randomUUID();
        when(principal.getAttribute("sub")).thenReturn(" ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.linkGoogleAccount(userId, principal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resolveGeoPointFromQuery_mapsPrimUnauthorizedToBadGateway() throws Exception {
        WebClientResponseException unauthorized = WebClientResponseException.create(
                401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        when(primApiClient.searchPlaces("query")).thenThrow(unauthorized);

        Method m = GoogleTasksController.class.getDeclaredMethod("resolveGeoPointFromQuery", String.class);
        m.setAccessible(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> {
                    try {
                        m.invoke(controller, "query");
                    } catch (Exception reflectionEx) {
                        throw (ResponseStatusException) reflectionEx.getCause();
                    }
                });

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    @Test
    void resolveGeoPointFromQuery_throwsBadRequestWhenNoUsableCoordinates() throws Exception {
        when(primApiClient.searchPlaces("query")).thenReturn(List.of(new PrimPlace("id", "NoCoord", "stop_area", null, null, null)));

        Method m = GoogleTasksController.class.getDeclaredMethod("resolveGeoPointFromQuery", String.class);
        m.setAccessible(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> {
                    try {
                        m.invoke(controller, "query");
                    } catch (Exception reflectionEx) {
                        throw (ResponseStatusException) reflectionEx.getCause();
                    }
                });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private OAuth2AuthorizedClient buildAuthorizedClient(String subject) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/user")
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
