package org.marly.mavigo.client.prim;

import java.util.List;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.model.PrimPlace;

public interface PrimApiClient {

    List<PrimPlace> searchPlaces(String query);

    /**
     * Cherche les arrêts de transport proches d'une coordonnée GPS.
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @param radiusMeters Rayon de recherche en mètres (par défaut 500m)
     * @return Liste des places (arrêts de transport) proches
     */
    List<PrimPlace> searchPlacesNearby(double latitude, double longitude, int radiusMeters);
    
    /**
     * Cherche les arrêts de transport proches d'une coordonnée GPS en utilisant un nom de ville/quartier.
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @param radiusMeters Rayon de recherche en mètres
     * @param cityName Nom de la ville/quartier pour la recherche textuelle dans PRIM
     * @return Liste des places (arrêts de transport) proches
     */
    List<PrimPlace> searchPlacesNearby(double latitude, double longitude, int radiusMeters, String cityName);

    List<PrimJourneyPlanDto> calculateJourneyPlans(PrimJourneyRequest request);
}
