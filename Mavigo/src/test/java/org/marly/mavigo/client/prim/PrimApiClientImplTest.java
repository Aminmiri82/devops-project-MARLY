package org.marly.mavigo.client.prim;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrimApiClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private PrimApiClientImpl client;

    @BeforeEach
    void setUp() {
        client = new PrimApiClientImpl(
                restTemplate,
                "https://api.test.com",
                "test-api-key",
                "Europe/Paris"
        );
    }

    // ========== searchPlaces tests ==========

    @Test
    void searchPlaces_ShouldReturnPlaces_WhenValidQuery() {
        PrimStopArea stopArea = new PrimStopArea("stop-1", "Gare de Lyon", null);
        PrimPlace place = new PrimPlace("place-1", "Gare de Lyon", "stop_area", stopArea);
        PrimPlacesResponse response = new PrimPlacesResponse(List.of(place));

        when(restTemplate.exchange(
                contains("/places?q="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimPlacesResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimPlace> result = client.searchPlaces("Gare de Lyon");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gare de Lyon", result.get(0).name());
    }

    @Test
    void searchPlaces_ShouldReturnEmptyList_WhenNoPlacesFound() {
        PrimPlacesResponse response = new PrimPlacesResponse(List.of());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimPlace> result = client.searchPlaces("NonExistent");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchPlaces_ShouldReturnEmptyList_WhenResponseBodyIsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<PrimPlace> result = client.searchPlaces("Query");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchPlaces_ShouldThrowException_WhenQueryIsNull() {
        assertThrows(IllegalArgumentException.class, () -> client.searchPlaces(null));
    }

    @Test
    void searchPlaces_ShouldThrowException_WhenQueryIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> client.searchPlaces("   "));
    }

    @Test
    void searchPlaces_ShouldThrowPrimApiException_WhenRestClientFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimPlacesResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        PrimApiException ex = assertThrows(PrimApiException.class, () -> client.searchPlaces("Query"));
        assertTrue(ex.getMessage().contains("Failed to search places"));
    }

    // ========== calculateJourneyPlans tests ==========

    @Test
    void calculateJourneyPlans_ShouldReturnPlans_WhenValidRequest() {
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:IDFM:1",
                "stop:IDFM:2",
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );

        // PrimJourney(id, duration, nbTransfers, departureDateTime, arrivalDateTime, sections)
        PrimJourney journey = new PrimJourney(
                "journey-1",
                1800,
                1,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 30),
                List.of()
        );
        PrimJourneyResponse response = new PrimJourneyResponse(List.of(journey));

        when(restTemplate.exchange(
                contains("/journeys?"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PrimJourneyResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("journey-1", result.get(0).journeyId());
    }

    @Test
    void calculateJourneyPlans_ShouldReturnEmptyList_WhenNoJourneys() {
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:IDFM:1",
                "stop:IDFM:2",
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(new PrimJourneyResponse(List.of()), HttpStatus.OK));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void calculateJourneyPlans_ShouldThrowException_WhenRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> client.calculateJourneyPlans(null));
    }

    @Test
    void calculateJourneyPlans_ShouldThrowException_WhenFromIsNull() {
        PrimJourneyRequest request = new PrimJourneyRequest(null, "to", LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> client.calculateJourneyPlans(request));
    }

    @Test
    void calculateJourneyPlans_ShouldThrowException_WhenFromIsBlank() {
        PrimJourneyRequest request = new PrimJourneyRequest("  ", "to", LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> client.calculateJourneyPlans(request));
    }

    @Test
    void calculateJourneyPlans_ShouldThrowException_WhenToIsNull() {
        PrimJourneyRequest request = new PrimJourneyRequest("from", null, LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> client.calculateJourneyPlans(request));
    }

    @Test
    void calculateJourneyPlans_ShouldThrowException_WhenToIsBlank() {
        PrimJourneyRequest request = new PrimJourneyRequest("from", "  ", LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> client.calculateJourneyPlans(request));
    }

    @Test
    void calculateJourneyPlans_ShouldThrowException_WhenDatetimeIsNull() {
        PrimJourneyRequest request = new PrimJourneyRequest("from", "to", null);
        assertThrows(IllegalArgumentException.class, () -> client.calculateJourneyPlans(request));
    }

    @Test
    void calculateJourneyPlans_ShouldThrowPrimApiException_WhenRestClientFails() {
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:IDFM:1",
                "stop:IDFM:2",
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        PrimApiException ex = assertThrows(PrimApiException.class, () -> client.calculateJourneyPlans(request));
        assertTrue(ex.getMessage().contains("Failed to calculate journey"));
    }

    @Test
    void calculateJourneyPlans_ShouldMapSectionsCorrectly() {
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:IDFM:1",
                "stop:IDFM:2",
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );

        PrimCoordinates coords = new PrimCoordinates(48.8566, 2.3522);
        PrimStopPoint fromStop = new PrimStopPoint("sp1", "Station A", coords);
        PrimStopPoint toStop = new PrimStopPoint("sp2", "Station B", coords);
        
        // PrimStopDateTime(stopPoint, arrivalDateTime, departureDateTime)
        PrimStopDateTime fromDt = new PrimStopDateTime(
                fromStop,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );
        PrimStopDateTime toDt = new PrimStopDateTime(
                toStop,
                LocalDateTime.of(2024, 1, 15, 10, 15),
                LocalDateTime.of(2024, 1, 15, 10, 15)
        );

        // PrimDisplayInformations(label, code, color, network, commercialMode, equipments)
        PrimDisplayInformations displayInfo = new PrimDisplayInformations(
                "Metro Line 1", "M1", "#FFBE00", "RATP", "Metro", List.of()
        );
        
        // PrimSection(id, type, duration, departureDateTime, arrivalDateTime, from, to, displayInformations, stopDateTimes)
        PrimSection section = new PrimSection(
                "section-1",
                "public_transport",
                900,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 15),
                fromStop,
                toStop,
                displayInfo,
                List.of(fromDt, toDt)
        );

        // PrimJourney(id, duration, nbTransfers, departureDateTime, arrivalDateTime, sections)
        PrimJourney journey = new PrimJourney(
                "journey-1",
                900,
                0,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 15),
                List.of(section)
        );
        PrimJourneyResponse response = new PrimJourneyResponse(List.of(journey));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(request);

        assertEquals(1, result.size());
        PrimJourneyPlanDto plan = result.get(0);
        assertEquals(1, plan.legs().size());
        
        PrimJourneyPlanDto.LegDto leg = plan.legs().get(0);
        assertEquals("public_transport", leg.sectionType());
        assertEquals("Station A", leg.originLabel());
        assertEquals("Station B", leg.destinationLabel());
        assertEquals(48.8566, leg.originLatitude());
    }

    @Test
    void calculateJourneyPlans_ShouldHandleNullSections() {
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:IDFM:1",
                "stop:IDFM:2",
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );

        // PrimJourney(id, duration, nbTransfers, departureDateTime, arrivalDateTime, sections)
        PrimJourney journey = new PrimJourney(
                "journey-1",
                1800,
                0,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 30),
                null
        );
        PrimJourneyResponse response = new PrimJourneyResponse(List.of(journey));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(request);

        assertEquals(1, result.size());
        assertTrue(result.get(0).legs().isEmpty());
    }

    @Test
    void calculateJourneyPlans_ShouldHandleSectionWithoutStopDateTimes() {
        PrimJourneyRequest request = new PrimJourneyRequest(
                "stop:IDFM:1",
                "stop:IDFM:2",
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );

        PrimCoordinates coords = new PrimCoordinates(48.8566, 2.3522);
        PrimStopPoint fromStop = new PrimStopPoint("sp1", "Station A", coords);
        PrimStopPoint toStop = new PrimStopPoint("sp2", "Station B", coords);
        
        // Section without stop_date_times (will be mapped via mapSection fallback)
        PrimSection section = new PrimSection(
                "section-1",
                "transfer",
                300,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 5),
                fromStop,
                toStop,
                null,
                null
        );

        PrimJourney journey = new PrimJourney(
                "journey-1",
                300,
                0,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 10, 5),
                List.of(section)
        );
        PrimJourneyResponse response = new PrimJourneyResponse(List.of(journey));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PrimJourneyResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<PrimJourneyPlanDto> result = client.calculateJourneyPlans(request);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).legs().size());
        assertEquals("transfer", result.get(0).legs().get(0).sectionType());
    }
}
