package org.marly.mavigo.service.stoparea;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimCoordinates;
import org.marly.mavigo.client.prim.PrimPlace;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StopAreaServiceImpl implements StopAreaService {

    private final StopAreaRepository stopAreaRepository;
    private final PrimApiClient primApiClient;

    public StopAreaServiceImpl(StopAreaRepository stopAreaRepository, PrimApiClient primApiClient) {
        this.stopAreaRepository = stopAreaRepository;
        this.primApiClient = primApiClient;
    }

    @Override
    @Transactional
    public StopArea findOrCreateByQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        Optional<StopArea> existing = stopAreaRepository.findByNameIgnoreCase(query.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        List<PrimPlace> places = primApiClient.searchPlaces(query);
        
        if (places.isEmpty()) {
            throw new IllegalArgumentException("No places found for query: " + query);
        }

        PrimPlace firstPlace = places.get(0);
        if (firstPlace.stopArea() == null || firstPlace.stopArea().id() == null) {
            throw new IllegalArgumentException("No stop area found in place: " + query);
        }

        String stopAreaId = firstPlace.stopArea().id();
        
        Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
        if (existingById.isPresent()) {
            return existingById.get();
        }

        saveStopAreas(places);
        return stopAreaRepository.findByExternalId(stopAreaId)
                .orElseThrow(() -> new IllegalStateException("Stop area not saved: " + stopAreaId));
    }

    @Override
    @Transactional
    public StopArea findByExternalId(String externalId) {
        Optional<StopArea> existing = stopAreaRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<PrimPlace> places = primApiClient.searchPlaces(externalId);
        saveStopAreas(places);
        
        for (PrimPlace place : places) {
            if (place.stopArea() != null && externalId.equals(place.stopArea().id())) {
                return stopAreaRepository.findByExternalId(externalId)
                        .orElseThrow(() -> new IllegalStateException("Stop area not saved: " + externalId));
            }
        }

        throw new IllegalArgumentException("Stop area not found: " + externalId);
    }

    @Transactional
    public void saveStopAreas(List<PrimPlace> places) {
        for (PrimPlace place : places) {
            if (place.stopArea() != null && place.stopArea().id() != null) {
                String stopAreaId = place.stopArea().id();
                if (stopAreaRepository.findByExternalId(stopAreaId).isEmpty()) {
                    saveStopArea(place);
                }
            }
        }
    }

    private StopArea saveStopArea(PrimPlace place) {
        String stopAreaId = place.stopArea().id();
        String name = place.stopArea().name();
        
        PrimCoordinates coords = place.stopArea().coordinates();
        GeoPoint geoPoint = coords != null && coords.latitude() != null && coords.longitude() != null
                ? new GeoPoint(coords.latitude(), coords.longitude())
                : null;
        
        StopArea stopArea = new StopArea(stopAreaId, name, geoPoint);
        return stopAreaRepository.save(stopArea);
    }
}

