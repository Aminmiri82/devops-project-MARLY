package org.marly.mavigo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.client.google.dto.TaskListDto;
import org.marly.mavigo.client.prim.PrimApiClient;
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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class GoogleTasksControllerSupplementalTest {

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;
    @Mock
    private UserService userService;
    @Mock
    private UserTaskRepository userTaskRepository;
    @Mock
    private PrimApiClient primApiClient;

    private OAuth2AuthorizedClient authorizedClient;

    @BeforeEach
    void setUp() {
        authorizedClient = buildAuthorizedClient("sub-1");
    }

    @Test
    void meEndpoints_returnListsAndTasks() {
        GoogleTasksController controller = controllerFor(exchangeWithListsAndTasks());

        List<TaskListDto> lists = controller.listsForMe(authorizedClient, 10, null);
        List<TaskDto> tasks = controller.tasksForMe(authorizedClient, "list-1", null, true);

        assertThat(lists).hasSize(1);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).id()).isEqualTo("task-1");
    }

    @Test
    void completeTaskForUser_returnsOkWhenPatchResponseBodyIsEmpty() {
        GoogleTasksController controller = controllerFor(exchangeForEmptyPatchBody());
        UUID userId = UUID.randomUUID();
        User user = new User("ext", "u@example.com", "User");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-1");

        when(userService.getUser(userId)).thenReturn(user);
        when(authorizedClientService.loadAuthorizedClient("google", "sub-1")).thenReturn(authorizedClient);

        Map<String, Object> response = controller.completeTaskForUser(userId, "list-1", "task-1");

        assertThat(response).containsEntry("ok", true);
    }

    @Test
    void defaultListForUser_throwsNotFoundWhenGoogleReturnsNoLists() {
        GoogleTasksController controller = controllerFor(exchangeWithNoLists());
        UUID userId = UUID.randomUUID();
        User user = new User("ext", "u@example.com", "User");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-1");

        when(userService.getUser(userId)).thenReturn(user);
        when(authorizedClientService.loadAuthorizedClient("google", "sub-1")).thenReturn(authorizedClient);

        assertThatThrownBy(() -> controller.defaultListForUser(userId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resolveGeoPointFromQuery_wrapsNon401ErrorsAsBadGateway() throws Exception {
        GoogleTasksController controller = controllerFor(exchangeWithNoLists());
        WebClientResponseException primError = WebClientResponseException.create(
                503, "Downstream", HttpHeaders.EMPTY, "broken".getBytes(), null);
        when(primApiClient.searchPlaces("test")).thenThrow(primError);

        Method m = GoogleTasksController.class.getDeclaredMethod("resolveGeoPointFromQuery", String.class);
        m.setAccessible(true);

        Exception reflectionEx = assertThrows(Exception.class, () -> m.invoke(controller, "test"));
        assertThat(reflectionEx.getCause()).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException rse = (ResponseStatusException) reflectionEx.getCause();
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(rse.getReason()).contains("PRIM error:");
    }

    private GoogleTasksController controllerFor(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new GoogleTasksController(webClient, authorizedClientService, userService, userTaskRepository, primApiClient);
    }

    private ExchangeFunction exchangeWithListsAndTasks() {
        return request -> {
            String path = request.url().getPath();
            if ("GET".equals(request.method().name()) && "/users/@me/lists".equals(path)) {
                String json = "{\"items\":[{\"id\":\"list-1\",\"title\":\"Default\"}]}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            if ("GET".equals(request.method().name()) && path.endsWith("/tasks")) {
                String json = "{\"items\":[{\"id\":\"task-1\",\"title\":\"Task\",\"status\":\"needsAction\"}]}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };
    }

    private ExchangeFunction exchangeWithNoLists() {
        return request -> {
            String path = request.url().getPath();
            if ("GET".equals(request.method().name()) && "/users/@me/lists".equals(path)) {
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"items\":[]}")
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };
    }

    private ExchangeFunction exchangeForEmptyPatchBody() {
        return request -> {
            String path = request.url().getPath();
            if ("PATCH".equals(request.method().name()) && path.contains("/lists/") && path.contains("/tasks/")) {
                return Mono.just(ClientResponse.create(HttpStatus.OK).build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };
    }

    private OAuth2AuthorizedClient buildAuthorizedClient(String subject) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
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
