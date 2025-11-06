package org.marly.mavigo.client.weather;

import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.weather.WeatherConditions;

public interface WeatherClient {

    WeatherConditions fetchCurrentConditions(GeoPoint location);
}

