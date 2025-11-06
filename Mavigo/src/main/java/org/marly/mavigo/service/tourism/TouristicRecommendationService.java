package org.marly.mavigo.service.tourism;

import java.util.List;
import org.marly.mavigo.models.poi.PointOfInterest;
import org.marly.mavigo.service.tourism.dto.TouristicContext;

public interface TouristicRecommendationService {
    // user tells us the journey they want to take, we recommend points of interest on or along thw way, 
    // they can then choose to add them to the journey or not
    // we can either use the POI's that are in db or use Google Places API to get them
    // check out client/places package for more details
    List<PointOfInterest> recommendPointsOfInterest(TouristicContext context);
}

