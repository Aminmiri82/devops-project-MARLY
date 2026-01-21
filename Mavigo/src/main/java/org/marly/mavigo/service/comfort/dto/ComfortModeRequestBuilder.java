package org.marly.mavigo.service.comfort.dto;

import java.time.OffsetDateTime;

import org.marly.mavigo.models.user.User;
import org.marly.mavigo.models.weather.WeatherConditions;

public record ComfortModeRequestBuilder(
        String originStopId,
        String destinationStopId,
        OffsetDateTime departureTime,
        User user,
        WeatherConditions weather){
}
