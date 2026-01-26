package org.marly.mavigo.service.journey.dto;

public record JourneyPreferences(
        boolean comfortModeEnabled,
        java.util.UUID namedComfortSettingId) {

    public static JourneyPreferences disabled() {
        return new JourneyPreferences(false, null);
    }
}
