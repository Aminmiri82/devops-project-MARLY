package org.marly.mavigo.service.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.shared.GeoPoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - NominatimGeocodingService")
class NominatimGeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private NominatimGeocodingService service;

    @BeforeEach
    void setUp() {
        service = new NominatimGeocodingService(restTemplate, "1.446,49.259,3.559,48.120", true);
    }

    @Nested
    @DisplayName("Tests geocode")
    class GeocodeTests {

        @Test
        @DisplayName("geocode avec adresse null retourne null")
        void geocode_withNullAddress_returnsNull() {
            // When
            GeoPoint result = service.geocode(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec adresse vide retourne null")
        void geocode_withBlankAddress_returnsNull() {
            // When
            GeoPoint result = service.geocode("   ");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec adresse vide (chaîne vide) retourne null")
        void geocode_withEmptyAddress_returnsNull() {
            // When
            GeoPoint result = service.geocode("");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec RestClientException retourne null")
        @SuppressWarnings("unchecked")
        void geocode_withRestClientException_returnsNull() {
            // Given
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            // When
            GeoPoint result = service.geocode("Gare de Lyon");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec réponse null du serveur retourne null")
        @SuppressWarnings("unchecked")
        void geocode_withNullResponseBody_returnsNull() {
            // Given
            ResponseEntity<?> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenReturn((ResponseEntity) responseEntity);

            // When
            GeoPoint result = service.geocode("Test");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec tableau de résultats vide retourne null")
        @SuppressWarnings("unchecked")
        void geocode_withEmptyResultArray_returnsNull() {
            // Given
            Object[] emptyArray = new Object[0];
            ResponseEntity<?> responseEntity = new ResponseEntity<>(emptyArray, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenReturn((ResponseEntity) responseEntity);

            // When
            GeoPoint result = service.geocode("Unknown Address");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec RuntimeException retourne null")
        @SuppressWarnings("unchecked")
        void geocode_withRuntimeException_returnsNull() {
            // Given
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            GeoPoint result = service.geocode("Test");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Tests reverseGeocode")
    class ReverseGeocodeTests {

        @Test
        @DisplayName("reverseGeocode avec point null retourne null")
        void reverseGeocode_withNullPoint_returnsNull() {
            // When
            String result = service.reverseGeocode(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec point incomplet (latitude null) retourne null")
        void reverseGeocode_withNullLatitude_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(null, 2.3730);

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec point incomplet (longitude null) retourne null")
        void reverseGeocode_withNullLongitude_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, null);

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec RestClientException retourne null")
        @SuppressWarnings("unchecked")
        void reverseGeocode_withRestClientException_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, 2.3730);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec réponse null retourne null")
        @SuppressWarnings("unchecked")
        void reverseGeocode_withNullResponse_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, 2.3730);
            ResponseEntity<?> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenReturn((ResponseEntity) responseEntity);

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec RuntimeException retourne null")
        @SuppressWarnings("unchecked")
        void reverseGeocode_withRuntimeException_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, 2.3730);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(Class.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Tests de configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("service créé avec viewbox vide fonctionne")
        void service_withEmptyViewbox_works() {
            // Given
            NominatimGeocodingService serviceNoViewbox = new NominatimGeocodingService(restTemplate, "", false);

            // When
            GeoPoint result = serviceNoViewbox.geocode(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("service créé avec viewbox null fonctionne")
        void service_withNullViewbox_works() {
            // Given
            NominatimGeocodingService serviceNullViewbox = new NominatimGeocodingService(restTemplate, null, false);

            // When
            GeoPoint result = serviceNullViewbox.geocode(null);

            // Then
            assertThat(result).isNull();
        }
    }
}
