package org.marly.mavigo.client.prim;

import java.util.List;

public record PrimItineraryResponse(String itineraryId, List<PrimLeg> legs) {
}

