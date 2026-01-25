package org.marly.mavigo.controller.dto;

public record JourneyPreferencesRequest(
                boolean comfortMode,
                boolean touristicMode,
                java.util.UUID namedComfortSettingId) {
}
