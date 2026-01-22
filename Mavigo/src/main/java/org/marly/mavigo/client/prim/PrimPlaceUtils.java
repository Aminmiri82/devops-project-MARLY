package org.marly.mavigo.client.prim;

import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;

/**
 * Utility methods for working with PrimPlace objects.
 */
public final class PrimPlaceUtils {

    private PrimPlaceUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Checks if the given place has a valid stop area or stop point.
     */
    public static boolean hasStopAreaOrPoint(PrimPlace place) {
        if (place == null) {
            return false;
        }
        return (place.stopArea() != null && place.stopArea().id() != null)
                || (place.stopPoint() != null && place.stopPoint().id() != null);
    }

    /**
     * Extracts coordinates from a PrimPlace, checking the place itself,
     * then its stop area, then its stop point.
     */
    public static PrimCoordinates placeCoordinates(PrimPlace place) {
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
     * Calculates the distance in meters between two GPS points using the Haversine formula.
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
