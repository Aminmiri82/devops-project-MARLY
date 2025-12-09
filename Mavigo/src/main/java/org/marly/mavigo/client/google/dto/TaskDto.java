package org.marly.mavigo.client.google.dto;

public record TaskDto(
        String id,
        String title,
        String notes,
        String status,
        String due,
        String completed,
        String updated,
        String webViewLink,
        String parent,
        String position
) {}
