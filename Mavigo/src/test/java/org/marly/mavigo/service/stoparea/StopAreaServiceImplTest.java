package org.marly.mavigo.service.stoparea;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.marly.mavigo.service.geocoding.GeocodingService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StopAreaServiceImplTest {

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
    void findOrCreateByQuery_shouldReturnExistingStopArea() {
        String query = "Gare de Lyon";
        StopArea existing = new StopArea("id", query, null);
        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.of(existing));

        StopArea result = service.findOrCreateByQuery(query);

        assertEquals(existing, result);
        verify(primApiClient, never()).searchPlaces(anyString());
    }

    @Test
    void findOrCreateByQuery_shouldSimplifyAddressIfNoResults() {
        String query = "123 Rue de Rivoli, Paris";
        String simplified = "Rue de Rivoli";
        
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(Collections.emptyList());
        
        PrimStopArea primStopArea = new PrimStopArea("ext-2", "Rivoli Stop", null);
        PrimPlace place = new PrimPlace("ext-2", "Rivoli", "stop_area", primStopArea, null, null);
        when(primApiClient.searchPlaces(simplified)).thenReturn(List.of(place));
        when(stopAreaRepository.findByExternalId("ext-2")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);

        assertNotNull(result);
        assertEquals("ext-2", result.getExternalId());
        verify(primApiClient).searchPlaces(simplified);
    }

    @Test
    void findOrCreateByQuery_shouldFallbackToGeocodingAndNearbySearch() {
        String query = "Some unknown address";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        
        org.marly.mavigo.models.shared.GeoPoint geoPoint = new org.marly.mavigo.models.shared.GeoPoint(48.8566, 2.3522);
        when(geocodingService.geocode(query)).thenReturn(geoPoint);
        when(geocodingService.reverseGeocode(geoPoint)).thenReturn("Paris, France");
        
        PrimStopArea primStopArea = new PrimStopArea("ext-3", "Paris Central Station", null);
        PrimPlace place = new PrimPlace("ext-3", "Paris Central", "stop_area", primStopArea, null, null);
        when(primApiClient.searchPlacesNearby(eq(48.8566), eq(2.3522), anyInt(), anyString()))
                .thenReturn(List.of(place));
        when(stopAreaRepository.findByExternalId("ext-3")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);

        assertNotNull(result);
        assertEquals("ext-3", result.getExternalId());
    }

    @Test
    void findOrCreateByQuery_shouldThrowExceptionIfNothingFound() {
        String query = "Nowhere land";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        when(geocodingService.geocode(query)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(query));
    }

    @Test
    void findByExternalId_shouldSearchAndSaveIfNotExists() {
        String externalId = "ext-4";
        when(stopAreaRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea(externalId, "New Station", null);
        PrimPlace place = new PrimPlace(externalId, "New Station", "stop_area", primStopArea, null, null);
        when(primApiClient.searchPlaces(externalId)).thenReturn(List.of(place));
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findByExternalId(externalId);

        assertNotNull(result);
        assertEquals(externalId, result.getExternalId());
    }

    @Test
    void findOrCreateByQuery_shouldThrowExceptionOnEmptyQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(""));
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(null));
    }

    @Test
    void findOrCreateByQuery_shouldExpandRadius_whenInitialSearchFails() {
        String query = "Remote address";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        
        org.marly.mavigo.models.shared.GeoPoint geoPoint = new org.marly.mavigo.models.shared.GeoPoint(48.0, 2.0);
        when(geocodingService.geocode(query)).thenReturn(geoPoint);
        when(geocodingService.reverseGeocode(geoPoint)).thenReturn("Remote City, Country");
        
        // Initial search (2000m) -> empty
        when(primApiClient.searchPlacesNearby(eq(48.0), eq(2.0), eq(2000), anyString()))
                .thenReturn(Collections.emptyList());
        
        // Secondary search (5000m) -> found
        PrimStopArea primStopArea = new PrimStopArea("ext-remote", "Remote Station", null);
        PrimPlace place = new PrimPlace("ext-remote", "Remote Station", "stop_area", primStopArea, null, null);
        
        // Using specific matchers to differentiate calls
        when(primApiClient.searchPlacesNearby(eq(48.0), eq(2.0), eq(5000), anyString()))
                .thenReturn(List.of(place));
                
        when(stopAreaRepository.findByExternalId("ext-remote")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);

        assertNotNull(result);
        assertEquals("ext-remote", result.getExternalId());
    }

    @Test
    void findOrCreateByQuery_shouldFallbackToDirectSearch_whenRadiusSearchFails() {
        String query = "Very Remote address";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        
        org.marly.mavigo.models.shared.GeoPoint geoPoint = new org.marly.mavigo.models.shared.GeoPoint(49.0, 3.0);
        when(geocodingService.geocode(query)).thenReturn(geoPoint);
        when(geocodingService.reverseGeocode(geoPoint)).thenReturn("CityName");

        // All radius searches return empty
        when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        
        // Direct search with city name returns result
        PrimStopArea primStopArea = new PrimStopArea("ext-direct", "City Station", null);
        PrimCoordinates coords = new PrimCoordinates(49.01, 3.01); 
        PrimPlace place = new PrimPlace("ext-direct", "City Station", "stop_area", primStopArea, null, coords);
        
        // Mock search for "CityName"
        // Note: The logic in impl might try "CityName" AND "CityName" (duplicate in array) or logic slightly different
        // but searchPlaces("CityName") is expected call.
        when(primApiClient.searchPlaces("CityName")).thenReturn(List.of(place));
        
        when(stopAreaRepository.findByExternalId("ext-direct")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);

        assertNotNull(result);
        assertEquals("ext-direct", result.getExternalId());
        verify(primApiClient).searchPlaces("CityName");
    }

    @Test
    void findOrCreateByQuery_shouldHandleBANFormattedCityExtraction() {
        String query = "21 Place Jean Charcot 95200 Sarcelles";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        
        org.marly.mavigo.models.shared.GeoPoint geoPoint = new org.marly.mavigo.models.shared.GeoPoint(48.99, 2.37);
        when(geocodingService.geocode(anyString())).thenReturn(geoPoint);
        when(geocodingService.reverseGeocode(geoPoint)).thenReturn("21 Place Jean Charcot 95200 Sarcelles");
        
        PrimStopArea primStopArea = new PrimStopArea("sa-ban", "Sarcelles Station", null);
        PrimPlace place = new PrimPlace("sa-ban", "Sarcelles Station", "stop_area", primStopArea, null, null);
        
        // The service should extract "Sarcelles" and search nearby
        when(primApiClient.searchPlacesNearby(eq(48.99), eq(2.37), anyInt(), eq("Sarcelles")))
                .thenReturn(List.of(place));
        when(stopAreaRepository.findByExternalId("sa-ban")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);
        assertNotNull(result);
        assertEquals("sa-ban", result.getExternalId());
    }

    @Test
    void findOrCreateByQuery_shouldHandleCommaFormattedCityExtraction() {
        String query = "Nanterre, Hauts-de-Seine, France";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        
        org.marly.mavigo.models.shared.GeoPoint geoPoint = new org.marly.mavigo.models.shared.GeoPoint(48.89, 2.21);
        when(geocodingService.geocode(anyString())).thenReturn(geoPoint);
        when(geocodingService.reverseGeocode(geoPoint)).thenReturn("Nanterre, France");
        
        PrimStopArea primStopArea = new PrimStopArea("sa-nan", "Nanterre Station", null);
        PrimPlace place = new PrimPlace("sa-nan", "Nanterre Station", "stop_area", primStopArea, null, null);
        
        // Extraction should find "Nanterre"
        when(primApiClient.searchPlacesNearby(eq(48.89), eq(2.21), anyInt(), eq("Nanterre")))
                .thenReturn(List.of(place));
        when(stopAreaRepository.findByExternalId("sa-nan")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);
        assertEquals("sa-nan", result.getExternalId());
    }

    @Test
    void findOrCreateByQuery_shouldUseIterativeSearchWithIncreasingRadius() {
        String query = "Deep in the woods";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(anyString())).thenReturn(Collections.emptyList());
        
        org.marly.mavigo.models.shared.GeoPoint geoPoint = new org.marly.mavigo.models.shared.GeoPoint(45.0, 1.0);
        when(geocodingService.geocode(anyString())).thenReturn(geoPoint);
        when(geocodingService.reverseGeocode(geoPoint)).thenReturn("Deep Woods, Some Region");

        // Initial search nearby (2000m) returns nothing
        when(primApiClient.searchPlacesNearby(eq(45.0), eq(1.0), eq(2000), anyString()))
                .thenReturn(Collections.emptyList());
        
        // Iterative search: 5000m, 10000m, 15000m, 20000m
        // Let's say it finds something at 15000m
        when(primApiClient.searchPlacesNearby(eq(45.0), eq(1.0), eq(5000), anyString()))
                .thenReturn(Collections.emptyList());
        when(primApiClient.searchPlacesNearby(eq(45.0), eq(1.0), eq(10000), anyString()))
                .thenReturn(Collections.emptyList());
        
        PrimStopArea primStopArea = new PrimStopArea("sa-woods", "Forest Station", null);
        PrimPlace place = new PrimPlace("sa-woods", "Forest", "stop_area", primStopArea, null, null);
        when(primApiClient.searchPlacesNearby(eq(45.0), eq(1.0), eq(15000), anyString()))
                .thenReturn(List.of(place));

        when(stopAreaRepository.findByExternalId("sa-woods")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(i -> i.getArguments()[0]);

        StopArea result = service.findOrCreateByQuery(query);
        assertEquals("sa-woods", result.getExternalId());
    }

    @Test
    void findOrCreateByQuery_shouldHandleConcurrentSaves() {
        String query = "Gare du Nord";
        when(stopAreaRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea("sa-gdn", "Gare du Nord", null);
        PrimPlace place = new PrimPlace("sa-gdn", "Gare du Nord", "stop_area", primStopArea, null, null);
        when(primApiClient.searchPlaces(query)).thenReturn(List.of(place));
        
        when(stopAreaRepository.findByExternalId("sa-gdn")).thenReturn(Optional.empty());
        
        // Simulate race condition: DataIntegrityViolationException on first save
        when(stopAreaRepository.save(any(StopArea.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"))
                .thenAnswer(i -> i.getArguments()[0]);
        
        // After exception, service should fetch it again
        StopArea existing = new StopArea("sa-gdn", "Gare du Nord", null);
        when(stopAreaRepository.findByExternalId("sa-gdn")).thenReturn(Optional.of(existing));

        StopArea result = service.findOrCreateByQuery(query);
        assertNotNull(result);
        assertEquals("sa-gdn", result.getExternalId());
    }
}
