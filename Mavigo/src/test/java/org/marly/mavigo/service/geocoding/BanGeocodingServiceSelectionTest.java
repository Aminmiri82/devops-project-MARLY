package org.marly.mavigo.service.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.shared.GeoPoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class BanGeocodingServiceSelectionTest {

    @Mock
    private RestTemplate restTemplate;

    private BanGeocodingService service;

    @BeforeEach
    void setUp() {
        service = new BanGeocodingService(restTemplate, "https://api-adresse.data.gouv.fr");
    }

    @Test
    void geocode_prefersFeatureMatchingCityHint() {
        BanGeocodingService.BanResponse response = response(
                feature(2.35, 48.86, "Best score but wrong city", "Lyon", 0.95),
                feature(2.33, 48.85, "Lower score but matching city", "PARIS", 0.40));
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        GeoPoint result = service.geocode("10 Rue de Test, Paris");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(48.85);
        assertThat(result.getLongitude()).isEqualTo(2.33);
    }

    @Test
    void geocode_prefersFeatureMatchingLabelWhenCityMissing() {
        BanGeocodingService.BanResponse response = response(
                feature(2.35, 48.86, "Anywhere", null, 0.99),
                feature(2.31, 48.84, "Gare, Paris", null, 0.10));
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        GeoPoint result = service.geocode("Place de la Republique, Paris");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(48.84);
        assertThat(result.getLongitude()).isEqualTo(2.31);
    }

    @Test
    void geocode_withoutCityHint_usesHighestScore() {
        BanGeocodingService.BanResponse response = response(
                feature(2.30, 48.80, "Low", "Paris", 0.11),
                feature(2.40, 48.90, "High", "Paris", 0.88));
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        GeoPoint result = service.geocode("Simple address without comma");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(48.90);
        assertThat(result.getLongitude()).isEqualTo(2.40);
    }

    @Test
    void geocode_whenNoCityMatch_fallsBackToHighestScore() {
        BanGeocodingService.BanResponse response = response(
                feature(2.21, 48.71, "Candidate A", "Lyon", 0.25),
                feature(2.22, 48.72, "Candidate B", "Marseille", 0.75));
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        GeoPoint result = service.geocode("Address, Paris");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(48.72);
        assertThat(result.getLongitude()).isEqualTo(2.22);
    }

    @Test
    void geocode_returnsNullWhenFallbackSelectsInvalidGeometry() {
        BanGeocodingService.BanFeature invalidHighest = feature(2.21, 48.71, "Invalid", "Lyon", 0.99);
        invalidHighest.geometry = null;

        BanGeocodingService.BanResponse response = response(
                invalidHighest,
                feature(2.22, 48.72, "Valid lower score", "Marseille", 0.40));
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        GeoPoint result = service.geocode("Address, Paris");

        assertThat(result).isNull();
    }

    @Test
    void reverseGeocode_returnsNullWhenFeatureHasNoProperties() {
        BanGeocodingService.BanResponse response = new BanGeocodingService.BanResponse();
        response.features = List.of(new BanGeocodingService.BanFeature());
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        String result = service.reverseGeocode(new GeoPoint(48.85, 2.35));

        assertThat(result).isNull();
    }

    @Test
    void reverseGeocode_returnsNullWhenLabelIsBlank() {
        BanGeocodingService.BanFeature feature = new BanGeocodingService.BanFeature();
        feature.properties = new BanGeocodingService.BanProperties();
        feature.properties.label = "   ";

        BanGeocodingService.BanResponse response = new BanGeocodingService.BanResponse();
        response.features = List.of(feature);
        when(restTemplate.getForObject(anyString(), eq(BanGeocodingService.BanResponse.class))).thenReturn(response);

        String result = service.reverseGeocode(new GeoPoint(48.85, 2.35));

        assertThat(result).isNull();
    }

    private BanGeocodingService.BanResponse response(BanGeocodingService.BanFeature... features) {
        BanGeocodingService.BanResponse response = new BanGeocodingService.BanResponse();
        response.features = List.of(features);
        return response;
    }

    private BanGeocodingService.BanFeature feature(double lon, double lat, String label, String city, double score) {
        BanGeocodingService.BanFeature feature = new BanGeocodingService.BanFeature();
        feature.geometry = new BanGeocodingService.BanGeometry();
        feature.geometry.coordinates = List.of(lon, lat);
        feature.properties = new BanGeocodingService.BanProperties();
        feature.properties.label = label;
        feature.properties.city = city;
        feature.properties.score = score;
        return feature;
    }
}
