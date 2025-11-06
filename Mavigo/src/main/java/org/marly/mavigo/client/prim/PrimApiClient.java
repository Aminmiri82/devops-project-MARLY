package org.marly.mavigo.client.prim;

import java.util.List;

public interface PrimApiClient {

    PrimItineraryResponse planItinerary(PrimItineraryRequest request);

    List<PrimDisruption> fetchRealtimeDisruptions();
}

