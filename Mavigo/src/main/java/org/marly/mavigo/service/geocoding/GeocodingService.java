package org.marly.mavigo.service.geocoding;

import org.marly.mavigo.models.shared.GeoPoint;

public interface GeocodingService {
    /**
     * Convertit une adresse en coordonnées GPS.
     * 
     * @param address L'adresse à géocoder (ex: "21 place jean charcot, nanterre")
     * @return Les coordonnées GPS, ou null si l'adresse n'a pas pu être géocodée
     */
    GeoPoint geocode(String address);
    
    /**
     * Effectue un géocodage inversé pour obtenir le nom du lieu depuis des coordonnées.
     * 
     * @param point Les coordonnées GPS
     * @return Le nom du lieu (quartier, ville, etc.), ou null si le géocodage inversé a échoué
     */
    String reverseGeocode(GeoPoint point);
}
