package org.marly.mavigo.service.comfort;

import org.marly.mavigo.client.prim.PrimItineraryRequest;
import org.marly.mavigo.service.comfort.dto.ComfortModeRequestBuilder;

public interface ComfortModeService {
    // Modifies a PRIM itinerary request based on user's comfort preferences and weather. called before calling PRIM API.
    PrimItineraryRequest applyComfortPreferences(ComfortModeRequestBuilder builder);
}

