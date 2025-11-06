package org.marly.mavigo.service.suggestion.dto;

import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.task.UserTask;

public record TaskSuggestion(UserTask task, Leg matchingLeg, String message) {
}

