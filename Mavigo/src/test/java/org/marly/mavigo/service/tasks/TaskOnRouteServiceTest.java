package org.marly.mavigo.service.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskOnRouteServiceTest {

    private TaskOnRouteService service;

    @BeforeEach
    void setUp() {
        service = new TaskOnRouteService();
    }

    private Journey createTestJourney() {
        User user = new User("ext-1", "test@test.com", "Test User");
        OffsetDateTime departure = OffsetDateTime.now();
        OffsetDateTime arrival = departure.plusHours(1);
        Journey journey = new Journey(user, "Paris", "Lyon", departure, arrival);
        journey.setOriginCoordinate(new GeoPoint(48.8, 2.3));
        journey.setDestinationCoordinate(new GeoPoint(45.75, 4.85));
        return journey;
    }

    // ========== extractRoutePoints tests ==========

    @Test
    void extractRoutePoints_ShouldExtractOriginAndDestination() {
        Journey journey = createTestJourney();

        List<GeoPoint> result = service.extractRoutePoints(journey);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(48.8, result.get(0).getLatitude());
        assertEquals(45.75, result.get(1).getLatitude());
    }

    @Test
    void extractRoutePoints_ShouldIncludeLegCoordinates() {
        Journey journey = createTestJourney();

        // Add a leg with coordinates
        Leg leg = new Leg();
        leg.setOriginCoordinate(new GeoPoint(48.82, 2.32));
        leg.setDestinationCoordinate(new GeoPoint(47.5, 3.5));
        journey.addLeg(leg);

        List<GeoPoint> result = service.extractRoutePoints(journey);

        // origin + leg.origin + leg.destination + destination
        assertEquals(4, result.size());
    }

    @Test
    void extractRoutePoints_ShouldHandleNullLegCoordinates() {
        Journey journey = createTestJourney();

        // Add a leg without coordinates
        Leg leg = new Leg();
        journey.addLeg(leg);

        List<GeoPoint> result = service.extractRoutePoints(journey);

        // Should still have origin and destination
        assertEquals(2, result.size());
    }

    // ========== minDistanceMeters tests ==========

    @Test
    void minDistanceMeters_ShouldReturnZero_WhenTargetIsOnRoute() {
        GeoPoint target = new GeoPoint(48.8566, 2.3522);
        List<GeoPoint> routePoints = List.of(target);

        double result = service.minDistanceMeters(target, routePoints);

        assertEquals(0.0, result, 0.001);
    }

    @Test
    void minDistanceMeters_ShouldReturnDistance_WhenTargetIsOffRoute() {
        GeoPoint target = new GeoPoint(48.8566, 2.3522); // Paris
        GeoPoint routePoint = new GeoPoint(48.8570, 2.3530); // ~50m away

        double result = service.minDistanceMeters(target, List.of(routePoint));

        assertTrue(result > 0);
        assertTrue(result < 100); // Should be less than 100m
    }

    @Test
    void minDistanceMeters_ShouldReturnMinimum_WhenMultipleRoutePoints() {
        GeoPoint target = new GeoPoint(48.8566, 2.3522);
        GeoPoint far = new GeoPoint(48.9, 2.4); // ~5km away
        GeoPoint near = new GeoPoint(48.8567, 2.3523); // ~15m away

        double result = service.minDistanceMeters(target, List.of(far, near));

        assertTrue(result < 50); // Should be close to the near point
    }

    @Test
    void minDistanceMeters_ShouldReturnInfinity_WhenTargetIsNull() {
        List<GeoPoint> routePoints = List.of(new GeoPoint(48.8566, 2.3522));

        double result = service.minDistanceMeters(null, routePoints);

        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    void minDistanceMeters_ShouldReturnInfinity_WhenRoutePointsIsNull() {
        GeoPoint target = new GeoPoint(48.8566, 2.3522);

        double result = service.minDistanceMeters(target, null);

        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    void minDistanceMeters_ShouldReturnInfinity_WhenRoutePointsIsEmpty() {
        GeoPoint target = new GeoPoint(48.8566, 2.3522);

        double result = service.minDistanceMeters(target, List.of());

        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    void minDistanceMeters_ShouldSkipNullRoutePoints() {
        GeoPoint target = new GeoPoint(48.8566, 2.3522);
        GeoPoint validPoint = new GeoPoint(48.8567, 2.3523);

        java.util.List<GeoPoint> routePoints = new java.util.ArrayList<>();
        routePoints.add(null);
        routePoints.add(validPoint);

        double result = service.minDistanceMeters(target, routePoints);

        assertTrue(result < 50);
    }

    // ========== distanceMeters calculation tests ==========

    @Test
    void distanceMeters_ShouldCalculateHaversineCorrectly() {
        // Paris to London is approximately 344 km
        GeoPoint paris = new GeoPoint(48.8566, 2.3522);
        GeoPoint london = new GeoPoint(51.5074, -0.1278);

        double result = service.minDistanceMeters(paris, List.of(london));

        // Should be approximately 344 km (344000 meters)
        assertTrue(result > 340000);
        assertTrue(result < 350000);
    }

    @Test
    void distanceMeters_ShouldReturnZero_WhenSamePoint() {
        GeoPoint point = new GeoPoint(48.8566, 2.3522);

        double result = service.minDistanceMeters(point, List.of(point));

        assertEquals(0.0, result, 0.001);
    }
}
