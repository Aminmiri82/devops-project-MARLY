package org.marly.mavigo.service.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.shared.GeoPoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class NominatimGeocodingServiceHappyPathTest {

    @Mock
    private RestTemplate restTemplate;

    private NominatimGeocodingService service;

    @BeforeEach
    void setUp() {
        service = new NominatimGeocodingService(restTemplate, "1.446,49.259,3.559,48.120", true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void geocode_returnsGeoPointWhenCoordinatesAreValid() throws Exception {
        Object responseBody = nominatimArray("48.8566", "2.3522", "Paris", Map.of("city", "Paris"));
        ResponseEntity<?> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) response);

        GeoPoint result = service.geocode("Paris");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(48.8566);
        assertThat(result.getLongitude()).isEqualTo(2.3522);
    }

    @Test
    @SuppressWarnings("unchecked")
    void geocode_returnsNullWhenCoordinatesAreInvalidNumbers() throws Exception {
        Object responseBody = nominatimArray("not-a-number", "2.3522", "Paris", Map.of());
        ResponseEntity<?> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) response);

        GeoPoint result = service.geocode("Paris");

        assertThat(result).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void geocode_returnsNullWhenLatOrLonMissing() throws Exception {
        Object responseBody = nominatimArray(null, "2.3522", "Paris", Map.of());
        ResponseEntity<?> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) response);

        GeoPoint result = service.geocode("Paris");

        assertThat(result).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void reverseGeocode_usesSuburbWhenNeighbourhoodBlank() throws Exception {
        Object body = reverseResponse("display", Map.of("neighbourhood", "  ", "suburb", "Bastille"));
        ResponseEntity<?> response = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) response);

        String result = service.reverseGeocode(new GeoPoint(48.85, 2.35));

        assertThat(result).isEqualTo("Bastille");
    }

    @Test
    @SuppressWarnings("unchecked")
    void reverseGeocode_fallsBackToTownWhenCityBlank() throws Exception {
        Object body = reverseResponse("display", Map.of("city", " ", "town", "Sceaux"));
        ResponseEntity<?> response = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) response);

        String result = service.reverseGeocode(new GeoPoint(48.77, 2.29));

        assertThat(result).isEqualTo("Sceaux");
    }

    @Test
    @SuppressWarnings("unchecked")
    void reverseGeocode_fallsBackToDisplayNameWhenAddressFieldsMissing() throws Exception {
        Object body = reverseResponse("  Full Place Name  ", Map.of());
        ResponseEntity<?> response = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) response);

        String result = service.reverseGeocode(new GeoPoint(48.80, 2.30));

        assertThat(result).isEqualTo("Full Place Name");
    }

    private Object nominatimArray(String lat, String lon, String displayName, Map<String, Object> address) throws Exception {
        Class<?> type = Class.forName("org.marly.mavigo.service.geocoding.NominatimGeocodingService$NominatimResponse");
        Constructor<?> constructor = type.getDeclaredConstructor(String.class, String.class, String.class, Map.class);
        constructor.setAccessible(true);
        Object element = constructor.newInstance(lat, lon, displayName, address);
        Object array = Array.newInstance(type, 1);
        Array.set(array, 0, element);
        return array;
    }

    private Object reverseResponse(String displayName, Map<String, Object> address) throws Exception {
        Class<?> type = Class.forName("org.marly.mavigo.service.geocoding.NominatimGeocodingService$ReverseGeocodeResponse");
        Constructor<?> constructor = type.getDeclaredConstructor(String.class, Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(displayName, address);
    }
}
