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
                .thenReturn(Optional.empty()) // 1. check in findOrCreateByQuery
                .thenReturn(Optional.empty()) // 2. check in saveStopAreaIfNotExists
                .thenReturn(Optional.of(new StopArea("stop-1", "name", null))); // 3. recovery in catch block
        
        // Throw exception on first save attempt
        when(stopAreaRepository.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

        StopArea result = service.findOrCreateByQuery("query");

        assertNotNull(result);
        assertEquals("stop-1", result.getExternalId());
        // Should have called findByExternalId 3 times (check, check before save, recovery)
        verify(stopAreaRepository, times(3)).findByExternalId("stop-1");
    }

    @Test
    void findByExternalId_ShouldReturnExisting() {
        StopArea existing = new StopArea("id", "name", null);
        when(stopAreaRepository.findByExternalId("id")).thenReturn(Optional.of(existing));

        StopArea result = service.findByExternalId("id");

        assertEquals(existing, result);
    }
}
