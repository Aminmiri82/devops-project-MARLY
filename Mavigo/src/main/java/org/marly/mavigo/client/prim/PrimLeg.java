package org.marly.mavigo.client.prim;

import java.time.OffsetDateTime;

public record PrimLeg(
        String legId,
        String mode,
        String lineCode,
        String originStopId,
        String destinationStopId,
        OffsetDateTime plannedDeparture,
        OffsetDateTime plannedArrival) {
}

