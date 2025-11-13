package org.marly.mavigo.client.prim;

import java.util.List;

public interface PrimApiClient {

    PrimItineraryResponse planItinerary(PrimItineraryRequest request);

    List<PrimDisruption> fetchRealtimeDisruptions();

    List<PrimPlace> searchPlaces(String query);

    PrimJourneyResponse getJourney(PrimJourneyRequest request);
}
