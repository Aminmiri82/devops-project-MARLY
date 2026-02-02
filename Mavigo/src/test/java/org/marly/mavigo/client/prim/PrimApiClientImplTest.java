package org.marly.mavigo.client.prim;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimJourney;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.model.PrimJourneyResponse;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimPlacesResponse;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@DisplayName("Tests unitaires - PrimApiClientImpl")
class PrimApiClientImplTest {

    private RestTemplate restTemplate;
    private PrimApiClientImpl primApiClient;

    private static final String API_ENDPOINT = "https://prim.test.com/api";
    private static final String API_KEY = "test-api-key";
    private static final String TIMEZONE = "Europe/Paris";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        primApiClient = new PrimApiClientImpl(restTemplate, API_ENDPOINT, API_KEY, TIMEZONE);
    }

    // --- searchPlaces tests ---

    @Test
    @DisplayName("searchPlaces devrait retourner une liste de places")
    void searchPlaces_shouldReturnPlaces() {
        // Given
        String query = "Gare de Lyon";
        PrimPlace mockPlace = createMockPlace("stop:123", "Gare de Lyon");
        PrimPlacesResponse mockResponse = new PrimPlacesResponse(List.of(mockPlace));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<PrimPlace> result = primApiClient.searchPlaces(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gare de Lyon", result.get(0).name());
    }

    @Test
    @DisplayName("searchPlaces devrait lever une exception si la requête est nulle")
    void searchPlaces_shouldThrowExceptionWhenQueryIsNull() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> primApiClient.searchPlaces(null));
    }

    @Test
    @DisplayName("searchPlaces devrait lever une exception si la requête est vide")
    void searchPlaces_shouldThrowExceptionWhenQueryIsEmpty() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> primApiClient.searchPlaces(""));
        assertThrows(IllegalArgumentException.class, () -> primApiClient.searchPlaces("   "));
    }

    @Test
    @DisplayName("searchPlaces devrait retourner une liste vide si la réponse est nulle")
    void searchPlaces_shouldReturnEmptyListWhenResponseIsNull() {
        // Given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When
        List<PrimPlace> result = primApiClient.searchPlaces("test");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("searchPlaces devrait lever une PrimApiException en cas d'erreur REST")
    void searchPlaces_shouldThrowPrimApiExceptionOnRestError() {
        // Given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // When/Then
        assertThrows(PrimApiException.class, () -> primApiClient.searchPlaces("test"));
    }

    // --- searchPlacesNearby tests ---

    @Test
    @DisplayName("searchPlacesNearby devrait rechercher des places à proximité")
    void searchPlacesNearby_shouldSearchNearbyPlaces() {
        // Given
        double latitude = 48.8443;
        double longitude = 2.3730;
        int radius = 1000;

        PrimPlace mockPlace = createMockPlaceWithCoordinates("stop:123", "Station Proche", 48.8450, 2.3735);
        PrimPlacesResponse mockResponse = new PrimPlacesResponse(List.of(mockPlace));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<PrimPlace> result = primApiClient.searchPlacesNearby(latitude, longitude, radius);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("searchPlacesNearby avec nom de ville devrait filtrer les résultats")
    void searchPlacesNearby_withCityName_shouldFilterResults() {
        // Given
        double latitude = 48.8443;
        double longitude = 2.3730;
        int radius = 5000;
        String cityName = "Paris";

        PrimPlace mockPlace = createMockPlaceWithCoordinates("stop:123", "Gare de Lyon", 48.8443, 2.3730);
        PrimPlacesResponse mockResponse = new PrimPlacesResponse(List.of(mockPlace));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<PrimPlace> result = primApiClient.searchPlacesNearby(latitude, longitude, radius, cityName);

        // Then
        assertNotNull(result);
    }

    // --- calculateJourneyPlans tests ---

    @Test
    @DisplayName("calculateJourneyPlans devrait retourner des plans de trajet")
    void calculateJourneyPlans_shouldReturnJourneyPlans() {
        // Given
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:origin",
                "stop:destination",
                LocalDateTime.now());

        PrimJourney mockJourney = createMockPrimJourney("journey-1");
        PrimJourneyResponse mockResponse = new PrimJourneyResponse(List.of(mockJourney));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<PrimJourneyPlanDto> result = primApiClient.calculateJourneyPlans(request);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("calculateJourneyPlans devrait lever une exception si la requête est nulle")
    void calculateJourneyPlans_shouldThrowExceptionWhenRequestIsNull() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> primApiClient.calculateJourneyPlans(null));
    }

    @Test
    @DisplayName("calculateJourneyPlans devrait lever une exception si fromStopAreaId est nul")
    void calculateJourneyPlans_shouldThrowExceptionWhenFromStopAreaIdIsNull() {
        // Given
        PrimJourneyRequest request = new PrimJourneyRequest(null, "stop:dest", LocalDateTime.now());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> primApiClient.calculateJourneyPlans(request));
    }

    @Test
    @DisplayName("calculateJourneyPlans devrait lever une exception si toStopAreaId est nul")
    void calculateJourneyPlans_shouldThrowExceptionWhenToStopAreaIdIsNull() {
        // Given
        PrimJourneyRequest request = new PrimJourneyRequest("stop:origin", null, LocalDateTime.now());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> primApiClient.calculateJourneyPlans(request));
    }

    @Test
    @DisplayName("calculateJourneyPlans devrait lever une exception si datetime est nul")
    void calculateJourneyPlans_shouldThrowExceptionWhenDatetimeIsNull() {
        // Given
        PrimJourneyRequest request = new PrimJourneyRequest("stop:origin", "stop:dest", null);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> primApiClient.calculateJourneyPlans(request));
    }

    @Test
    @DisplayName("calculateJourneyPlans devrait retourner une liste vide si aucun trajet trouvé")
    void calculateJourneyPlans_shouldReturnEmptyListWhenNoJourneysFound() {
        // Given
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:origin",
                "stop:destination",
                LocalDateTime.now());

        PrimJourneyResponse mockResponse = new PrimJourneyResponse(List.of());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<PrimJourneyPlanDto> result = primApiClient.calculateJourneyPlans(request);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("calculateJourneyPlans devrait lever une PrimApiException en cas d'erreur REST")
    void calculateJourneyPlans_shouldThrowPrimApiExceptionOnRestError() {
        // Given
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:origin",
                "stop:destination",
                LocalDateTime.now());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimJourneyResponse.class)))
                .thenThrow(new RestClientException("API error"));

        // When/Then
        assertThrows(PrimApiException.class, () -> primApiClient.calculateJourneyPlans(request));
    }

    // Helper methods

    private PrimPlace createMockPlace(String id, String name) {
        PrimStopArea stopArea = new PrimStopArea(id, name, null);
        return new PrimPlace(id, name, "stop_area", stopArea, null, null);
    }

    private PrimPlace createMockPlaceWithCoordinates(String id, String name, double lat, double lon) {
        PrimCoordinates coords = new PrimCoordinates(lat, lon);
        PrimStopArea stopArea = new PrimStopArea(id, name, coords);
        return new PrimPlace(id, name, "stop_area", stopArea, null, coords);
    }

    private PrimJourney createMockPrimJourney(String id) {
        return new PrimJourney(
                id,
                3600,
                1,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                List.of());
    }
}
