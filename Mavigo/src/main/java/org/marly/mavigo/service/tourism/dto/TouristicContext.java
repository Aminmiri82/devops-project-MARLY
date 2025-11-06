package org.marly.mavigo.service.tourism.dto;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.user.User;

public record TouristicContext(User user, Journey journey, int maxResults) {
}

