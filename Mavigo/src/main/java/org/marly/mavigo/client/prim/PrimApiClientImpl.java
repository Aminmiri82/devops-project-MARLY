package org.marly.mavigo.client.prim;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PrimApiClientImpl implements PrimApiClient {

    private static final String PLACES_ENDPOINT = "/places";
    private static final String JOURNEYS_ENDPOINT = "/journeys";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final RestTemplate restTemplate;
    private final String apiEndpoint;
    private final String apiKey;

    public PrimApiClientImpl(
            RestTemplate restTemplate,
            @Value("${PRIM_API_ENDPOINT:https://prim.iledefrance-mobilites.fr/marketplace/v2/navitia}") String apiEndpoint,
            @Value("${PRIM_API_KEY}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiEndpoint = apiEndpoint;
        this.apiKey = apiKey;
    }

    @Override
    public List<PrimPlace> searchPlaces(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = apiEndpoint + PLACES_ENDPOINT + "?q=" + encodedQuery;

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PrimPlacesResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PrimPlacesResponse.class
            );

            PrimPlacesResponse placesResponse = response.getBody();
            return placesResponse != null && placesResponse.places() != null
                    ? placesResponse.places()
                    : List.of();
        } catch (RestClientException e) {
            throw new PrimApiException("Failed to search places: " + e.getMessage(), e);
        }
    }

    @Override
    public PrimJourneyResponse getJourney(PrimJourneyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Journey request cannot be null");
        }
        if (request.getFromStopAreaId() == null || request.getFromStopAreaId().isBlank()) {
            throw new IllegalArgumentException("From stop area ID cannot be null or empty");
        }
        if (request.getToStopAreaId() == null || request.getToStopAreaId().isBlank()) {
            throw new IllegalArgumentException("To stop area ID cannot be null or empty");
        }
        if (request.getDatetime() == null) {
            throw new IllegalArgumentException("Datetime cannot be null");
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(apiEndpoint + JOURNEYS_ENDPOINT)
                    .queryParam("from", request.getFromStopAreaId())
                    .queryParam("to", request.getToStopAreaId())
                    .queryParam("datetime", formatDateTime(request.getDatetime()))
                    .queryParam("datetime_represents", request.getDatetimeRepresents());

            // place holder params
            request.getMaxDuration().ifPresent(duration -> uriBuilder.queryParam("max_duration", duration));
            request.getMaxNbTransfers().ifPresent(transfers -> uriBuilder.queryParam("max_nb_transfers", transfers));
            request.getWheelchair().ifPresent(wheelchair -> uriBuilder.queryParam("wheelchair", wheelchair));
            request.getRealtime().ifPresent(realtime -> uriBuilder.queryParam("realtime", realtime));

            String url = uriBuilder.toUriString();

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PrimJourneyResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PrimJourneyResponse.class
            );

            PrimJourneyResponse journeyResponse = response.getBody();
            if (journeyResponse == null) {
                throw new PrimApiException("Received null response from journeys API");
            }
            return journeyResponse;
        } catch (RestClientException e) {
            throw new PrimApiException("Failed to get journey: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PrimDisruption> getDisruptions() {
        return List.of(
                new PrimDisruption(
                        "1",
                        "RER A",
                        "Trafic interrompu entre La Défense et Nanterre-Préfecture",
                        "HIGH",
                        LocalDateTime.now().minusMinutes(20)
                ),
                new PrimDisruption(
                        "2",
                        "Metro 13",
                        "Ralentissements en raison d'un incident technique",
                        "MEDIUM",
                        LocalDateTime.now().minusMinutes(5)
                ),
                new PrimDisruption(
                        "3",
                        "Bus 27",
                        "Retards importants dus à un embouteillage",
                        "LOW",
                        LocalDateTime.now()
                )
        );
    }

    @Override
    public PrimItineraryResponse planItinerary(PrimItineraryRequest request) {
        // to do: rename the get journey method to planItinerary
        throw new UnsupportedOperationException("planItinerary doesnt exist yet");
    }

    @Override
    public List<PrimDisruption> fetchRealtimeDisruptions() {
        throw new UnsupportedOperationException("fetchRealtimeDisruptions doesnt exist yet");
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        return headers;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }
}

