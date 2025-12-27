package org.marly.mavigo.client.google;

import java.util.List;

import org.marly.mavigo.client.google.dto.TaskListDto;
import org.marly.mavigo.client.google.dto.TasksListsResponse;
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
                        .queryParamIfPresent("pageToken", java.util.Optional.ofNullable(pageToken))
                        .build())
                .retrieve()
                .bodyToMono(TasksListsResponse.class)
                .block();

        return resp == null || resp.items() == null ? List.of() : resp.items();
    }
}