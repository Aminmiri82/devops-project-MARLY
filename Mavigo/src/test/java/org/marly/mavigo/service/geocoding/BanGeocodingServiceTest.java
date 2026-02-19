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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

        @ParameterizedTest(name = "geocode avec adresse \"{0}\" retourne null")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void geocode_withNullOrBlankAddress_returnsNull(String address) {
            GeoPoint result = service.geocode(address);
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
        @DisplayName("geocode avec réponse valide retourne GeoPoint")
        void geocode_withValidResponse_returnsGeoPoint() {
            // Given
            BanGeocodingService.BanResponse response = new BanGeocodingService.BanResponse();
            BanGeocodingService.BanFeature feature = new BanGeocodingService.BanFeature();
            feature.geometry = new BanGeocodingService.BanGeometry();
            feature.geometry.coordinates = java.util.List.of(2.3522, 48.8566);
            feature.properties = new BanGeocodingService.BanProperties();
            feature.properties.label = "Paris";
            feature.properties.score = 1.0;
            response.features = java.util.List.of(feature);

            when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class)))
                    .thenReturn(response);

            // When
            GeoPoint result = service.geocode("Paris");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLatitude()).isEqualTo(48.8566);
            assertThat(result.getLongitude()).isEqualTo(2.3522);
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
        @DisplayName("reverseGeocode avec réponse valide retourne label")
        void reverseGeocode_withValidResponse_returnsLabel() {
            // Given
            GeoPoint point = new GeoPoint(48.8566, 2.3522);
            BanGeocodingService.BanResponse response = new BanGeocodingService.BanResponse();
            BanGeocodingService.BanFeature feature = new BanGeocodingService.BanFeature();
            feature.properties = new BanGeocodingService.BanProperties();
            feature.properties.label = "Paris, France";
            response.features = java.util.List.of(feature);

            when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class)))
                    .thenReturn(response);

            // When
            String result = service.reverseGeocode(point);

            // Then
            assertThat(result).isEqualTo("Paris, France");
        }

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

    @Nested
    @DisplayName("Tests chooseBestFeature")
    class ChooseBestFeatureTests {
        @Test
        @DisplayName("choisit la meilleure feature basée sur le score et le cityHint")
        void chooseBestFeature_withCityHint_returnsMatchingCity() {
            // Given - Paris (score 0.5) vs Nanterre (score 0.9), avec cityHint "Paris"
            BanGeocodingService.BanFeature f1 = new BanGeocodingService.BanFeature();
            f1.properties = new BanGeocodingService.BanProperties();
            f1.properties.city = "Paris";
            f1.properties.score = 0.5;
            f1.geometry = new BanGeocodingService.BanGeometry();
            f1.geometry.coordinates = java.util.List.of(2.3522, 48.8566);

            BanGeocodingService.BanFeature f2 = new BanGeocodingService.BanFeature();
            f2.properties = new BanGeocodingService.BanProperties();
            f2.properties.city = "Nanterre";
            f2.properties.score = 0.9;
            f2.geometry = new BanGeocodingService.BanGeometry();
            f2.geometry.coordinates = java.util.List.of(2.2137, 48.8924);

            BanGeocodingService.BanResponse response = new BanGeocodingService.BanResponse();
            response.features = java.util.List.of(f1, f2);

            when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class)))
                    .thenReturn(response);

            // When - geocode utilise chooseBestFeature en interne, cityHint = "Paris" (après la virgule)
            GeoPoint result = service.geocode("10 Rue de Test, Paris");

            // Then - doit retourner Paris (matching cityHint) et non Nanterre (meilleur score)
            assertThat(result).isNotNull();
            assertThat(result.getLatitude()).isEqualTo(48.8566);
            assertThat(result.getLongitude()).isEqualTo(2.3522);
        }
    }
}
