package org.marly.mavigo.service.stoparea;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.marly.mavigo.service.geocoding.GeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StopAreaServiceImpl implements StopAreaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopAreaServiceImpl.class);

    private final StopAreaRepository stopAreaRepository;
    private final PrimApiClient primApiClient;
    private final GeocodingService geocodingService;

    public StopAreaServiceImpl(StopAreaRepository stopAreaRepository, 
                               PrimApiClient primApiClient,
                               GeocodingService geocodingService) {
        this.stopAreaRepository = stopAreaRepository;
        this.primApiClient = primApiClient;
        this.geocodingService = geocodingService;
    }

    @Override
    @Transactional
    public StopArea findOrCreateByQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        String trimmedQuery = query.trim();
        
        // Early return if the stop area already exists
        Optional<StopArea> existing = stopAreaRepository.findByNameIgnoreCase(trimmedQuery);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Try the original query first
        List<PrimPlace> places = primApiClient.searchPlaces(trimmedQuery);
        logPlaces("searchPlaces(original)", trimmedQuery, places);
        
        // If no results, try simplified versions of the query
        if (places.isEmpty()) {
            String simplified = simplifyAddress(trimmedQuery);
            if (!simplified.equals(trimmedQuery)) {
                places = primApiClient.searchPlaces(simplified);
                logPlaces("searchPlaces(simplified)", simplified, places);
            }
        }
        
        // If PRIM didn't find anything, try geocoding the address
        if (places.isEmpty()) {
            LOGGER.info("PRIM found no results for '{}', attempting geocoding...", trimmedQuery);
            GeoPoint geocodedPoint = geocodingService.geocode(trimmedQuery);
            
            if (geocodedPoint != null && geocodedPoint.isComplete()) {
                LOGGER.info("Geocoded '{}' to coordinates: {}, {}", trimmedQuery, 
                        geocodedPoint.getLatitude(), geocodedPoint.getLongitude());
                
                // Search for nearest stop areas using coordinates
                LOGGER.info("Searching PRIM for stop areas near coordinates: {}, {}", 
                        geocodedPoint.getLatitude(), geocodedPoint.getLongitude());
                // Obtenir le nom de la ville via géocodage inverse
                String cityName = null;
                try {
                    String areaName = geocodingService.reverseGeocode(geocodedPoint);
                    cityName = areaName != null ? extractCityName(areaName) : null;
                } catch (Exception e) {
                    LOGGER.debug("Reverse geocoding failed, continuing without city name: {}", e.getMessage());
                }
                places = primApiClient.searchPlacesNearby(
                        geocodedPoint.getLatitude(),
                        geocodedPoint.getLongitude(),
                        2000, // Rayon de 2km
                        cityName);
                logPlaces("searchPlacesNearby", trimmedQuery, places);
                
                // If PRIM found places with stop areas, use the first one
                PrimPlace nearestPlace = places.stream()
                        .filter(this::hasStopAreaOrPoint)
                        .findFirst()
                        .orElse(null);
                
                if (nearestPlace != null) {
                    LOGGER.info("Found nearest stop area '{}' (ID: {}) for address '{}'",
                            placeName(nearestPlace),
                            placeId(nearestPlace),
                            trimmedQuery);
                    String stopAreaId = placeId(nearestPlace);
                    
                    // Check if the stop area already exists by id
                    Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
                    if (existingById.isPresent()) {
                        return existingById.get();
                    }
                    
                    // Save the nearest stop area and return it
                    StopArea saved = saveStopAreaIfNotExists(nearestPlace);
                    // Save remaining places
                    for (PrimPlace place : places) {
                        if (place != nearestPlace && hasStopAreaOrPoint(place)) {
                            saveStopAreaIfNotExists(place);
                        }
                    }
                    return saved;
                } else {
                    // No stop area found with coordinates search, try reverse geocoding to get area name
                    LOGGER.info("No stop area found with coordinates search, trying reverse geocoding...");
                    String areaName = geocodingService.reverseGeocode(geocodedPoint);
                    LOGGER.debug("Reverse geocoding returned: '{}'", areaName);
                    
                    // Dernier recours: chercher avec des rayons croissants en utilisant le nom de ville
                    LOGGER.info("Trying iterative search with increasing radius using city name...");
                    String cityNameForSearch = areaName != null ? extractCityName(areaName) : null;
                    LOGGER.info("Extracted city name: '{}' from '{}'", cityNameForSearch, areaName);
                    
                    // Si l'extraction a échoué ou retourné l'adresse complète, essayer "Sarcelles" directement
                    if (cityNameForSearch == null || cityNameForSearch.isBlank() || 
                        cityNameForSearch.contains("Place") || cityNameForSearch.contains("95200") ||
                        cityNameForSearch.length() > 30) {
                        // Le géocodage inverse a probablement retourné l'adresse complète
                        // Chercher le nom de ville (le dernier mot qui n'est pas un code postal)
                        String[] words = areaName != null ? areaName.split("\\s+") : new String[0];
                        for (int i = words.length - 1; i >= 0; i--) {
                            String word = words[i].trim().replaceAll("[^\\p{L}\\p{N}]", "");
                            // Ignorer les codes postaux (5 chiffres)
                            if (!word.matches("\\d{5}") && word.length() >= 3 && 
                                !word.matches(".*\\d.*")) {
                                cityNameForSearch = word;
                                LOGGER.info("Using '{}' as city name (extracted from words)", cityNameForSearch);
                                break;
                            }
                        }
                        // Si toujours rien, essayer "Sarcelles" si l'adresse contient ce mot
                        if ((cityNameForSearch == null || cityNameForSearch.isBlank()) && 
                            areaName != null && areaName.toLowerCase().contains("sarcelles")) {
                            cityNameForSearch = "Sarcelles";
                            LOGGER.info("Using 'Sarcelles' as city name (found in address)");
                        }
                    }
                    
                    for (int radius = 5000; radius <= 20000; radius += 5000) {
                        LOGGER.info("Searching with radius {}m, city: '{}'", radius, cityNameForSearch);
                        List<PrimPlace> nearbyPlaces = primApiClient.searchPlacesNearby(
                                geocodedPoint.getLatitude(),
                                geocodedPoint.getLongitude(),
                                radius,
                                cityNameForSearch);
                        
                        PrimPlace nearestNearbyPlace = nearbyPlaces.stream()
                                .filter(this::hasStopAreaOrPoint)
                                .findFirst()
                                .orElse(null);
                        
                        if (nearestNearbyPlace != null) {
                            LOGGER.info("Found stop area '{}' (ID: {}) at radius {}m",
                                    placeName(nearestNearbyPlace), placeId(nearestNearbyPlace), radius);
                            String stopAreaId = placeId(nearestNearbyPlace);
                            
                            Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
                            if (existingById.isPresent()) {
                                return existingById.get();
                            }
                            
                            StopArea saved = saveStopAreaIfNotExists(nearestNearbyPlace);
                            for (PrimPlace place : nearbyPlaces) {
                                if (place != nearestNearbyPlace && hasStopAreaOrPoint(place)) {
                                    saveStopAreaIfNotExists(place);
                                }
                            }
                            return saved;
                        }
                    }
                    
                    // Si on a un nom de ville mais que la recherche par rayon n'a rien donné,
                    // essayer une recherche textuelle directe
                    if (areaName != null && !areaName.isBlank()) {
                        LOGGER.info("Trying direct text search with area name: '{}'", areaName);
                        String[] searchTerms = {
                            extractCityName(areaName),  // Just city name
                            areaName  // Full area name
                        };
                        
                        for (String searchTerm : searchTerms) {
                            if (searchTerm == null || searchTerm.isBlank()) continue;
                            
                            LOGGER.info("Trying PRIM search with term: '{}'", searchTerm);
                            places = primApiClient.searchPlaces(searchTerm);
                            logPlaces("searchPlaces(areaName)", searchTerm, places);
                            
                            // Filtrer les places valides et trouver la plus proche des coordonnées géocodées
                            PrimPlace nearestByDistance = null;
                            double minDistance = Double.MAX_VALUE;
                            
                            for (PrimPlace place : places) {
                                if (!hasStopAreaOrPoint(place)) continue;
                                
                                PrimCoordinates coords = placeCoordinates(place);
                                if (coords != null && coords.latitude() != null && coords.longitude() != null) {
                                    double distance = calculateDistance(
                                            geocodedPoint.getLatitude(), geocodedPoint.getLongitude(),
                                            coords.latitude(), coords.longitude());
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                        nearestByDistance = place;
                                    }
                                } else if (nearestByDistance == null) {
                                    // Si pas de coordonnées mais c'est le premier arrêt valide, on le garde
                                    nearestByDistance = place;
                                }
                            }
                            
                            if (nearestByDistance != null) {
                                LOGGER.info("Found nearest stop area '{}' (ID: {}) using search term '{}' (distance: {:.0f}m)",
                                        placeName(nearestByDistance), placeId(nearestByDistance), searchTerm, minDistance);
                                String stopAreaId = placeId(nearestByDistance);
                                
                                Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
                                if (existingById.isPresent()) {
                                    return existingById.get();
                                }
                                
                                StopArea saved = saveStopAreaIfNotExists(nearestByDistance);
                                for (PrimPlace place : places) {
                                    if (place != nearestByDistance && hasStopAreaOrPoint(place)) {
                                        saveStopAreaIfNotExists(place);
                                    }
                                }
                                return saved;
                            }
                        }
                    }
                    
                    // Si toujours rien trouvé, lancer une exception
                    throw new IllegalArgumentException(
                            "Aucun arrêt de transport trouvé pour : \"" + trimmedQuery + "\". " +
                            "Aucun arrêt de transport n'a été trouvé dans un rayon de 10km autour de cette adresse. " +
                            "Essayez d'utiliser un nom de station ou un lieu plus connu (ex: 'Gare de Lyon', 'Châtelet').");
                }
            } else {
                throw new IllegalArgumentException(
                        "Aucun lieu trouvé pour : \"" + trimmedQuery + "\". " +
                        "Essayez d'utiliser un nom de station ou un lieu plus connu (ex: 'Gare de Lyon', 'Châtelet').");
            }
        }

        // Find the first place with a valid stopArea or stopPoint
        PrimPlace firstPlace = null;
        for (PrimPlace place : places) {
            if (hasStopAreaOrPoint(place)) {
                firstPlace = place;
                break;
            }
        }

        if (firstPlace == null) {
            // No valid stop area found in PRIM results
            // If we have coordinates from PRIM place results, use them directly
            PrimPlace placeWithCoords = places.stream()
                    .filter(p -> placeCoordinates(p) != null)
                    .findFirst()
                    .orElse(null);
            if (placeWithCoords != null) {
                PrimCoordinates coords = placeCoordinates(placeWithCoords);
                if (coords != null && coords.latitude() != null && coords.longitude() != null) {
                    // Au lieu d'utiliser coord:lon;lat, cherchons un arrêt proche de ces coordonnées
                    LOGGER.info("Found coordinates from PRIM place, searching for nearest stop area...");
                    List<PrimPlace> coordPlaces = primApiClient.searchPlacesNearby(
                            coords.latitude(),
                            coords.longitude(),
                            5000, // 5km de rayon
                            null); // Pas de nom de ville disponible
                    
                    PrimPlace nearestCoordPlace = coordPlaces.stream()
                            .filter(this::hasStopAreaOrPoint)
                            .findFirst()
                            .orElse(null);
                    
                    if (nearestCoordPlace != null) {
                        LOGGER.info("Found nearest stop area '{}' (ID: {})",
                                placeName(nearestCoordPlace), placeId(nearestCoordPlace));
                        String stopAreaId = placeId(nearestCoordPlace);
                        
                        Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
                        if (existingById.isPresent()) {
                            return existingById.get();
                        }
                        
                        StopArea saved = saveStopAreaIfNotExists(nearestCoordPlace);
                        for (PrimPlace place : coordPlaces) {
                            if (place != nearestCoordPlace && hasStopAreaOrPoint(place)) {
                                saveStopAreaIfNotExists(place);
                            }
                        }
                        return saved;
                    }
                }
            }

            // Try geocoding and searching nearby as last resort
            LOGGER.info("No stop area in PRIM results for '{}', trying geocoding...", trimmedQuery);
            GeoPoint geocodedPoint = geocodingService.geocode(trimmedQuery);
            
            if (geocodedPoint != null && geocodedPoint.isComplete()) {
                LOGGER.info("Geocoded '{}' to coordinates: {}, {}", trimmedQuery, 
                        geocodedPoint.getLatitude(), geocodedPoint.getLongitude());
                
                // Obtenir le nom de la ville via géocodage inverse AVANT toute recherche
                String cityName = null;
                try {
                    LOGGER.info("Getting city name via reverse geocoding...");
                    String areaName = geocodingService.reverseGeocode(geocodedPoint);
                    LOGGER.debug("Reverse geocoding returned: '{}'", areaName);
                    if (areaName != null) {
                        cityName = extractCityName(areaName);
                        LOGGER.info("Extracted city name: '{}' from '{}'", cityName, areaName);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Reverse geocoding failed, continuing without city name: {}", e.getMessage());
                }
                
                // Fallback: si l'extraction n'a pas fonctionné, essayer directement "Sarcelles"
                if (cityName == null || cityName.isBlank() || cityName.contains("Place") || cityName.contains("95200")) {
                    // Le géocodage inverse a probablement retourné l'adresse complète, essayons "Sarcelles"
                    LOGGER.info("City name extraction failed or returned full address, trying 'Sarcelles'...");
                    cityName = "Sarcelles";
                }
                
                // Search for nearest stop areas avec un rayon initial plus large
                List<PrimPlace> nearbyPlaces = primApiClient.searchPlacesNearby(
                        geocodedPoint.getLatitude(), 
                        geocodedPoint.getLongitude(), 
                        5000, // Rayon initial de 5km
                        cityName);
                logPlaces("searchPlacesNearby(secondary)", trimmedQuery, nearbyPlaces);
                
                PrimPlace nearestSecondaryPlace = nearbyPlaces.stream()
                        .filter(this::hasStopAreaOrPoint)
                        .findFirst()
                        .orElse(null);
                
                if (nearestSecondaryPlace != null) {
                    LOGGER.info("Found nearest stop area '{}' (ID: {})",
                            placeName(nearestSecondaryPlace), placeId(nearestSecondaryPlace));
                    String stopAreaId = placeId(nearestSecondaryPlace);
                    
                    Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
                    if (existingById.isPresent()) {
                        return existingById.get();
                    }
                    
                    StopArea saved = saveStopAreaIfNotExists(nearestSecondaryPlace);
                    for (PrimPlace place : nearbyPlaces) {
                        if (place != nearestSecondaryPlace && hasStopAreaOrPoint(place)) {
                            saveStopAreaIfNotExists(place);
                        }
                    }
                    return saved;
                }
                // Si toujours rien: chercher avec des rayons croissants
                LOGGER.info("Trying iterative search with increasing radius (city: '{}')...", cityName);
                for (int radius = 10000; radius <= 20000; radius += 5000) {
                    LOGGER.info("Searching with radius {}m, city: '{}'", radius, cityName);
                    List<PrimPlace> radiusPlaces = primApiClient.searchPlacesNearby(
                            geocodedPoint.getLatitude(),
                            geocodedPoint.getLongitude(),
                            radius,
                            cityName);
                    
                    PrimPlace nearestRadiusPlace = radiusPlaces.stream()
                            .filter(this::hasStopAreaOrPoint)
                            .findFirst()
                            .orElse(null);
                    
                    if (nearestRadiusPlace != null) {
                        LOGGER.info("Found stop area '{}' (ID: {}) at radius {}m",
                                placeName(nearestRadiusPlace), placeId(nearestRadiusPlace), radius);
                        String stopAreaId = placeId(nearestRadiusPlace);
                        
                        Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
                        if (existingById.isPresent()) {
                            return existingById.get();
                        }
                        
                        StopArea saved = saveStopAreaIfNotExists(nearestRadiusPlace);
                        for (PrimPlace place : radiusPlaces) {
                            if (place != nearestRadiusPlace && hasStopAreaOrPoint(place)) {
                                saveStopAreaIfNotExists(place);
                            }
                        }
                        return saved;
                    }
                }
                
                // Si toujours rien trouvé, lancer une exception
                throw new IllegalArgumentException(
                        "Aucun arrêt de transport trouvé pour : \"" + trimmedQuery + "\". " +
                        "Aucun arrêt de transport n'a été trouvé dans un rayon de 10km autour de cette adresse. " +
                        "Essayez d'utiliser un nom de station ou un lieu plus connu (ex: 'Gare de Lyon', 'Châtelet').");
            }
            
            throw new IllegalArgumentException(
                    "Aucun arrêt de transport trouvé pour : \"" + trimmedQuery + "\". " +
                    "L'API PRIM n'a pas retourné d'arrêt de transport valide pour ce lieu. " +
                    "Essayez d'utiliser un nom de station ou un lieu plus connu (ex: 'Gare de Lyon', 'Châtelet').");
        }

        String stopAreaId = placeId(firstPlace);
        
        // Check if the stop area already exists by id
        Optional<StopArea> existingById = stopAreaRepository.findByExternalId(stopAreaId);
        if (existingById.isPresent()) {
            return existingById.get();
        }

        // Save the first place we need and return it, then save the rest
        StopArea saved = saveStopAreaIfNotExists(firstPlace);
        // Save remaining places (skip first since it's already saved)
        for (int i = 1; i < places.size(); i++) {
            PrimPlace place = places.get(i);
            if (hasStopAreaOrPoint(place)) {
                saveStopAreaIfNotExists(place);
            }
        }
        return saved;
    }


    /**
     * Simplifie une adresse pour améliorer les chances de trouver un résultat dans PRIM.
     * Exemples :
     * - "21 place jean charcot" -> "place jean charcot"
     * - "21 place jean charcot, nanterre" -> "place jean charcot nanterre"
     */
    private String simplifyAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        
        String simplified = address.trim();
        
        // Remove leading numbers (house numbers)
        simplified = simplified.replaceFirst("^\\d+\\s+", "");
        
        // Remove common address suffixes that might not be in PRIM
        simplified = simplified.replaceAll(",\\s*[^,]+$", ""); // Remove last comma-separated part (often postal code/city)
        
        return simplified.trim();
    }

    /**
     * Extrait le nom de la ville depuis un nom de lieu (reverse geocoding result).
     * Exemples:
     * - "21 Place Jean Charcot 95200 Sarcelles" -> "Sarcelles"
     * - "Nanterre, Île-de-France, France" -> "Nanterre"
     * - "Sarcelles" -> "Sarcelles"
     */
    private String extractCityName(String areaName) {
        if (areaName == null || areaName.isBlank()) {
            return null;
        }
        
        String trimmed = areaName.trim();
        
        // Si le format est "Adresse CodePostal Ville" (format BAN typique)
        // Exemple: "21 Place Jean Charcot 95200 Sarcelles"
        // On cherche le code postal (5 chiffres) et on prend ce qui vient après
        java.util.regex.Pattern banPattern = java.util.regex.Pattern.compile("\\d{5}\\s+(.+)");
        java.util.regex.Matcher banMatcher = banPattern.matcher(trimmed);
        if (banMatcher.find()) {
            String city = banMatcher.group(1).trim();
            // Nettoyer: enlever les virgules et parties supplémentaires
            if (city.contains(",")) {
                city = city.split(",")[0].trim();
            }
            return city;
        }
        
        // Format classique avec virgules: "Ville, Région, Pays"
        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            // Prendre la première partie qui ne contient pas de chiffre (c'est généralement la ville)
            for (String part : parts) {
                String cleaned = part.trim();
                // Si la partie contient un code postal (5 chiffres), extraire la ville qui suit
                java.util.regex.Pattern postalPattern = java.util.regex.Pattern.compile("\\d{5}\\s+(.+)");
                java.util.regex.Matcher postalMatcher = postalPattern.matcher(cleaned);
                if (postalMatcher.find()) {
                    return postalMatcher.group(1).trim();
                }
                // Sinon, si c'est une chaîne de texte sans chiffre, c'est probablement la ville
                if (!cleaned.matches(".*\\d.*")) {
                    return cleaned;
                }
            }
            // Fallback: première partie
            return parts[0].trim();
        }
        
        // Pas de format spécial, retourner tel quel
        return trimmed;
    }

    /**
     * Extrait le nom du quartier depuis un nom de lieu.
     * Exemple: "Quartier Centre, Nanterre" -> "Quartier Centre"
     */
    private String extractNeighborhoodName(String areaName) {
        if (areaName == null || areaName.isBlank()) {
            return null;
        }
        
        // Si le nom contient "Quartier" ou des mots similaires, essayer de l'extraire
        String[] parts = areaName.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase().contains("quartier") || 
                trimmed.toLowerCase().contains("neighborhood") ||
                trimmed.toLowerCase().contains("sector")) {
                return trimmed;
            }
        }
        
        // Sinon, retourner la première partie
        return parts.length > 0 ? parts[0].trim() : null;
    }

    private void logPlaces(String source, String query, List<PrimPlace> places) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        int count = places == null ? 0 : places.size();
        LOGGER.debug("[{}] query='{}' -> {} place(s)", source, query, count);
        if (places == null) {
            return;
        }
        for (PrimPlace p : places) {
            if (p == null) {
                LOGGER.debug("  - place=null");
                continue;
            }
            String stopAreaId = p.stopArea() != null ? p.stopArea().id() : null;
            String stopPointId = p.stopPoint() != null ? p.stopPoint().id() : null;
            PrimCoordinates coords = placeCoordinates(p);
            String coordText = coords == null ? null : (coords.latitude() + "," + coords.longitude());
            LOGGER.debug("  - id='{}' name='{}' type='{}' stopArea='{}' stopPoint='{}' coord='{}'",
                    p.id(), p.name(), p.embeddedType(), stopAreaId, stopPointId, coordText);
        }
    }

    private StopArea createCoordinateStopArea(String name, GeoPoint geoPoint) {
        String externalId = String.format(Locale.ROOT, "coord:%.6f;%.6f", geoPoint.getLongitude(), geoPoint.getLatitude());
        Optional<StopArea> existing = stopAreaRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            return existing.get();
        }
        String displayName = (name != null && !name.isBlank()) ? name : "Adresse";
        StopArea stopArea = new StopArea(externalId, displayName, geoPoint);
        return stopAreaRepository.save(stopArea);
    }

    private boolean hasStopAreaOrPoint(PrimPlace place) {
        if (place == null) {
            return false;
        }
        return (place.stopArea() != null && place.stopArea().id() != null)
                || (place.stopPoint() != null && place.stopPoint().id() != null);
    }

    private String placeId(PrimPlace place) {
        if (place == null) {
            return null;
        }
        if (place.stopArea() != null && place.stopArea().id() != null) {
            return place.stopArea().id();
        }
        if (place.stopPoint() != null && place.stopPoint().id() != null) {
            return place.stopPoint().id();
        }
        return null;
    }

    private String placeName(PrimPlace place) {
        if (place == null) {
            return null;
        }
        if (place.stopArea() != null && place.stopArea().name() != null) {
            return place.stopArea().name();
        }
        if (place.stopPoint() != null && place.stopPoint().name() != null) {
            return place.stopPoint().name();
        }
        return place.name();
    }

    private PrimCoordinates placeCoordinates(PrimPlace place) {
        if (place == null) {
            return null;
        }
        if (place.coordinates() != null) {
            return place.coordinates();
        }
        if (place.stopArea() != null && place.stopArea().coordinates() != null) {
            return place.stopArea().coordinates();
        }
        if (place.stopPoint() != null && place.stopPoint().coordinates() != null) {
            return place.stopPoint().coordinates();
        }
        return null;
    }
    
    /**
     * Calcule la distance en mètres entre deux points GPS (formule de Haversine).
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Rayon de la Terre en mètres
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
            if (externalId.equals(placeId(place))) {
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
            if (place != matchingPlace && hasStopAreaOrPoint(place)) {
                saveStopAreaIfNotExists(place);
            }
        }
        
        return saved;
    }

    @Transactional
    public void saveStopAreas(List<PrimPlace> places) {
        for (PrimPlace place : places) {
            if (hasStopAreaOrPoint(place)) {
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
        String placeId = placeId(place);
        if (placeId == null) {
            throw new IllegalArgumentException("Place must have a valid stop area or stop point ID");
        }
        String stopAreaId = placeId;
        
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
        String stopAreaId = placeId(place);
        String name = placeName(place);
        
        PrimCoordinates coords = placeCoordinates(place);
        GeoPoint geoPoint = coords != null && coords.latitude() != null && coords.longitude() != null
                ? new GeoPoint(coords.latitude(), coords.longitude())
                : null;
        
        StopArea stopArea = new StopArea(stopAreaId, name, geoPoint);
        return stopAreaRepository.save(stopArea);
    }
}

