package org.marly.mavigo.service.journey.dto;

public record JourneyPreferences(
        boolean comfortModeEnabled,
        boolean touristicModeEnabled,
        boolean wheelchairAccessible) {

    public JourneyPreferences(boolean comfortModeEnabled, boolean touristicModeEnabled) {
        this(comfortModeEnabled, touristicModeEnabled, false);
    }

    public static JourneyPreferences disabled() {
        return new JourneyPreferences(false, false, false);
    }
}