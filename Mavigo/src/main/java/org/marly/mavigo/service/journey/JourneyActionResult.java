package org.marly.mavigo.service.journey;

import java.util.List;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.tracking.Badge;

public record JourneyActionResult(Journey journey, List<Badge> newBadges) {
}
