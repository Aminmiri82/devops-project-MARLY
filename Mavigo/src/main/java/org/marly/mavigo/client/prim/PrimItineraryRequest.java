package org.marly.mavigo.client.prim;

import java.time.OffsetDateTime;

public record PrimItineraryRequest(
        String originStopId,
        String destinationStopId,
        OffsetDateTime departureTime,
        boolean allowWalking,
        boolean allowCycling,
        int maximumTransfers,
        boolean luggageAllowed,
        boolean wheelchairAccessible
        // many other parameters...
        ) {

}



