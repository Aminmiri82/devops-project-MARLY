package org.marly.mavigo.client.prim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrimApiClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private PrimApiClientImpl client;
    private final String apiEndpoint = "https://example.com";
    private final String apiKey = "test-key";

    @BeforeEach
    void setUp() {
        client = new PrimApiClientImpl(restTemplate, apiEndpoint, apiKey, "Europe/Paris");
    }

    @Test
    @DisplayName("searchPlaces: returns places when API succeeds")
    void searchPlaces_shouldReturnPlaces() {
        PrimPlace place = new PrimPlace("id", "name", "type", null, null, null);
        PrimPlacesResponse response = new PrimPlacesResponse(List.of(place));
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimPlace> result = client.searchPlaces("query");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("name", result.get(0).name());
    }

    @Test
    @DisplayName("searchPlaces: throws exception on null/empty query")
    void searchPlaces_shouldThrowExceptionOnNullQuery() {
        assertThrows(IllegalArgumentException.class, () -> client.searchPlaces(null));
        assertThrows(IllegalArgumentException.class, () -> client.searchPlaces(""));
    }

    @Test
    @DisplayName("searchPlaces: wraps RestClientException")
    void searchPlaces_shouldWrapException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenThrow(new RestClientException("API Error"));

        assertThrows(PrimApiException.class, () -> client.searchPlaces("query"));
    }

    @Test
    @DisplayName("searchPlacesNearby (City): filters by distance and sorts")
    void searchPlacesNearby_withCityName_shouldFilterAndSort() {
        // Center: Paris (48.8566, 2.3522)
        PrimCoordinates center = new PrimCoordinates(48.8566, 2.3522);
        
        // Place 1: Very close (Notre Dame)
        PrimCoordinates c1 = new PrimCoordinates(48.8530, 2.3499);
        PrimStopArea sa1 = new PrimStopArea("id1", "Notre Dame", c1);
        PrimPlace p1 = new PrimPlace("id1", "Notre Dame", "stop_area", sa1, null, c1);

        // Place 2: Far away (Lyon)
        PrimCoordinates c2 = new PrimCoordinates(45.7640, 4.8357);
        PrimStopArea sa2 = new PrimStopArea("id2", "Lyon", c2);
        PrimPlace p2 = new PrimPlace("id2", "Lyon", "stop_area", sa2, null, c2);

        // Place 3: Medium distance (Tour Eiffel - ~4km)
        PrimCoordinates c3 = new PrimCoordinates(48.8584, 2.2945);
        PrimStopArea sa3 = new PrimStopArea("id3", "Eiffel", c3);
        PrimPlace p3 = new PrimPlace("id3", "Eiffel", "stop_area", sa3, null, c3);

        PrimPlacesResponse response = new PrimPlacesResponse(List.of(p2, p1, p3)); // Unsorted from API
        
        when(restTemplate.exchange(contains("q=Paris"), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // Radius 4500m (< 5000) should exclude Lyon (p2) but keep Notre Dame (p1) and Eiffel (p3)
        List<PrimPlace> result = client.searchPlacesNearby(48.8566, 2.3522, 4500, "Paris");

        assertEquals(2, result.size());
        assertEquals("Notre Dame", result.get(0).name()); // Closest first
        assertEquals("Eiffel", result.get(1).name());
    }

    @Test
    @DisplayName("searchPlacesNearby (Coords): uses coord query and filters")
    void searchPlacesNearby_withCoordinates_shouldUseCoordQuery() {
        PrimCoordinates c1 = new PrimCoordinates(48.8566, 2.3522);
        PrimStopArea sa1 = new PrimStopArea("id1", "Center", c1);
        PrimPlace p1 = new PrimPlace("id1", "Center", "stop_area", sa1, null, c1);
        
        PrimPlacesResponse response = new PrimPlacesResponse(List.of(p1));

        // Expect query to contain "coord:2.352200;48.856600"
        when(restTemplate.exchange(matches(".*coord%3A2.352200%3B48.856600.*"), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimPlace> result = client.searchPlacesNearby(48.8566, 2.3522, 1000);

        assertEquals(1, result.size());
        assertEquals("Center", result.get(0).name());
    }

    @Test
    @DisplayName("calculateJourneyPlans: builds correct URI with all params")
    void calculateJourneyPlans_shouldBuildCorrectUri() {
        LocalDateTime now = LocalDateTime.of(2023, 10, 27, 10, 0);
        PrimJourneyRequest request = new PrimJourneyRequest("from-id", "to-id", now);
        request.withMaxDuration(3600);
        request.withMaxNbTransfers(2);
        request.withWheelchair(true);
        request.withDirectPath("preferred");

         PrimJourneyResponse response = new PrimJourneyResponse(Collections.emptyList());
         when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                 .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

         client.calculateJourneyPlans(request);

         ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
         verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class));

         String url = urlCaptor.getValue();
         assertTrue(url.contains("from=from-id"));
         assertTrue(url.contains("to=to-id"));
         assertTrue(url.contains("datetime=20231027T100000"));
         assertTrue(url.contains("max_duration=3600"));
         assertTrue(url.contains("max_nb_transfers=2"));
         assertTrue(url.contains("wheelchair=true"));
         assertTrue(url.contains("direct_path=preferred"));
    }

    @Test
    @DisplayName("calculateJourneyPlans: handles no_origin error with retry")
    void calculateJourneyPlans_shouldRetryOnNoOrigin() {
        PrimJourneyRequest request = new PrimJourneyRequest("from-id", "to-id", LocalDateTime.now());

        // First call fails with no_origin
        String errorBody = "{\"error\": {\"id\":\"no_origin\"}}";
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", errorBody.getBytes(StandardCharsets.UTF_8), null);

        // Second call succeeds
        PrimJourneyResponse response = new PrimJourneyResponse(Collections.emptyList());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenThrow(exception)
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(request);

        assertNotNull(result);
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class));
    }

    @Test
    @DisplayName("calculateJourneyPlans: maps response to DTOs")
    void calculateJourneyPlans_shouldMapResponse() {
        LocalDateTime dep = LocalDateTime.of(2023, 10, 27, 10, 0);
        LocalDateTime arr = LocalDateTime.of(2023, 10, 27, 11, 0);
        
        // Corrected constructor usage
        PrimStopPoint sp1 = new PrimStopPoint("sp1", "Station A", null, null);
        PrimStopPoint sp2 = new PrimStopPoint("sp2", "Station B", null, null);
        
        PrimSection section = new PrimSection(
            "section-id", "public_transport", 3600, dep, arr, 
            sp1, sp2, null, null
        );
                          
        PrimJourney journey = new PrimJourney("j1", 3600, 0, dep, arr, List.of(section));
        PrimJourneyResponse response = new PrimJourneyResponse(List.of(journey));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        PrimJourneyRequest request = new PrimJourneyRequest("A", "B", dep);
        List<PrimJourneyPlanDto> plans = client.calculateJourneyPlans(request);

        assertEquals(1, plans.size());
        assertEquals("j1", plans.get(0).journeyId());
        assertEquals(1, plans.get(0).legs().size());
        assertEquals("Station A", plans.get(0).legs().get(0).originLabel());
    }
}
