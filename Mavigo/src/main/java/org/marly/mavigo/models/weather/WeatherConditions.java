package org.marly.mavigo.models.weather;

import java.time.OffsetDateTime;

public class WeatherConditions {

    private final double temperatureCelsius;
    private final double precipitationProbability;
    private final boolean precipitationExpected;
    private final OffsetDateTime observedAt;

    public WeatherConditions(double temperatureCelsius, double precipitationProbability, boolean precipitationExpected,
                             OffsetDateTime observedAt) {
        this.temperatureCelsius = temperatureCelsius;
        this.precipitationProbability = precipitationProbability;
        this.precipitationExpected = precipitationExpected;
        this.observedAt = observedAt;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getPrecipitationProbability() {
        return precipitationProbability;
    }

    public boolean isPrecipitationExpected() {
        return precipitationExpected;
    }

    public OffsetDateTime getObservedAt() {
        return observedAt;
    }
}

