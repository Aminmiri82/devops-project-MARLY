package org.marly.mavigo.client.prim;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimDisplayInformations;
import org.marly.mavigo.client.prim.model.PrimJourney;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.model.PrimJourneyResponse;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimPlacesResponse;
import org.marly.mavigo.client.prim.model.PrimSection;
import org.marly.mavigo.client.prim.model.PrimStopDateTime;
import org.marly.mavigo.client.prim.model.PrimStopPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
                    PrimPlacesResponse.class);

            PrimPlacesResponse placesResponse = response.getBody();
            return placesResponse != null && placesResponse.places() != null
                    ? placesResponse.places()
                    : List.of();
        } catch (RestClientException e) {
            throw new PrimApiException("Failed to search places: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PrimPlace> searchPlacesNearby(double latitude, double longitude, int radiusMeters) {
        return searchPlacesNearby(latitude, longitude, radiusMeters, null);
    }

    @Override
    public List<PrimPlace> searchPlacesNearby(double latitude, double longitude, int radiusMeters, String cityName) {
        LOGGER.debug("Searching for places near coordinates {}, {} (radius: {}m, city: {})",
                latitude, longitude, radiusMeters, cityName);

        if (cityName != null && !cityName.isBlank()) {
            try {
                String url = apiEndpoint + PLACES_ENDPOINT + "?q="
                        + URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                        + "&count=200&type[]=stop_area&type[]=stop_point";

                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<PrimPlacesResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        PrimPlacesResponse.class);

                PrimPlacesResponse placesResponse = response.getBody();
                List<PrimPlace> places = placesResponse != null && placesResponse.places() != null
                        ? placesResponse.places()
                        : List.of();

                LOGGER.debug("PRIM returned {} places for city '{}'", places.size(), cityName);

                List<PrimPlace> validPlaces = new ArrayList<>();
                for (PrimPlace place : places) {
                    if (place == null) continue;
                    if (!hasStopAreaOrPoint(place)) continue;

                    PrimCoordinates coords = placeCoordinates(place);
                    if (coords != null && coords.latitude() != null && coords.longitude() != null) {
                        double distance = calculateDistance(latitude, longitude,
                                coords.latitude(), coords.longitude());
                        if (radiusMeters >= 5000 || distance <= radiusMeters) {
                            validPlaces.add(place);
                        }
                    } else {
                        if (radiusMeters >= 5000) {
                            validPlaces.add(place);
                        }
                    }
                }

                validPlaces.sort((p1, p2) -> {
                    PrimCoordinates c1 = placeCoordinates(p1);
                    PrimCoordinates c2 = placeCoordinates(p2);
                    if (c1 == null && c2 == null) return 0;
                    if (c1 == null) return 1;
                    if (c2 == null) return -1;
                    double d1 = calculateDistance(latitude, longitude, c1.latitude(), c1.longitude());
                    double d2 = calculateDistance(latitude, longitude, c2.latitude(), c2.longitude());
                    return Double.compare(d1, d2);
                });

                if (!validPlaces.isEmpty()) {
                    LOGGER.info("Found {} stop areas near coordinates {}, {} (radius: {}m) using city '{}'",
                            validPlaces.size(), latitude, longitude, radiusMeters, cityName);
                    return validPlaces;
                } else {
                    LOGGER.warn("City search '{}' returned {} places but none within {}m of coordinates {}, {}",
                            cityName, places.size(), radiusMeters, latitude, longitude);
                }
            } catch (RestClientException e) {
                LOGGER.warn("City search '{}' failed: {}", cityName, e.getMessage());
            }
        }

        String coordQuery = String.format(Locale.ROOT, "coord:%.6f;%.6f", longitude, latitude);
        try {
            String url = apiEndpoint + PLACES_ENDPOINT + "?q="
                    + URLEncoder.encode(coordQuery, StandardCharsets.UTF_8)
                    + "&count=50&type[]=stop_area&type[]=stop_point";

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PrimPlacesResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PrimPlacesResponse.class);

            PrimPlacesResponse placesResponse = response.getBody();
            List<PrimPlace> places = placesResponse != null && placesResponse.places() != null
                    ? placesResponse.places()
                    : List.of();

            List<PrimPlace> validPlaces = new ArrayList<>();
            for (PrimPlace place : places) {
                if (place == null) continue;
                if (!hasStopAreaOrPoint(place)) continue;

                PrimCoordinates coords = placeCoordinates(place);
                if (coords != null && coords.latitude() != null && coords.longitude() != null) {
                    double distance = calculateDistance(latitude, longitude,
                            coords.latitude(), coords.longitude());
                    if (distance <= radiusMeters) {
                        validPlaces.add(place);
                    }
                }
            }

            if (!validPlaces.isEmpty()) {
                LOGGER.info("Found {} stop areas near coordinates {}, {} (within {}m)",
                        validPlaces.size(), latitude, longitude, radiusMeters);
                return validPlaces;
            }
        } catch (RestClientException e) {
            LOGGER.debug("Coord query '{}' failed: {}", coordQuery, e.getMessage());
        }

        LOGGER.warn("No stop areas found near coordinates {}, {} (radius: {}m)", latitude, longitude, radiusMeters);
        return List.of();
    }

    private boolean hasStopAreaOrPoint(PrimPlace place) {
        if (place == null) return false;
        return (place.stopArea() != null && place.stopArea().id() != null)
                || (place.stopPoint() != null && place.stopPoint().id() != null);
    }

    private PrimCoordinates placeCoordinates(PrimPlace place) {
        if (place == null) return null;
        if (place.coordinates() != null) return place.coordinates();
        if (place.stopArea() != null && place.stopArea().coordinates() != null) {
            return place.stopArea().coordinates();
        }
        if (place.stopPoint() != null && place.stopPoint().coordinates() != null) {
            return place.stopPoint().coordinates();
        }
        return null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
            return executeJourneyRequest(request);
        } catch (RestClientException e) {
            if (isNoOriginError(e)) {
                LOGGER.warn("PRIM returned no_origin. Retrying with direct_path=only and higher walking limit.");
                PrimJourneyRequest fallback = new PrimJourneyRequest(
                        request.getFromStopAreaId(),
                        request.getToStopAreaId(),
                        request.getDatetime());
                fallback.withDirectPath("only");
                fallback.withMaxWalkingDurationToPt(7200);
                fallback.withMaxDuration(14400);
                try {
                    return executeJourneyRequest(fallback);
                } catch (RestClientException ex) {
                    throw new PrimApiException("Failed to calculate journey (fallback): " + ex.getMessage(), ex);
                }
            }
            throw new PrimApiException("Failed to calculate journey: " + e.getMessage(), e);
        }
    }

    private List<PrimJourneyPlanDto> executeJourneyRequest(PrimJourneyRequest request) {
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
        request.getMaxWaitingDuration().ifPresent(duration -> uriBuilder.queryParam("max_waiting_duration", duration));
        request.getMaxWalkingDurationToPt().ifPresent(duration -> uriBuilder.queryParam("max_walking_duration_to_pt", duration));
        request.getDirectPath().ifPresent(path -> uriBuilder.queryParam("direct_path", path));
        request.getEquipmentDetails().ifPresent(details -> uriBuilder.queryParam("equipment_details", details));
        request.getFirstSectionModes().ifPresent(modes -> modes.forEach(mode -> uriBuilder.queryParam("first_section_mode[]", mode)));
        request.getLastSectionModes().ifPresent(modes -> modes.forEach(mode -> uriBuilder.queryParam("last_section_mode[]", mode)));

        String url = uriBuilder.toUriString();

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PrimJourneyResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                PrimJourneyResponse.class);

        PrimJourneyResponse journeyResponse = response.getBody();
        List<PrimJourneyPlanDto> plans = toJourneyPlanDtos(journeyResponse);
        LOGGER.info("Prim journeys API returned {} option(s)", plans.size());
        return plans;
    }

    private boolean isNoOriginError(RestClientException e) {
        if (e instanceof HttpClientErrorException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            return body != null && body.contains("\"id\":\"no_origin\"");
        }
        return false;
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

        List<PrimJourneyPlanDto.LegDto> legs = new ArrayList<>();
        int seq = 1;

        for (PrimSection section : sections) {
            if (section == null)
                continue;

            var sdt = section.stopDateTimes();
            PrimDisplayInformations di = section.displayInformations();

            // Extract line info from display informations
            String lineCode = di != null ? di.code() : null;
            String lineName = di != null ? di.label() : null;
            String lineColor = di != null ? di.color() : null;
            String networkName = di != null ? di.network() : null;

            // Build stopDateTimes DTO list
            List<PrimJourneyPlanDto.StopDateTimeDto> stopDateTimeDtos = mapStopDateTimes(sdt);

            if (sdt != null && sdt.size() >= 2) {
                PrimStopDateTime first = sdt.get(0);
                PrimStopDateTime last = sdt.get(sdt.size() - 1);

                if (first != null && last != null) {
                    PrimStopPoint from = first.stopPoint();
                    PrimStopPoint to = last.stopPoint();

                    if (from != null && to != null) {
                        legs.add(new PrimJourneyPlanDto.LegDto(
                                seq++,
                                section.id(),
                                section.type(),
                                di != null ? di.commercialMode() : null,
                                lineCode,
                                lineName,
                                lineColor,
                                networkName,
                                toOffset(first.departureDateTime() != null ? first.departureDateTime()
                                        : section.departureDateTime()),
                                toOffset(last.arrivalDateTime() != null ? last.arrivalDateTime() : section.arrivalDateTime()),
                                section.duration(),
                                from.id(),
                                from.name(),
                                extractLatitude(from),
                                extractLongitude(from),
                                to.id(),
                                to.name(),
                                extractLatitude(to),
                                extractLongitude(to),
                                lineName,
                                section.hasAirConditioning(),
                                stopDateTimeDtos));
                    }
                }
                continue;
            }

            legs.add(mapSection(section, seq++, stopDateTimeDtos));
        }

        return Collections.unmodifiableList(legs);
    }

    private List<PrimJourneyPlanDto.StopDateTimeDto> mapStopDateTimes(List<PrimStopDateTime> stopDateTimes) {
        if (stopDateTimes == null || stopDateTimes.isEmpty()) {
            return null;
        }

        List<PrimJourneyPlanDto.StopDateTimeDto> dtos = new ArrayList<>(stopDateTimes.size());
        for (PrimStopDateTime sdt : stopDateTimes) {
            if (sdt == null || sdt.stopPoint() == null) {
                continue;
            }

            PrimStopPoint sp = sdt.stopPoint();
            String stopAreaId = null;
            if (sp.stopArea() != null) {
                stopAreaId = sp.stopArea().id();
            }

            dtos.add(new PrimJourneyPlanDto.StopDateTimeDto(
                    sp.id(),
                    sp.name(),
                    stopAreaId,
                    extractLatitude(sp),
                    extractLongitude(sp),
                    toOffset(sdt.arrivalDateTime()),
                    toOffset(sdt.departureDateTime())));
        }

        return dtos.isEmpty() ? null : Collections.unmodifiableList(dtos);
    }

    private PrimJourneyPlanDto.LegDto mapSection(PrimSection section, int sequenceOrder,
            List<PrimJourneyPlanDto.StopDateTimeDto> stopDateTimeDtos) {
        PrimStopPoint from = section.from();
        PrimStopPoint to = section.to();
        PrimDisplayInformations di = section.displayInformations();

        String lineCode = di != null ? di.code() : null;
        String lineName = di != null ? di.label() : null;
        String lineColor = di != null ? di.color() : null;
        String networkName = di != null ? di.network() : null;

        return new PrimJourneyPlanDto.LegDto(
                sequenceOrder,
                section.id(),
                section.type(),
                di != null ? di.commercialMode() : null,
                lineCode,
                lineName,
                lineColor,
                networkName,
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
                lineName,
                section.hasAirConditioning(),
                stopDateTimeDtos);
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
