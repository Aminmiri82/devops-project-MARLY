package org.marly.mavigo.service.stoparea;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.marly.mavigo.service.geocoding.GeocodingService;

@DisplayName("Tests unitaires - StopAreaService")
class StopAreaServiceTest {

    private StopAreaRepository stopAreaRepository;
    private PrimApiClient primApiClient;
    private GeocodingService geocodingService;
    private StopAreaServiceImpl service;

    @BeforeEach
    void setUp() {
        stopAreaRepository = mock(StopAreaRepository.class);
        primApiClient = mock(PrimApiClient.class);
        geocodingService = mock(GeocodingService.class);

        service = new StopAreaServiceImpl(stopAreaRepository, primApiClient, geocodingService);
    }

    @Test
    @DisplayName("findOrCreateByQuery devrait retourner une zone d'arrêt existante")
    void findOrCreateByQuery_shouldReturnExistingStopArea() {
        // Given
        String query = "Gare de Lyon";
        StopArea existingStopArea = new StopArea("stop:123", "Gare de Lyon", new GeoPoint(48.8443, 2.3730));

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.of(existingStopArea));

        // When
        StopArea result = service.findOrCreateByQuery(query);

        // Then
        assertNotNull(result);
        assertEquals("Gare de Lyon", result.getName());
        verify(primApiClient, never()).searchPlaces(anyString());
    }

    @Test
    @DisplayName("findOrCreateByQuery devrait créer une nouvelle zone d'arrêt depuis PRIM")
    void findOrCreateByQuery_shouldCreateNewStopAreaFromPrim() {
        // Given
        String query = "Châtelet";
        PrimPlace mockPlace = createMockPlaceWithStopArea("stop:456", "Châtelet", 48.8584, 2.3470);

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(List.of(mockPlace));
        when(stopAreaRepository.findByExternalId("stop:456")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        StopArea result = service.findOrCreateByQuery(query);

        // Then
        assertNotNull(result);
        assertEquals("Châtelet", result.getName());
        verify(stopAreaRepository).save(any(StopArea.class));
    }

    @Test
    @DisplayName("findOrCreateByQuery devrait lever une exception si la requête est nulle")
    void findOrCreateByQuery_shouldThrowExceptionWhenQueryIsNull() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(null));
    }

    @Test
    @DisplayName("findOrCreateByQuery devrait lever une exception si la requête est vide")
    void findOrCreateByQuery_shouldThrowExceptionWhenQueryIsEmpty() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(""));
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery("   "));
    }

    @Test
    @DisplayName("findOrCreateByQuery devrait utiliser le géocodage si PRIM ne trouve rien")
    void findOrCreateByQuery_shouldUseGeocodingWhenPrimFindsNothing() {
        // Given
        String query = "21 place Jean Charcot, Sarcelles";
        GeoPoint geocodedPoint = new GeoPoint(48.9845, 2.3775);
        PrimPlace mockPlace = createMockPlaceWithStopArea("stop:789", "Sarcelles Gare", 48.9850, 2.3780);

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(List.of());
        when(primApiClient.searchPlaces(anyString())).thenReturn(List.of()); // For simplified query
        when(geocodingService.geocode(query)).thenReturn(geocodedPoint);
        when(geocodingService.reverseGeocode(any(GeoPoint.class))).thenReturn("Sarcelles");
        when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), anyInt(), any()))
                .thenReturn(List.of(mockPlace));
        when(stopAreaRepository.findByExternalId("stop:789")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        StopArea result = service.findOrCreateByQuery(query);

        // Then
        assertNotNull(result);
        verify(geocodingService).geocode(query);
    }

    @Test
    @DisplayName("findByExternalId devrait retourner une zone d'arrêt existante")
    void findByExternalId_shouldReturnExistingStopArea() {
        // Given
        String externalId = "stop:123";
        StopArea existingStopArea = new StopArea(externalId, "Gare de Lyon", new GeoPoint(48.8443, 2.3730));

        when(stopAreaRepository.findByExternalId(externalId)).thenReturn(Optional.of(existingStopArea));

        // When
        StopArea result = service.findByExternalId(externalId);

        // Then
        assertNotNull(result);
        assertEquals(externalId, result.getExternalId());
        verify(primApiClient, never()).searchPlaces(anyString());
    }

    @Test
    @DisplayName("findByExternalId devrait rechercher via PRIM si non trouvé localement")
    void findByExternalId_shouldSearchPrimWhenNotFoundLocally() {
        // Given
        String externalId = "stop:456";
        PrimPlace mockPlace = createMockPlaceWithStopArea(externalId, "Nation", 48.8483, 2.3952);

        when(stopAreaRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(externalId)).thenReturn(List.of(mockPlace));
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        StopArea result = service.findByExternalId(externalId);

        // Then
        assertNotNull(result);
        verify(primApiClient).searchPlaces(externalId);
        verify(stopAreaRepository).save(any(StopArea.class));
    }

    @Test
    @DisplayName("findByExternalId devrait lever une exception si non trouvé")
    void findByExternalId_shouldThrowExceptionWhenNotFound() {
        // Given
        String externalId = "stop:nonexistent";

        when(stopAreaRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(externalId)).thenReturn(List.of());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.findByExternalId(externalId));
    }

    // Helper methods

    private PrimPlace createMockPlaceWithStopArea(String id, String name, double lat, double lon) {
        PrimCoordinates coords = new PrimCoordinates(lat, lon);
        PrimStopArea stopArea = new PrimStopArea(id, name, coords);
        return new PrimPlace(id, name, "stop_area", stopArea, null, coords);
    }
}
