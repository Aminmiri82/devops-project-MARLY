package org.marly.mavigo.client.prim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimJourney;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.model.PrimJourneyResponse;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimPlacesResponse;
import org.marly.mavigo.client.prim.model.PrimSection;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.client.prim.model.PrimStopDateTime;
import org.marly.mavigo.client.prim.model.PrimStopPoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PrimApiClientImplEdgeMappingTest {

    @Mock
    private RestTemplate restTemplate;

    private PrimApiClientImpl client;

    @BeforeEach
    void setUp() {
        client = new PrimApiClientImpl(restTemplate, "https://example.com", "edge-key", "Europe/Paris");
    }

    @Test
    void searchPlaces_returnsEmptyWhenBodyIsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        List<PrimPlace> result = client.searchPlaces("Paris");

        assertThat(result).isEmpty();
    }

    @Test
    void searchPlacesNearby_cityPathFiltersAndSortsWithNullAndCoordlessPlaces() {
        PrimPlace ignoredNull = null;
        PrimPlace ignoredNoStop = new PrimPlace("x", "x", "address", null, null, new PrimCoordinates(48.8, 2.3));
        PrimPlace coordlessStop = new PrimPlace("sa-nocoord", "No Coord", "stop_area", new PrimStopArea("sa-nocoord", "No Coord", null), null, null);
        PrimPlace stopPointWithCoords = new PrimPlace(
                "sp-1", "StopPoint", "stop_point", null,
                new PrimStopPoint("sp-1", "StopPoint", new PrimCoordinates(48.8002, 2.3002), null),
                null);
        PrimPlace topLevelCoords = new PrimPlace(
                "sa-1", "Top Level", "stop_area", new PrimStopArea("sa-1", "Top Level", null), null,
                new PrimCoordinates(48.8001, 2.3001));

        when(restTemplate.exchange(contains("q=Paris"), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(ResponseEntity.ok(
                        new PrimPlacesResponse(Arrays.asList(ignoredNull, ignoredNoStop, coordlessStop, stopPointWithCoords, topLevelCoords))));

        List<PrimPlace> result = client.searchPlacesNearby(48.8000, 2.3000, 5000, "Paris");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo("sa-1");
        assertThat(result.get(1).id()).isEqualTo("sp-1");
        assertThat(result.get(2).id()).isEqualTo("sa-nocoord");
    }

    @Test
    void searchPlacesNearby_returnsEmptyWhenCoordQueryFails() {
        when(restTemplate.exchange(contains("coord%3A2.300000%3B48.800000"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(PrimPlacesResponse.class)))
                .thenThrow(new RestClientException("down"));

        List<PrimPlace> result = client.searchPlacesNearby(48.8, 2.3, 1000, null);

        assertThat(result).isEmpty();
    }

    @Test
    void calculateJourneyPlans_validatesRequestFields() {
        assertThatThrownBy(() -> client.calculateJourneyPlans(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Journey request cannot be null");

        PrimJourneyRequest blankFrom = new PrimJourneyRequest(" ", "to", LocalDateTime.now());
        assertThatThrownBy(() -> client.calculateJourneyPlans(blankFrom))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From stop area ID");

        PrimJourneyRequest emptyTo = new PrimJourneyRequest("from", "", LocalDateTime.now());
        assertThatThrownBy(() -> client.calculateJourneyPlans(emptyTo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To stop area ID");

        PrimJourneyRequest nullDatetime = new PrimJourneyRequest("from", "to", null);
        assertThatThrownBy(() -> client.calculateJourneyPlans(nullDatetime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Datetime cannot be null");
    }

    @Test
    void calculateJourneyPlans_wrapsNonNoOriginRestClientError() {
        PrimJourneyRequest request = new PrimJourneyRequest("from", "to", LocalDateTime.now());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenThrow(new RestClientException("gateway timeout"));

        assertThatThrownBy(() -> client.calculateJourneyPlans(request))
                .isInstanceOf(PrimApiException.class)
                .hasMessageContaining("Failed to calculate journey:");
    }

    @Test
    void calculateJourneyPlans_mapsFallbackSectionWhenDisplayInfoMissingAndStopTimesInvalid() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 14, 10, 0);

        PrimStopDateTime invalidSdt = new PrimStopDateTime(null, now.plusMinutes(1), now.plusMinutes(2));
        PrimSection skippedBecauseInvalidSdt = new PrimSection(
                "sec-skip", "public_transport", 60, now, now.plusMinutes(1),
                null, null, null, List.of(invalidSdt, invalidSdt));

        PrimSection mappedFallback = new PrimSection(
                "sec-map", "transfer", 120, null, null,
                null, null, null, List.of(invalidSdt));

        PrimJourney journey = new PrimJourney("j-edge", 120, 0, now, now.plusMinutes(3),
                List.of(skippedBecauseInvalidSdt, mappedFallback));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(ResponseEntity.ok(new PrimJourneyResponse(List.of(journey))));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(new PrimJourneyRequest("from", "to", now));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).legs()).hasSize(1);
        PrimJourneyPlanDto.LegDto leg = result.get(0).legs().get(0);
        assertThat(leg.sectionId()).isEqualTo("sec-map");
        assertThat(leg.lineCode()).isNull();
        assertThat(leg.originStopId()).isNull();
        assertThat(leg.destinationStopId()).isNull();
        assertThat(leg.departureDateTime()).isNull();
        assertThat(leg.arrivalDateTime()).isNull();
        assertThat(leg.stopDateTimes()).isNull();
    }
}
