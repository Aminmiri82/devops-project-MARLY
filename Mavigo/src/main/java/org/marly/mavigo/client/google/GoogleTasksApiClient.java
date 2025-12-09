package org.marly.mavigo.client.google;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GoogleTasksApiClient {

    private final WebClient web;

    public GoogleTasksApiClient(WebClient googleApiWebClient) {
        this.web = googleApiWebClient;
    }

    public List<TaskListDto> listTaskLists(Integer pageSize, String pageToken) {
        var resp = web.get()
                .uri(uri -> uri.path("/users/@me/lists")
                        .queryParam("maxResults", pageSize == null ? 50 : pageSize)
                        .queryParamIfPresent("pageToken", pageToken == null ? null : java.util.Optional.of(pageToken))
                        .build())
                .retrieve()
                .bodyToMono(TaskListsResponse.class)
                .block();

        return resp == null || resp.items == null ? List.of() : resp.items;
    }

    // --- DTOs minimalistes ---

    public record TaskListDto(String id, String title) {}

    public record TaskListsResponse(List<TaskListDto> items, String nextPageToken) {}
}
