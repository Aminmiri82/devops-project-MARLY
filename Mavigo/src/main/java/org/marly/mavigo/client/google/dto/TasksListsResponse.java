package org.marly.mavigo.client.google.dto;

import java.util.List;

public record TasksListsResponse(List<TaskListDto> items, String nextPageToken) {
}