package org.marly.mavigo.service.journey.dto;

public record JourneyPreferences(
        boolean comfortModeEnabled,
        boolean touristicModeEnabled) {

    public static JourneyPreferences disabled() {
        return new JourneyPreferences(false, false);
    }
}

