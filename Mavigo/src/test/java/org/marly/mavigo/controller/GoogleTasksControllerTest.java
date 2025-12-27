package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimPlace;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.user.UserService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleTasksControllerTest {

    private WebClient googleApiWebClient;
    private UserService userService;
    private UserRepository userRepository;
    private UserTaskRepository userTaskRepository;
    private PrimApiClient primApiClient;

    private GoogleTasksController controller;

    @BeforeEach
    void setup() {
        // WebClient fake: simule Google Tasks POST /lists/{id}/tasks => {"id":"g-task-123"}
        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();

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

        this.userService = mock(UserService.class);
        this.userRepository = mock(UserRepository.class);
        this.userTaskRepository = mock(UserTaskRepository.class);
        this.primApiClient = mock(PrimApiClient.class);

        this.controller = new GoogleTasksController(
                googleApiWebClient,
                userService,
                userRepository,
                userTaskRepository,
                primApiClient
        );
    }

    private OAuth2AuthorizedClient dummyAuthorizedClient() {
        ClientRegistration reg = ClientRegistration.withRegistrationId("google")
                .clientId("x")
                .clientSecret("y")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .authorizationUri("http://localhost/auth")
                .tokenUri("http://localhost/token")
                .userInfoUri("http://localhost/userinfo")
                .userNameAttributeName("sub")
                .scope("openid", "profile", "email", "https://www.googleapis.com/auth/tasks")
                .clientName("google")
                .build();

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "dummy-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2AuthorizedClient(reg, "principalName", token);
    }

    /**
     * Spring Security 6 / Boot 3: on utilise DefaultOAuth2User (et non DefaultOAuth2AuthenticatedPrincipal)
     */
    private OAuth2AuthenticatedPrincipal principalWithSub(String sub) {
        Map<String, Object> attrs = Map.of(
                "sub", sub,
                "email", "test@example.com",
                "name", "Test"
        );

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "sub"
        );
    }

    @Test
    void createTask_shouldFail_whenLocationMissing() {
        var req = new GoogleTasksController.CreateTaskRequest("t", null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.createTask(dummyAuthorizedClient(), principalWithSub("sub1"), "list1", req)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("locationQuery"));
    }

    @Test
    void createTask_shouldFail_whenGoogleNotLinkedToLocalUser() {
        when(userRepository.findByGoogleAccountSubject("sub1")).thenReturn(java.util.Optional.empty());

        var req = new GoogleTasksController.CreateTaskRequest("t", null, null, "Gare de Lyon");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.createTask(dummyAuthorizedClient(), principalWithSub("sub1"), "list1", req)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createTask_shouldFail_whenGeocodingReturnsNothing() {
        // User() a un constructeur protected => on mock
        User u = Mockito.mock(User.class);
        when(userRepository.findByGoogleAccountSubject("sub1")).thenReturn(java.util.Optional.of(u));
        when(primApiClient.searchPlaces("Nowhere")).thenReturn(List.of());

        var req = new GoogleTasksController.CreateTaskRequest("t", null, null, "Nowhere");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.createTask(dummyAuthorizedClient(), principalWithSub("sub1"), "list1", req)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("No location found"));
    }

    @Test
    void createTask_shouldCreateGoogleTask_andPersistUserTask_withCoordinates() {
        // user local lié (mock car constructeur protected)
        User u = Mockito.mock(User.class);
        when(userRepository.findByGoogleAccountSubject("sub1")).thenReturn(java.util.Optional.of(u));

        // geocoding prim (mock deep)
        PrimPlace place = Mockito.mock(PrimPlace.class, Mockito.RETURNS_DEEP_STUBS);
        when(place.stopArea().coordinates().latitude()).thenReturn(48.8443);
        when(place.stopArea().coordinates().longitude()).thenReturn(2.3730);
        when(primApiClient.searchPlaces("Gare de Lyon")).thenReturn(List.of(place));

        // repo save: on simule un id pour que localTaskId soit non null
        when(userTaskRepository.save(any(UserTask.class))).thenAnswer(inv -> {
            UserTask t = inv.getArgument(0);
            try {
                var idField = UserTask.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(t, UUID.randomUUID());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return t;
        });

        var req = new GoogleTasksController.CreateTaskRequest(
                "Acheter du lait",
                "Sur le chemin",
                null,
                "Gare de Lyon"
        );

        Map<String, Object> res = controller.createTask(
                dummyAuthorizedClient(),
                principalWithSub("sub1"),
                "list1",
                req
        );

        // réponse enrichie
        assertEquals("g-task-123", String.valueOf(res.get("id")));
        assertNotNull(res.get("localTaskId"));
        assertEquals(48.8443, (Double) res.get("locationLat"), 0.0001);
        assertEquals(2.3730, (Double) res.get("locationLng"), 0.0001);

        // vérifie ce qui est persisté
        ArgumentCaptor<UserTask> captor = ArgumentCaptor.forClass(UserTask.class);
        verify(userTaskRepository).save(captor.capture());

        UserTask saved = captor.getValue();
        assertEquals(TaskSource.GOOGLE_TASKS, saved.getSource());
        assertEquals("Acheter du lait", saved.getTitle());
        assertNotNull(saved.getLocationHint());
        assertEquals(48.8443, saved.getLocationHint().getLatitude(), 0.0001);
        assertEquals(2.3730, saved.getLocationHint().getLongitude(), 0.0001);
    }
}