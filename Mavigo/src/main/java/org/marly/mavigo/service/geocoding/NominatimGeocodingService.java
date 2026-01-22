package org.marly.mavigo.service.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.marly.mavigo.models.shared.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class NominatimGeocodingService implements GeocodingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NominatimGeocodingService.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    
    private final RestTemplate restTemplate;
    private final String viewbox;
    private final boolean bounded;

    public NominatimGeocodingService(
            RestTemplate restTemplate,
            @Value("${geocoding.nominatim.viewbox:1.446,49.259,3.559,48.120}") String viewbox,
            @Value("${geocoding.nominatim.bounded:true}") boolean bounded) {
        this.restTemplate = restTemplate;
        this.viewbox = viewbox;
        this.bounded = bounded;
    }

    @Override
    public GeoPoint geocode(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?q=" + encodedAddress
                    + "&format=json&limit=1&addressdetails=1"
                    + "&countrycodes=fr"
                    + "&accept-language=fr";
            if (viewbox != null && !viewbox.isBlank()) {
                url += "&viewbox=" + URLEncoder.encode(viewbox, StandardCharsets.UTF_8);
                if (bounded) {
                    url += "&bounded=1";
                }
            }

            // Nominatim requires a User-Agent header per their usage policy
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mavigo/1.0 (Transport Planning App)");
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    NominatimResponse[].class);

            NominatimResponse[] results = response.getBody();
            if (results == null || results.length == 0) {
                LOGGER.debug("No geocoding results for address: {}", address);
                return null;
            }

            NominatimResponse first = results[0];
            if (first.lat() != null && first.lon() != null) {
                try {
                    double lat = Double.parseDouble(first.lat());
                    double lon = Double.parseDouble(first.lon());
                    LOGGER.info("Geocoded '{}' to coordinates: {}, {}", address, lat, lon);
                    return new GeoPoint(lat, lon);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid coordinates in Nominatim response: lat={}, lon={}", first.lat(), first.lon());
                    return null;
                }
            }

            return null;
        } catch (RestClientException e) {
            LOGGER.warn("Failed to geocode address '{}': {}", address, e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("Unexpected error while geocoding '{}'", address, e);
            return null;
        }
    }

    @Override
    public String reverseGeocode(GeoPoint point) {
        if (point == null || !point.isComplete()) {
            return null;
        }

        try {
            String url = "https://nominatim.openstreetmap.org/reverse" 
                    + "?lat=" + point.getLatitude()
                    + "&lon=" + point.getLongitude()
                    + "&format=json&addressdetails=1"
                    + "&zoom=16"; // Detail level: neighborhood/city

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mavigo/1.0 (Transport Planning App)");
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<ReverseGeocodeResponse> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    ReverseGeocodeResponse.class);

            ReverseGeocodeResponse result = response.getBody();
            if (result == null || result.address() == null) {
                return null;
            }

            // Extract the neighborhood, city, or place name
            Map<String, Object> address = result.address();
            String name = (String) address.get("neighbourhood");
            if (name == null || name.isBlank()) {
                name = (String) address.get("suburb");
            }
            if (name == null || name.isBlank()) {
                name = (String) address.get("city");
            }
            if (name == null || name.isBlank()) {
                name = (String) address.get("town");
            }
            if (name == null || name.isBlank()) {
                name = result.displayName();
            }

            return name != null ? name.trim() : null;
        } catch (RestClientException e) {
            LOGGER.warn("Failed to reverse geocode coordinates {}, {}: {}", 
                    point.getLatitude(), point.getLongitude(), e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("Unexpected error while reverse geocoding", e);
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResponse(
            @JsonProperty("lat") String lat,
            @JsonProperty("lon") String lon,
            @JsonProperty("display_name") String displayName,
            @JsonProperty("address") Map<String, Object> address
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReverseGeocodeResponse(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("address") Map<String, Object> address
    ) {}
}
