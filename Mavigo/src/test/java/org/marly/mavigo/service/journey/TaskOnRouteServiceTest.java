package org.marly.mavigo.service.journey;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskOnRouteServiceTest {

    private final TaskOnRouteService service = new TaskOnRouteService();

    @Test
    void minDistanceMeters_shouldBeSmall_whenPointNearRoute() {
        // Route point: Gare de Lyon approx
        GeoPoint routePoint = new GeoPoint(48.8443, 2.3730);

        // Task very close (~ a few meters)
        GeoPoint taskPoint = new GeoPoint(48.84435, 2.37305);

        double d = service.minDistanceMeters(taskPoint, List.of(routePoint));
        assertTrue(d < 50, "Expected < 50m, got " + d);
    }

    @Test
    void minDistanceMeters_shouldBeLarge_whenPointFarFromRoute() {
        GeoPoint routePoint = new GeoPoint(48.8443, 2.3730);
        // Far: Tour Eiffel approx
        GeoPoint taskPoint = new GeoPoint(48.8584, 2.2945);

        double d = service.minDistanceMeters(taskPoint, List.of(routePoint));
        assertTrue(d > 3000, "Expected > 3km, got " + d);
    }
}