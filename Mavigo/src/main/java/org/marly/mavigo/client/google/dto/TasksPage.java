package org.marly.mavigo.client.google.dto;

import java.util.List;

public record TasksPage(List<TaskDto> items, String nextPageToken) {}
