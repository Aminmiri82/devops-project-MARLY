package org.marly.mavigo.service.stoparea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.client.prim.model.PrimStopPoint;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.marly.mavigo.service.geocoding.GeocodingService;
import org.springframework.dao.DataIntegrityViolationException;

class StopAreaServiceImplAdvancedTest {

    private StopAreaRepository stopAreaRepository;
    private PrimApiClient primApiClient;
    private GeocodingService geocodingService;
    private StopAreaServiceImpl service;

    @BeforeEach
    void setUp() {
        stopAreaRepository = org.mockito.Mockito.mock(StopAreaRepository.class);
        primApiClient = org.mockito.Mockito.mock(PrimApiClient.class);
        geocodingService = org.mockito.Mockito.mock(GeocodingService.class);
        service = new StopAreaServiceImpl(stopAreaRepository, primApiClient, geocodingService);
    }

    @Test
    void findByExternalId_throwsWhenPrimHasNoMatchingExternalId() {
        when(stopAreaRepository.findByExternalId("target-id")).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces("target-id")).thenReturn(List.of(stopAreaPlace("other-id", "Other")));

        assertThrows(IllegalArgumentException.class, () -> service.findByExternalId("target-id"));
    }

    @Test
    void saveStopAreas_savesOnlyPlacesWithStopAreaOrStopPoint() {
        PrimPlace validStopArea = stopAreaPlace("sa-1", "Station A");
        PrimPlace validStopPoint = stopPointPlace("sp-1", "Point B");
        PrimPlace invalid = new PrimPlace("x", "invalid", "address", null, null, null);

        when(stopAreaRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(inv -> inv.getArgument(0));

        service.saveStopAreas(List.of(validStopArea, invalid, validStopPoint));

        verify(stopAreaRepository, times(2)).save(any(StopArea.class));
    }

    @Test
    void findOrCreateByQuery_returnsExistingByIdFromNearbySearch() {
        String query = "Unknown Place";
        GeoPoint geocoded = new GeoPoint(48.8566, 2.3522);
        PrimPlace nearby = stopAreaPlace("sa-existing", "Existing Station");
        StopArea existing = new StopArea("sa-existing", "Existing Station", new GeoPoint(48.85, 2.35));

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(Collections.emptyList());
        when(geocodingService.geocode(query)).thenReturn(geocoded);
        when(geocodingService.reverseGeocode(geocoded)).thenReturn("Paris, France");
        when(primApiClient.searchPlacesNearby(eq(48.8566), eq(2.3522), eq(2000), anyString()))
                .thenReturn(List.of(nearby));
        when(stopAreaRepository.findByExternalId("sa-existing")).thenReturn(Optional.of(existing));

        StopArea result = service.findOrCreateByQuery(query);

        assertSame(existing, result);
        verify(stopAreaRepository, never()).save(any());
    }

    @Test
    void findOrCreateByQuery_handlesFirstReverseGeocodeException_thenThrowsIfNothingFound() {
        String query = "No Transit Here";
        GeoPoint geocoded = new GeoPoint(48.0, 2.0);

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(Collections.emptyList());
        when(geocodingService.geocode(query)).thenReturn(geocoded);
        when(geocodingService.reverseGeocode(geocoded))
                .thenThrow(new RuntimeException("temporary reverse failure"))
                .thenReturn("No stop zone, France");
        when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(primApiClient.searchPlaces("No stop zone")).thenReturn(Collections.emptyList());
        when(primApiClient.searchPlaces("No stop zone, France")).thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(query));
    }

    @Test
    void findOrCreateByQuery_usesDirectSearchFallbackWithFirstValidPlaceWithoutCoordinates() {
        String query = "Fallback Address";
        GeoPoint geocoded = new GeoPoint(48.9, 2.3);
        PrimPlace noCoordValid = stopAreaPlace("sa-direct", "Direct Stop");

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(Collections.emptyList());
        when(geocodingService.geocode(query)).thenReturn(geocoded);
        when(geocodingService.reverseGeocode(geocoded)).thenReturn("Direct City, Region");
        when(primApiClient.searchPlacesNearby(anyDouble(), anyDouble(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(primApiClient.searchPlaces("Direct City")).thenReturn(List.of(noCoordValid));
        when(stopAreaRepository.findByExternalId("sa-direct")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(inv -> inv.getArgument(0));

        StopArea result = service.findOrCreateByQuery(query);

        assertNotNull(result);
        assertEquals("sa-direct", result.getExternalId());
    }

    @Test
    void saveStopAreas_throwsIllegalStateWhenConcurrentInsertCannotBeReadBack() {
        PrimPlace place = stopAreaPlace("sa-race", "Race Stop");

        when(stopAreaRepository.findByExternalId("sa-race")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any(StopArea.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(IllegalStateException.class, () -> service.saveStopAreas(List.of(place)));
    }

    private PrimPlace stopAreaPlace(String id, String name) {
        PrimCoordinates coordinates = new PrimCoordinates(48.8566, 2.3522);
        return new PrimPlace(id, name, "stop_area", new PrimStopArea(id, name, coordinates), null, coordinates);
    }

    private PrimPlace stopPointPlace(String id, String name) {
        PrimCoordinates coordinates = new PrimCoordinates(48.8590, 2.3550);
        PrimStopPoint stopPoint = new PrimStopPoint(id, name, coordinates, new PrimStopArea("sa-parent", "Parent", coordinates));
        return new PrimPlace(id, name, "stop_point", null, stopPoint, coordinates);
    }
}
