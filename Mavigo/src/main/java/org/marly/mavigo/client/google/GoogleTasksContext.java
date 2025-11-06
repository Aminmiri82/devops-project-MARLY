package org.marly.mavigo.client.google;

public record GoogleTasksContext(
        String accessToken,
        String refreshToken,
        String taskListId) {
}

