package org.marly.mavigo.client.google;

import java.time.OffsetDateTime;

import org.marly.mavigo.models.shared.GeoPoint;

public record GoogleTaskItem(
        String id,
        String title,
        String notes,
        GeoPoint location,
        OffsetDateTime due,
        boolean completed) {
}

