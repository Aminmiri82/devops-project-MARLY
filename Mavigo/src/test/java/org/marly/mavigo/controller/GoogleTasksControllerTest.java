package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
}
