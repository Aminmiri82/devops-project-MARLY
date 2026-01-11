package org.marly.mavigo.client.prim;

import java.util.List;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.model.PrimPlace;

public interface PrimApiClient {

    List<PrimPlace> searchPlaces(String query);

    List<PrimJourneyPlanDto> calculateJourneyPlans(PrimJourneyRequest request);
}
