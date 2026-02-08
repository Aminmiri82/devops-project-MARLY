package org.marly.mavigo.service.stoparea;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopAreaServiceImplTest {

    @Mock
    private StopAreaRepository stopAreaRepository;

    @Mock
    private PrimApiClient primApiClient;

    @InjectMocks
    private StopAreaServiceImpl service;

    // ========== findOrCreateByQuery tests ==========

    @Test
    void findOrCreateByQuery_ShouldReturnExisting() {
        StopArea existing = new StopArea("id", "name", null);
        when(stopAreaRepository.findByNameIgnoreCase("query")).thenReturn(Optional.of(existing));

        StopArea result = service.findOrCreateByQuery("query");

        assertEquals(existing, result);
        verifyNoInteractions(primApiClient);
    }

    @Test
    void findOrCreateByQuery_ShouldCallApiAndSave_WhenNotExists() {
        when(stopAreaRepository.findByNameIgnoreCase("query")).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea("stop-1", "name", new PrimCoordinates(10.0, 10.0));
        PrimPlace place = new PrimPlace("id", "name", "stop_area", primStopArea);
        when(primApiClient.searchPlaces("query")).thenReturn(List.of(place));
        
        when(stopAreaRepository.findByExternalId("stop-1")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StopArea result = service.findOrCreateByQuery("query");

        assertNotNull(result);
        assertEquals("stop-1", result.getExternalId());
        verify(stopAreaRepository).save(any());
    }
    
    @Test
    void findOrCreateByQuery_ShouldHandleConcurrency() {
        when(stopAreaRepository.findByNameIgnoreCase("query")).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea("stop-1", "name", null);
        PrimPlace place = new PrimPlace("id", "name", "stop_area", primStopArea);
        when(primApiClient.searchPlaces("query")).thenReturn(List.of(place));
        
        when(stopAreaRepository.findByExternalId("stop-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new StopArea("stop-1", "name", null)));
        
        when(stopAreaRepository.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

        StopArea result = service.findOrCreateByQuery("query");

        assertNotNull(result);
        assertEquals("stop-1", result.getExternalId());
        verify(stopAreaRepository, times(3)).findByExternalId("stop-1");
    }

    @Test
    void findOrCreateByQuery_ShouldThrowWhenQueryIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery(null));
    }

    @Test
    void findOrCreateByQuery_ShouldThrowWhenQueryIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery("   "));
    }

    @Test
    void findOrCreateByQuery_ShouldThrowWhenNoPlacesFound() {
        when(stopAreaRepository.findByNameIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces("unknown")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery("unknown"));
    }

    @Test
    void findOrCreateByQuery_ShouldThrowWhenPlaceHasNoStopArea() {
        when(stopAreaRepository.findByNameIgnoreCase("query")).thenReturn(Optional.empty());
        PrimPlace place = new PrimPlace("id", "name", "stop_area", null);
        when(primApiClient.searchPlaces("query")).thenReturn(List.of(place));

        assertThrows(IllegalArgumentException.class, () -> service.findOrCreateByQuery("query"));
    }

    @Test
    void findOrCreateByQuery_ShouldReturnExistingById_WhenFoundByExternalId() {
        when(stopAreaRepository.findByNameIgnoreCase("query")).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea("stop-1", "name", null);
        PrimPlace place = new PrimPlace("id", "name", "stop_area", primStopArea);
        when(primApiClient.searchPlaces("query")).thenReturn(List.of(place));
        
        StopArea existing = new StopArea("stop-1", "name", null);
        when(stopAreaRepository.findByExternalId("stop-1")).thenReturn(Optional.of(existing));

        StopArea result = service.findOrCreateByQuery("query");

        assertEquals(existing, result);
        verify(stopAreaRepository, never()).save(any());
    }

    @Test
    void findOrCreateByQuery_ShouldSaveMultiplePlaces() {
        when(stopAreaRepository.findByNameIgnoreCase("query")).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea1 = new PrimStopArea("stop-1", "name1", new PrimCoordinates(10.0, 10.0));
        PrimPlace place1 = new PrimPlace("id1", "name1", "stop_area", primStopArea1);
        PrimStopArea primStopArea2 = new PrimStopArea("stop-2", "name2", new PrimCoordinates(20.0, 20.0));
        PrimPlace place2 = new PrimPlace("id2", "name2", "stop_area", primStopArea2);
        
        when(primApiClient.searchPlaces("query")).thenReturn(List.of(place1, place2));
        when(stopAreaRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StopArea result = service.findOrCreateByQuery("query");

        assertNotNull(result);
        verify(stopAreaRepository, times(2)).save(any());
    }

    // ========== findByExternalId tests ==========

    @Test
    void findByExternalId_ShouldReturnExisting() {
        StopArea existing = new StopArea("id", "name", null);
        when(stopAreaRepository.findByExternalId("id")).thenReturn(Optional.of(existing));

        StopArea result = service.findByExternalId("id");

        assertEquals(existing, result);
    }

    @Test
    void findByExternalId_ShouldSearchApiWhenNotExists() {
        when(stopAreaRepository.findByExternalId("stop-1")).thenReturn(Optional.empty(), Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea("stop-1", "name", new PrimCoordinates(10.0, 10.0));
        PrimPlace place = new PrimPlace("id", "name", "stop_area", primStopArea);
        when(primApiClient.searchPlaces("stop-1")).thenReturn(List.of(place));
        when(stopAreaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StopArea result = service.findByExternalId("stop-1");

        assertNotNull(result);
        assertEquals("stop-1", result.getExternalId());
    }

    @Test
    void findByExternalId_ShouldThrowWhenNotFoundInApi() {
        when(stopAreaRepository.findByExternalId("unknown")).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces("unknown")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.findByExternalId("unknown"));
    }

    @Test
    void findByExternalId_ShouldThrowWhenNoMatchingStop() {
        when(stopAreaRepository.findByExternalId("target")).thenReturn(Optional.empty());
        
        PrimStopArea primStopArea = new PrimStopArea("different-id", "name", null);
        PrimPlace place = new PrimPlace("id", "name", "stop_area", primStopArea);
        when(primApiClient.searchPlaces("target")).thenReturn(List.of(place));

        assertThrows(IllegalArgumentException.class, () -> service.findByExternalId("target"));
    }

    // ========== saveStopAreas tests ==========

    @Test
    void saveStopAreas_ShouldSavePlacesWithValidStopArea() {
        PrimStopArea primStopArea1 = new PrimStopArea("stop-1", "name1", new PrimCoordinates(10.0, 10.0));
        PrimPlace place1 = new PrimPlace("id1", "name1", "stop_area", primStopArea1);
        PrimPlace placeNoStop = new PrimPlace("id2", "name2", "stop_area", null);
        
        when(stopAreaRepository.findByExternalId("stop-1")).thenReturn(Optional.empty());
        when(stopAreaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveStopAreas(List.of(place1, placeNoStop));

        verify(stopAreaRepository, times(1)).save(any());
    }

    @Test
    void saveStopAreas_ShouldSkipExistingStops() {
        PrimStopArea primStopArea = new PrimStopArea("stop-1", "name", null);
        PrimPlace place = new PrimPlace("id", "name", "stop_area", primStopArea);
        
        when(stopAreaRepository.findByExternalId("stop-1")).thenReturn(Optional.of(new StopArea("stop-1", "name", null)));

        service.saveStopAreas(List.of(place));

        verify(stopAreaRepository, never()).save(any());
    }
}

