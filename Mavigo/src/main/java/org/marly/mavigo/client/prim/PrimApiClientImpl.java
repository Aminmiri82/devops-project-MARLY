package org.marly.mavigo.client.prim;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PrimApiClientImpl implements PrimApiClient {

    private static final String PLACES_ENDPOINT = "/places";
    private static final String JOURNEYS_ENDPOINT = "/journeys";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimApiClientImpl.class);

    private final RestTemplate restTemplate;
    private final String apiEndpoint;
    private final String apiKey;
    private final ZoneId navitiaZone;

    public PrimApiClientImpl(
            RestTemplate restTemplate,
            @Value("${PRIM_API_ENDPOINT:https://prim.iledefrance-mobilites.fr/marketplace/v2/navitia}") String apiEndpoint,
            @Value("${PRIM_API_KEY}") String apiKey,
            @Value("${PRIM_API_TIMEZONE:Europe/Paris}") String navitiaTimezoneId) {
        this.restTemplate = restTemplate;
        this.apiEndpoint = apiEndpoint;
        this.apiKey = apiKey;
        this.navitiaZone = ZoneId.of(navitiaTimezoneId);
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
    public List<PrimJourneyPlanDto> calculateJourneyPlans(PrimJourneyRequest request) {
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
            LOGGER.info("Requesting journey from {} to {} at {}",
                    request.getFromStopAreaId(),
                    request.getToStopAreaId(),
                    request.getDatetime());

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(apiEndpoint + JOURNEYS_ENDPOINT)
                    .queryParam("from", request.getFromStopAreaId())
                    .queryParam("to", request.getToStopAreaId())
                    .queryParam("datetime", formatDateTime(request.getDatetime()))
                    .queryParam("datetime_represents", request.getDatetimeRepresents());

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
            List<PrimJourneyPlanDto> plans = toJourneyPlanDtos(journeyResponse);
            LOGGER.info("Prim journeys API returned {} option(s)", plans.size());
            return plans;
        } catch (RestClientException e) {
            throw new PrimApiException("Failed to calculate journey: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        return headers;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }

    private List<PrimJourneyPlanDto> toJourneyPlanDtos(PrimJourneyResponse response) {
        if (response == null || response.journeys() == null || response.journeys().isEmpty()) {
            return List.of();
        }

        List<PrimJourneyPlanDto> plans = new ArrayList<>(response.journeys().size());
        for (PrimJourney journey : response.journeys()) {
            if (journey == null) {
                continue;
            }
            plans.add(mapJourney(journey));
        }
        return Collections.unmodifiableList(plans);
    }

    private PrimJourneyPlanDto mapJourney(PrimJourney journey) {
        List<PrimJourneyPlanDto.LegDto> legs = mapSections(journey.sections());
        return new PrimJourneyPlanDto(
                journey.id(),
                toOffset(journey.departureDateTime()),
                toOffset(journey.arrivalDateTime()),
                journey.duration(),
                journey.nbTransfers(),
                legs);
    }

    private List<PrimJourneyPlanDto.LegDto> mapSections(List<PrimSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }

        List<PrimJourneyPlanDto.LegDto> legs = new ArrayList<>(sections.size());
        for (int index = 0; index < sections.size(); index++) {
            PrimSection section = sections.get(index);
            legs.add(mapSection(section, index));
        }
        return Collections.unmodifiableList(legs);
    }

    private PrimJourneyPlanDto.LegDto mapSection(PrimSection section, int index) {
        PrimStopPoint from = section.from();
        PrimStopPoint to = section.to();
        PrimDisplayInformations displayInformations = section.displayInformations();

        return new PrimJourneyPlanDto.LegDto(
                index + 1,
                section.id(),
                section.type(),
                displayInformations != null ? displayInformations.commercialMode() : null,
                displayInformations != null ? displayInformations.code() : null,
                toOffset(section.departureDateTime()),
                toOffset(section.arrivalDateTime()),
                section.duration(),
                from != null ? from.id() : null,
                from != null ? from.name() : null,
                extractLatitude(from),
                extractLongitude(from),
                to != null ? to.id() : null,
                to != null ? to.name() : null,
                extractLatitude(to),
                extractLongitude(to),
                displayInformations != null ? displayInformations.label() : null);
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(navitiaZone).toOffsetDateTime();
    }

    private Double extractLatitude(PrimStopPoint stopPoint) {
        if (stopPoint == null || stopPoint.coordinates() == null) {
            return null;
        }
        return stopPoint.coordinates().latitude();
    }

    private Double extractLongitude(PrimStopPoint stopPoint) {
        if (stopPoint == null || stopPoint.coordinates() == null) {
            return null;
        }
        return stopPoint.coordinates().longitude();
    }
}

