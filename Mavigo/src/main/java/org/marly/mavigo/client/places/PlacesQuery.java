package org.marly.mavigo.client.places;

import java.util.Set;
import org.marly.mavigo.models.poi.PointOfInterestCategory;
import org.marly.mavigo.models.shared.GeoPoint;

public record PlacesQuery(
        GeoPoint coordinate,
        int radiusMeters,
        Set<PointOfInterestCategory> categories,
        int maxResults) {
}

