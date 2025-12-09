package org.marly.mavigo.service.stoparea;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimCoordinates;
import org.marly.mavigo.client.prim.PrimPlace;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        }// early return if the stop area already exists

        List<PrimPlace> places = primApiClient.searchPlaces(query);
        
        if (places.isEmpty()) {
            throw new IllegalArgumentException("No places found for query: " + query);
        }

        PrimPlace firstPlace = places.get(0);
        if (firstPlace.stopArea() == null || firstPlace.stopArea().id() == null) {
            throw new IllegalArgumentException("No stop area found in place: " + query);
        }

        String stopAreaId = firstPlace.stopArea().id();
        
        // checks if the stop area already exists by id
        Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
        if (existingById.isPresent()) {
            return existingById.get();
        }

        // Save the first place we need and return it, then save the rest
        StopArea saved = saveStopAreaIfNotExists(firstPlace);
        // Save remaining places (skip first since it's already saved)
        for (int i = 1; i < places.size(); i++) {
            PrimPlace place = places.get(i);
            if (place.stopArea() != null && place.stopArea().id() != null) {
                saveStopAreaIfNotExists(place);
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public StopArea findByExternalId(String externalId) {
        Optional<StopArea> existing = stopAreaRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<PrimPlace> places = primApiClient.searchPlaces(externalId);
        
        // Find and save the matching place first, then save the rest
        PrimPlace matchingPlace = null;
        for (PrimPlace place : places) {
            if (place.stopArea() != null && externalId.equals(place.stopArea().id())) {
                matchingPlace = place;
                break;
            }
        }
        
        if (matchingPlace == null) {
            throw new IllegalArgumentException("Stop area not found: " + externalId);
        }
        
        // Save the matching place and return it
        StopArea saved = saveStopAreaIfNotExists(matchingPlace);
        
        // Save remaining places
        for (PrimPlace place : places) {
            if (place != matchingPlace && place.stopArea() != null && place.stopArea().id() != null) {
                saveStopAreaIfNotExists(place);
            }
        }
        
        return saved;
    }

    @Transactional
    public void saveStopAreas(List<PrimPlace> places) {
        for (PrimPlace place : places) {
            if (place.stopArea() != null && place.stopArea().id() != null) {
                saveStopAreaIfNotExists(place);
            }
        }
    }

    /**
     * Saves a stop area if it doesn't exist, handling concurrent save attempts gracefully.
     * Returns the saved or existing stop area.
     * 
     * @param place The PrimPlace containing stop area information
     * @return The saved or existing StopArea
     */
    private StopArea saveStopAreaIfNotExists(PrimPlace place) {
        if (place.stopArea() == null || place.stopArea().id() == null) {
            throw new IllegalArgumentException("Place must have a valid stop area with ID");
        }
        
        String stopAreaId = place.stopArea().id();
        
        // Check if it already exists
        Optional<StopArea> existing = stopAreaRepository.findByExternalId(stopAreaId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Try to save, handling potential concurrent saves
        try {
            return saveStopArea(place);
        } catch (DataIntegrityViolationException e) {
            // Another thread may have inserted it concurrently, fetch it
            return stopAreaRepository.findByExternalId(stopAreaId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Stop area was not saved and could not be found: " + stopAreaId, e));
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

