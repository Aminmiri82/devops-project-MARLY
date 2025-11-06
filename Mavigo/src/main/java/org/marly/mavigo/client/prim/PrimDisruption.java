package org.marly.mavigo.client.prim;

import java.time.OffsetDateTime;

public record PrimDisruption(
        String alertId,
        String lineCode,
        String severity,
        String message,
        OffsetDateTime validFrom,
        OffsetDateTime validUntil) {
}

