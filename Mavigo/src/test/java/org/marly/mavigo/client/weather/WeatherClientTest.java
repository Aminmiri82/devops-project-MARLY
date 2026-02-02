package org.marly.mavigo.client.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.weather.WeatherConditions;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@DisplayName("Tests unitaires - WeatherClient")
class WeatherClientTest {

    private RestTemplate restTemplate;
    private WeatherClientImpl weatherClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        weatherClient = new WeatherClientImpl(restTemplate, "test-api-key");
    }

    @Test
    @DisplayName("fetchCurrentConditions devrait retourner les conditions météo")
    void fetchCurrentConditions_shouldReturnWeatherConditions() {
        // Given
        GeoPoint location = new GeoPoint(48.8443, 2.3730);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(WeatherApiResponse.class)))
                .thenReturn(new ResponseEntity<>(createMockApiResponse(), HttpStatus.OK));

        // When
        WeatherConditions result = weatherClient.fetchCurrentConditions(location);

        // Then
        assertNotNull(result);
        assertEquals(22.5, result.getTemperatureCelsius());
    }

    @Test
    @DisplayName("fetchCurrentConditions devrait lever une exception si la localisation est nulle")
    void fetchCurrentConditions_shouldThrowExceptionWhenLocationIsNull() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> weatherClient.fetchCurrentConditions(null));
    }

    @Test
    @DisplayName("fetchCurrentConditions devrait lever une exception si les coordonnées sont incomplètes")
    void fetchCurrentConditions_shouldThrowExceptionWhenCoordinatesIncomplete() {
        // Given
        GeoPoint incompleteLocation = new GeoPoint(null, 2.3730);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> weatherClient.fetchCurrentConditions(incompleteLocation));
    }

    @Test
    @DisplayName("fetchCurrentConditions devrait gérer les erreurs API")
    void fetchCurrentConditions_shouldHandleApiErrors() {
        // Given
        GeoPoint location = new GeoPoint(48.8443, 2.3730);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(WeatherApiResponse.class)))
                .thenThrow(new RestClientException("API error"));

        // When/Then
        assertThrows(WeatherApiException.class, () -> weatherClient.fetchCurrentConditions(location));
    }

    @Test
    @DisplayName("fetchCurrentConditions devrait gérer une réponse nulle")
    void fetchCurrentConditions_shouldHandleNullResponse() {
        // Given
        GeoPoint location = new GeoPoint(48.8443, 2.3730);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(WeatherApiResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When/Then
        assertThrows(WeatherApiException.class, () -> weatherClient.fetchCurrentConditions(location));
    }

    // Helper methods

    private WeatherApiResponse createMockApiResponse() {
        return new WeatherApiResponse(
                new WeatherApiResponse.Current(
                        22.5,
                        0.3,
                        true));
    }
}

/**
 * Implementation for WeatherClient to test against.
 */
class WeatherClientImpl implements WeatherClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private static final String API_BASE_URL = "https://api.weather.example.com";

    public WeatherClientImpl(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    @Override
    public WeatherConditions fetchCurrentConditions(GeoPoint location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (!location.isComplete()) {
            throw new IllegalArgumentException("Location coordinates must be complete");
        }

        try {
            String url = String.format("%s/current?lat=%f&lon=%f&apikey=%s",
                    API_BASE_URL, location.getLatitude(), location.getLongitude(), apiKey);

            ResponseEntity<WeatherApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    WeatherApiResponse.class);

            WeatherApiResponse apiResponse = response.getBody();
            if (apiResponse == null || apiResponse.current() == null) {
                throw new WeatherApiException("Empty response from weather API");
            }

            WeatherApiResponse.Current current = apiResponse.current();
            return new WeatherConditions(
                    current.temperature(),
                    current.precipitationProbability(),
                    current.precipitationExpected(),
                    OffsetDateTime.now());

        } catch (RestClientException e) {
            throw new WeatherApiException("Failed to fetch weather conditions: " + e.getMessage(), e);
        }
    }
}

/**
 * Weather API response DTO.
 */
record WeatherApiResponse(Current current) {
    record Current(double temperature, double precipitationProbability, boolean precipitationExpected) {
    }
}

/**
 * Weather API exception.
 */
class WeatherApiException extends RuntimeException {
    public WeatherApiException(String message) {
        super(message);
    }

    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
