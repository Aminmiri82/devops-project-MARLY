package org.marly.mavigo.service.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.shared.GeoPoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Tests unitaires - BanGeocodingService")
class BanGeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private BanGeocodingService service;

    @BeforeEach
    void setUp() {
        service = new BanGeocodingService(restTemplate, "https://api-adresse.data.gouv.fr");
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
        @DisplayName("geocode avec réponse null retourne null")
        void geocode_withNullResponse_returnsNull() {
            // Given
            when(restTemplate.getForObject(anyString(), any()))
                    .thenReturn(null);

            // When
            GeoPoint result = service.geocode("Test");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec RestClientException retourne null")
        void geocode_withRestClientException_returnsNull() {
            // Given
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RestClientException("Connection refused"));

            // When
            GeoPoint result = service.geocode("Test");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("geocode avec RuntimeException retourne null")
        void geocode_withRuntimeException_returnsNull() {
            // Given
            when(restTemplate.getForObject(anyString(), any()))
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
        @DisplayName("reverseGeocode avec réponse null retourne null")
        void reverseGeocode_withNullResponse_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, 2.3730);

            when(restTemplate.getForObject(anyString(), any()))
                    .thenReturn(null);

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec RestClientException retourne null")
        void reverseGeocode_withRestClientException_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, 2.3730);

            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RestClientException("Connection refused"));

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reverseGeocode avec RuntimeException retourne null")
        void reverseGeocode_withRuntimeException_returnsNull() {
            // Given
            GeoPoint point = new GeoPoint(48.8443, 2.3730);

            when(restTemplate.getForObject(anyString(), any()))
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
        @DisplayName("service créé avec baseUrl custom fonctionne")
        void service_withCustomBaseUrl_works() {
            // Given
            BanGeocodingService customService = new BanGeocodingService(restTemplate, "https://custom-api.example.com");

            // When
            GeoPoint result = customService.geocode(null);

            // Then
            assertThat(result).isNull();
        }
    }
}
