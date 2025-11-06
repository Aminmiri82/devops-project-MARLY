package org.marly.mavigo.service.suggestion.dto;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.user.User;

public record TaskSuggestionContext(User user, Journey journey) {
}

