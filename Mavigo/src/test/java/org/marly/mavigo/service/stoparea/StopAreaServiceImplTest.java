package org.marly.mavigo.service.stoparea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.client.prim.model.PrimStopPoint;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.marly.mavigo.service.geocoding.GeocodingService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - StopAreaServiceImpl")
class StopAreaServiceImplTest {

    @Mock
    private StopAreaRepository stopAreaRepository;

    @Mock
    private PrimApiClient primApiClient;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private StopAreaServiceImpl service;

    @Nested
    @DisplayName("Tests findOrCreateByQuery - chaîne de fallback")
    class FindOrCreateByQueryFallbackTests {

        @Test
        @DisplayName("retourne une zone d'arrêt existante par nom")
        void findOrCreateByQuery_returnsExistingByName() {
            // Given
            String query = "Gare de Lyon";
            StopArea existing = new StopArea("stop:123", "Gare de Lyon", new GeoPoint(48.8443, 2.3730));

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.of(existing));

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isEqualTo(existing);
            verify(primApiClient, never()).searchPlaces(anyString());
        }

        @Test
        @DisplayName("utilise PRIM quand une zone d'arrêt est trouvée")
        void findOrCreateByQuery_usesPrimWhenStopAreaFound() {
            // Given
            String query = "Châtelet";
            PrimPlace place = createMockPlaceWithStopArea("stop:456", "Châtelet", 48.8584, 2.3470);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(query)).thenReturn(List.of(place));
            when(stopAreaRepository.findByExternalId("stop:456")).thenReturn(Optional.empty());
            when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Châtelet");
            verify(stopAreaRepository).save(any(StopArea.class));
        }

        @Test
        @DisplayName("simplifie l'adresse en cas d'échec PRIM")
        void findOrCreateByQuery_simplifiesAddressOnPrimFailure() {
            // Given
            String query = "21 Place Jean Charcot";
            String simplified = "Place Jean Charcot";
            PrimPlace place = createMockPlaceWithStopArea("stop:789", "Place Jean Charcot", 48.9845, 2.3775);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(query)).thenReturn(List.of()); // Original fails
            when(primApiClient.searchPlaces(simplified)).thenReturn(List.of(place)); // Simplified works
            when(stopAreaRepository.findByExternalId("stop:789")).thenReturn(Optional.empty());
            when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isNotNull();
            verify(primApiClient, times(2)).searchPlaces(anyString());
        }

        @Test
        @DisplayName("utilise le géocodage quand PRIM échoue")
        void findOrCreateByQuery_usesGeocodingWhenPrimFails() {
            // Given
            String query = "123 Rue Example, Paris";
            GeoPoint geocodedPoint = new GeoPoint(48.8566, 2.3522);
            PrimPlace place = createMockPlaceWithStopArea("stop:nearby", "Nearby Station", 48.8570, 2.3530);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(anyString())).thenReturn(List.of());
            when(geocodingService.geocode(query)).thenReturn(geocodedPoint);
            when(geocodingService.reverseGeocode(any(GeoPoint.class))).thenReturn("Paris");
            when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), anyInt(), any()))
                    .thenReturn(List.of(place));
            when(stopAreaRepository.findByExternalId("stop:nearby")).thenReturn(Optional.empty());
            when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isNotNull();
            verify(geocodingService).geocode(query);
        }

        @Test
        @DisplayName("recherche à proximité après géocodage")
        void findOrCreateByQuery_searchesNearbyAfterGeocoding() {
            // Given
            String query = "Some Address";
            GeoPoint geocodedPoint = new GeoPoint(48.8566, 2.3522);
            PrimPlace place = createMockPlaceWithStopArea("stop:nearby", "Station", 48.8570, 2.3530);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(anyString())).thenReturn(List.of());
            when(geocodingService.geocode(query)).thenReturn(geocodedPoint);
            when(geocodingService.reverseGeocode(any())).thenReturn("Paris");
            when(primApiClient.searchPlacesNearby(eq(48.8566), eq(2.3522), anyInt(), any()))
                    .thenReturn(List.of(place));
            when(stopAreaRepository.findByExternalId("stop:nearby")).thenReturn(Optional.empty());
            when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            service.findOrCreateByQuery(query);

            // Then
            verify(primApiClient).searchPlacesNearby(eq(48.8566), eq(2.3522), eq(2000), any());
        }

        @Test
        @DisplayName("étend le rayon jusqu'au maximum")
        void findOrCreateByQuery_expandsRadiusUpToMax() {
            // Given
            String query = "Remote Location";
            GeoPoint geocodedPoint = new GeoPoint(48.0, 2.0);
            PrimPlace place = createMockPlaceWithStopArea("stop:far", "Far Station", 48.1, 2.1);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(anyString())).thenReturn(List.of());
            when(geocodingService.geocode(query)).thenReturn(geocodedPoint);
            when(geocodingService.reverseGeocode(any())).thenReturn("SomeCity");
            // First search at initial radius fails
            when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), eq(2000), any()))
                    .thenReturn(List.of());
            // Search at expanded radii
            when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), eq(5000), any()))
                    .thenReturn(List.of());
            when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), eq(10000), any()))
                    .thenReturn(List.of(place));
            when(stopAreaRepository.findByExternalId("stop:far")).thenReturn(Optional.empty());
            when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("lève une exception quand rien n'est trouvé")
        void findOrCreateByQuery_throwsWhenNothingFound() {
            // Given
            String query = "Nonexistent Place";

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(anyString())).thenReturn(List.of());
            when(geocodingService.geocode(query)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> service.findOrCreateByQuery(query))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No location found");
        }

        @Test
        @DisplayName("lève une exception avec une requête null")
        void findOrCreateByQuery_withNullQuery_throwsException() {
            // When/Then
            assertThatThrownBy(() -> service.findOrCreateByQuery(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or empty");
        }

        @Test
        @DisplayName("lève une exception avec une requête vide")
        void findOrCreateByQuery_withBlankQuery_throwsException() {
            // When/Then
            assertThatThrownBy(() -> service.findOrCreateByQuery("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Tests simplifyAddress")
    class SimplifyAddressTests {

        @Test
        @DisplayName("supprime le numéro de rue")
        void simplifyAddress_removesStreetNumber() throws Exception {
            // Given
            String address = "21 place jean charcot";

            // When - use reflection to access private method
            String result = invokeSimplifyAddress(address);

            // Then
            assertThat(result).isEqualTo("place jean charcot");
        }

        @Test
        @DisplayName("supprime le code postal et la ville")
        void simplifyAddress_removesPostalCodeAndCity() throws Exception {
            // Given
            String address = "21 place jean charcot, nanterre";

            // When
            String result = invokeSimplifyAddress(address);

            // Then
            assertThat(result).isEqualTo("place jean charcot");
        }

        @Test
        @DisplayName("gère une adresse null")
        void simplifyAddress_handlesNull() throws Exception {
            // When
            String result = invokeSimplifyAddress(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("gère une adresse vide")
        void simplifyAddress_handlesBlank() throws Exception {
            // When
            String result = invokeSimplifyAddress("   ");

            // Then
            assertThat(result).isEqualTo("   ");
        }

        private String invokeSimplifyAddress(String address) throws Exception {
            Method method = StopAreaServiceImpl.class.getDeclaredMethod("simplifyAddress", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, address);
        }
    }

    @Nested
    @DisplayName("Tests extractCityName")
    class ExtractCityNameTests {

        @Test
        @DisplayName("extrait la ville après le code postal")
        void extractCityName_extractsCityAfterPostalCode() throws Exception {
            // Given
            String areaName = "21 Place Jean Charcot 95200 Sarcelles";

            // When
            String result = invokeExtractCityName(areaName);

            // Then
            assertThat(result).isEqualTo("Sarcelles");
        }

        @Test
        @DisplayName("utilise la première partie avec des virgules")
        void extractCityName_usesFirstPartWithCommas() throws Exception {
            // Given
            String areaName = "Nanterre, Île-de-France, France";

            // When
            String result = invokeExtractCityName(areaName);

            // Then
            assertThat(result).isEqualTo("Nanterre");
        }

        @Test
        @DisplayName("gère une entrée null")
        void extractCityName_handlesNullInput() throws Exception {
            // When
            String result = invokeExtractCityName(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("gère une entrée vide")
        void extractCityName_handlesBlankInput() throws Exception {
            // When
            String result = invokeExtractCityName("   ");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("retourne tel quel sans format spécial")
        void extractCityName_returnsAsIsWithoutSpecialFormat() throws Exception {
            // Given
            String areaName = "Paris";

            // When
            String result = invokeExtractCityName(areaName);

            // Then
            assertThat(result).isEqualTo("Paris");
        }

        @Test
        @DisplayName("extrait la ville d'un format avec code postal dans une partie")
        void extractCityName_extractsCityFromPartWithPostalCode() throws Exception {
            // Given
            String areaName = "Rue Example, 75001 Paris, France";

            // When
            String result = invokeExtractCityName(areaName);

            // Then
            assertThat(result).isEqualTo("Paris");
        }

        private String invokeExtractCityName(String areaName) throws Exception {
            Method method = StopAreaServiceImpl.class.getDeclaredMethod("extractCityName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, areaName);
        }
    }

    @Nested
    @DisplayName("Tests gestion des sauvegardes concurrentes")
    class ConcurrentSaveTests {

        @Test
        @DisplayName("gère DataIntegrityViolationException en récupérant l'existant")
        void saveStopAreaIfNotExists_handlesDataIntegrityViolation() {
            // Given
            String query = "Concurrent Station";
            PrimPlace place = createMockPlaceWithStopArea("stop:concurrent", "Station", 48.8, 2.3);
            StopArea existingAfterViolation = new StopArea("stop:concurrent", "Station", new GeoPoint(48.8, 2.3));

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(query)).thenReturn(List.of(place));
            // findByExternalId is called twice before save: once in findOrCreateByQuery, once in saveStopAreaIfNotExists
            // Then after DataIntegrityViolationException, it's called again to retrieve the existing
            when(stopAreaRepository.findByExternalId("stop:concurrent"))
                    .thenReturn(Optional.empty())  // First check in findOrCreateByQuery
                    .thenReturn(Optional.empty())  // Second check in saveStopAreaIfNotExists
                    .thenReturn(Optional.of(existingAfterViolation)); // After DataIntegrityViolationException
            when(stopAreaRepository.save(any(StopArea.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate key"));

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isEqualTo(existingAfterViolation);
            verify(stopAreaRepository).save(any(StopArea.class));
        }

        @Test
        @DisplayName("retourne l'existant quand trouvé")
        void saveStopAreaIfNotExists_returnsExistingWhenFound() {
            // Given
            String query = "Existing Station";
            StopArea existing = new StopArea("stop:existing", "Station", new GeoPoint(48.8, 2.3));
            PrimPlace place = createMockPlaceWithStopArea("stop:existing", "Station", 48.8, 2.3);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(query)).thenReturn(List.of(place));
            when(stopAreaRepository.findByExternalId("stop:existing")).thenReturn(Optional.of(existing));

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isEqualTo(existing);
            verify(stopAreaRepository, never()).save(any(StopArea.class));
        }
    }

    @Nested
    @DisplayName("Tests avec StopPoint au lieu de StopArea")
    class StopPointTests {

        @Test
        @DisplayName("utilise stopPoint quand stopArea est absent")
        void findOrCreateByQuery_usesStopPointWhenNoStopArea() {
            // Given
            String query = "Bus Stop";
            PrimPlace place = createMockPlaceWithStopPoint("stoppoint:123", "Bus Stop", 48.8, 2.3);

            when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
            when(primApiClient.searchPlaces(query)).thenReturn(List.of(place));
            when(stopAreaRepository.findByExternalId("stoppoint:123")).thenReturn(Optional.empty());
            when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            StopArea result = service.findOrCreateByQuery(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getExternalId()).isEqualTo("stoppoint:123");
        }
    }

    // Helper methods
    private PrimPlace createMockPlaceWithStopArea(String id, String name, double lat, double lon) {
        PrimCoordinates coords = new PrimCoordinates(lat, lon);
        PrimStopArea stopArea = new PrimStopArea(id, name, coords);
        return new PrimPlace(id, name, "stop_area", stopArea, null, coords);
    }

    private PrimPlace createMockPlaceWithStopPoint(String id, String name, double lat, double lon) {
        PrimCoordinates coords = new PrimCoordinates(lat, lon);
        PrimStopPoint stopPoint = new PrimStopPoint(id, name, coords, null);
        return new PrimPlace(id, name, "stop_point", null, stopPoint, coords);
    }
}
