package org.marly.mavigo.client.prim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimDisplayInformations;
import org.marly.mavigo.client.prim.model.PrimJourney;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.model.PrimJourneyResponse;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimPlacesResponse;
import org.marly.mavigo.client.prim.model.PrimSection;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.client.prim.model.PrimStopDateTime;
import org.marly.mavigo.client.prim.model.PrimStopPoint;
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

@ExtendWith(MockitoExtension.class)
class PrimApiClientImplAdvancedTest {

    @Mock
    private RestTemplate restTemplate;

    private PrimApiClientImpl client;

    @BeforeEach
    void setUp() {
        client = new PrimApiClientImpl(restTemplate, "https://example.com", "api-key-123", "Europe/Paris");
    }

    @Test
    void searchPlacesNearby_fallsBackToCoordQueryWhenCityCallFails() {
        PrimCoordinates c = new PrimCoordinates(48.8566, 2.3522);
        PrimPlace p = new PrimPlace("id-1", "Center", "stop_area", new PrimStopArea("id-1", "Center", c), null, c);

        when(restTemplate.exchange(contains("q=Paris"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenThrow(new RestClientException("city call failed"));
        when(restTemplate.exchange(contains("coord%3A2.352200%3B48.856600"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimPlacesResponse(List.of(p))));

        List<PrimPlace> result = client.searchPlacesNearby(48.8566, 2.3522, 1000, "Paris");

        assertEquals(1, result.size());
        assertEquals("Center", result.get(0).name());
    }

    @Test
    void searchPlacesNearby_withRadiusAtLeastFiveKmKeepsPlaceWithoutCoordinates() {
        PrimPlace withoutCoords = new PrimPlace("id-2", "NoCoord", "stop_area", new PrimStopArea("id-2", "NoCoord", null), null, null);

        when(restTemplate.exchange(contains("q=Paris"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimPlacesResponse(List.of(withoutCoords))));

        List<PrimPlace> result = client.searchPlacesNearby(48.8566, 2.3522, 5000, "Paris");

        assertEquals(1, result.size());
        assertEquals("id-2", result.get(0).id());
    }

    @Test
    void searchPlacesNearby_withSmallRadiusExcludesPlacesWithoutCoordinates() {
        PrimPlace withoutCoords = new PrimPlace("id-3", "NoCoord", "stop_area", new PrimStopArea("id-3", "NoCoord", null), null, null);

        when(restTemplate.exchange(contains("q=Paris"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimPlacesResponse(List.of(withoutCoords))));
        when(restTemplate.exchange(contains("coord%3A2.352200%3B48.856600"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimPlacesResponse(List.of())));

        List<PrimPlace> result = client.searchPlacesNearby(48.8566, 2.3522, 4999, "Paris");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchPlaces_includesApiKeyHeader() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimPlacesResponse(List.of())));

        client.searchPlaces("Gare de Lyon");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), captor.capture(), eq(PrimPlacesResponse.class));
        assertEquals("api-key-123", captor.getValue().getHeaders().getFirst("apikey"));
    }

    @Test
    void calculateJourneyPlans_throwsFallbackExceptionWhenRetryFails() {
        PrimJourneyRequest request = new PrimJourneyRequest("from", "to", LocalDateTime.now());
        String noOriginBody = "{\"error\":{\"id\":\"no_origin\"}}";

        HttpClientErrorException firstError = new HttpClientErrorException(
                HttpStatus.BAD_REQUEST, "Bad Request", noOriginBody.getBytes(StandardCharsets.UTF_8), null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenThrow(firstError)
                .thenThrow(new RestClientException("still failing"));

        PrimApiException ex = assertThrows(PrimApiException.class, () -> client.calculateJourneyPlans(request));
        assertTrue(ex.getMessage().contains("fallback"));
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimJourneyResponse.class));
    }

    @Test
    void calculateJourneyPlans_mapsSectionsWithAndWithoutStopDateTimes() {
        LocalDateTime dep = LocalDateTime.of(2026, 2, 14, 9, 0);
        LocalDateTime arr = LocalDateTime.of(2026, 2, 14, 9, 45);

        PrimCoordinates fromCoord = new PrimCoordinates(48.844, 2.374);
        PrimCoordinates toCoord = new PrimCoordinates(48.858, 2.347);
        PrimStopPoint from = new PrimStopPoint("sp-a", "A", fromCoord, new PrimStopArea("sa-a", "Area A", fromCoord));
        PrimStopPoint to = new PrimStopPoint("sp-b", "B", toCoord, new PrimStopArea("sa-b", "Area B", toCoord));

        PrimDisplayInformations di = new PrimDisplayInformations(
                "RER A", "A", "FF0000", "RATP", "rapid",
                List.of("has_air_conditioned"));

        PrimStopDateTime sdt1 = new PrimStopDateTime(from, dep, dep.plusMinutes(1));
        PrimStopDateTime sdt2 = new PrimStopDateTime(to, arr.minusMinutes(1), arr);

        PrimSection publicTransport = new PrimSection(
                "sec-1", "public_transport", 2700, dep, arr, from, to, di, List.of(sdt1, sdt2));
        PrimSection walking = new PrimSection(
                "sec-2", "street_network", 300, arr, arr.plusMinutes(5), to, to, null, null);

        List<PrimSection> sections = new ArrayList<>();
        sections.add(null);
        sections.add(publicTransport);
        sections.add(walking);
        PrimJourney journey = new PrimJourney("journey-1", 3000, 1, dep, arr.plusMinutes(5), sections);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimJourneyResponse(List.of(journey))));

        List<PrimJourneyPlanDto> plans = client.calculateJourneyPlans(new PrimJourneyRequest("sa-from", "sa-to", dep));

        assertEquals(1, plans.size());
        PrimJourneyPlanDto plan = plans.get(0);
        assertEquals("journey-1", plan.journeyId());
        assertEquals(2, plan.legs().size());
        assertNotNull(plan.legs().get(0).stopDateTimes());
        assertEquals(2, plan.legs().get(0).stopDateTimes().size());
        assertTrue(Boolean.TRUE.equals(plan.legs().get(0).hasAirConditioning()));
    }

    @Test
    void calculateJourneyPlans_returnsEmptyWhenResponseHasNoJourneys() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimJourneyResponse(null)));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(
                new PrimJourneyRequest("sa-from", "sa-to", LocalDateTime.now()));

        assertTrue(result.isEmpty());
    }
}
