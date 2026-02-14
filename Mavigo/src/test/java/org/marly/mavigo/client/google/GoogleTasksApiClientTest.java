package org.marly.mavigo.client.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.google.dto.TaskListDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

class GoogleTasksApiClientTest {

    @Test
    void listTaskLists_returnsItemsAndUsesProvidedPagingParams() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            capturedUri.set(request.url());
            String json = """
                    {"items":[{"id":"list-1","title":"Inbox"}]}
                    """;
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build());
        };

        GoogleTasksApiClient client = new GoogleTasksApiClient(WebClient.builder().exchangeFunction(exchange).build());
        List<TaskListDto> lists = client.listTaskLists(25, "token-abc");

        assertEquals(1, lists.size());
        assertEquals("list-1", lists.get(0).id());
        assertTrue(capturedUri.get().toString().contains("maxResults=25"));
        assertTrue(capturedUri.get().toString().contains("pageToken=token-abc"));
    }

    @Test
    void listTaskLists_usesDefaultPageSizeWhenNull() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            capturedUri.set(request.url());
            String json = """
                    {"items":[]}
                    """;
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build());
        };

        GoogleTasksApiClient client = new GoogleTasksApiClient(WebClient.builder().exchangeFunction(exchange).build());
        client.listTaskLists(null, null);

        assertTrue(capturedUri.get().toString().contains("maxResults=50"));
    }

    @Test
    void listTaskLists_returnsEmptyWhenItemsMissingOrBodyEmpty() {
        ExchangeFunction emptyBodyExchange = request -> Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
        GoogleTasksApiClient clientNoBody = new GoogleTasksApiClient(
                WebClient.builder().exchangeFunction(emptyBodyExchange).build());
        assertTrue(clientNoBody.listTaskLists(10, null).isEmpty());

        ExchangeFunction noItemsExchange = request -> {
            String json = "{}";
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build());
        };
        GoogleTasksApiClient clientNoItems = new GoogleTasksApiClient(
                WebClient.builder().exchangeFunction(noItemsExchange).build());
        assertTrue(clientNoItems.listTaskLists(10, null).isEmpty());
    }
}
