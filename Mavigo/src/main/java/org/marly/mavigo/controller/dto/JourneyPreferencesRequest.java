package org.marly.mavigo.controller.dto;

public record JourneyPreferencesRequest(
                boolean comfortMode,
                java.util.UUID namedComfortSettingId) {
}
