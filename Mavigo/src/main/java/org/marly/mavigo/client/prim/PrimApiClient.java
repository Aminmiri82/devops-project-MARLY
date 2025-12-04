package org.marly.mavigo.client.prim;

import java.util.List;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;

public interface PrimApiClient {

    List<PrimDisruption> fetchRealtimeDisruptions();

    List<PrimPlace> searchPlaces(String query);


    List<PrimJourneyPlanDto> calculateJourneyPlans(PrimJourneyRequest request);

    List<PrimDisruption> getDisruptions();
}
