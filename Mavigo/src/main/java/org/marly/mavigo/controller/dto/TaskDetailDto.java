package org.marly.mavigo.controller.dto;

/**
 * DTO pour une tâche issue de Google Tasks, envoyée par le frontend
 * pour l'optimisation de trajet (sans stockage en base).
 */
public record TaskDetailDto(
        String id,
        String title,
        String locationQuery,
        Double lat,
        Double lng,
        boolean completed) {
}
