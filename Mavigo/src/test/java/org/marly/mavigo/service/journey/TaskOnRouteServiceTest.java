package org.marly.mavigo.service.journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    void extractRoutePoints_ShouldReturnEmpty_WhenJourneyIsNull() {
        assertTrue(service.extractRoutePoints(null).isEmpty());
    }

    @Test
    void extractRoutePoints_ShouldIncludeLegsAndDedupe() {
        Journey journey = createTestJourney();
        
        Leg leg1 = new Leg();
        leg1.setOriginCoordinate(new GeoPoint(48.8, 2.3)); // same as journey origin
        leg1.setDestinationCoordinate(new GeoPoint(47.0, 3.0));
        
        journey.addLeg(leg1);
        
        List<GeoPoint> points = service.extractRoutePoints(journey);
        
        // Expected: journey_origin (same as leg1_origin), leg1_destination, journey_destination
        // dedupeConsecutive should remove the duplicate origin
        assertEquals(3, points.size());
        assertEquals(48.8, points.get(0).getLatitude());
        assertEquals(47.0, points.get(1).getLatitude());
        assertEquals(45.75, points.get(2).getLatitude());
    }

    @Test
    void extractRoutePoints_ShouldSkipInvalidLegs() {
        Journey journey = createTestJourney();
        journey.replaceLegs(new ArrayList<Leg>());
        // Unfortunately addLeg doesn't allow nulls if we use Collections.unmodifiableList
        // But the service checks for nulls anyway. 
        // Let's see if we can trigger the 'if (leg == null) continue'
        // Since getLegs is unmodifiable, we have to use replaceLegs with a list containing null if allowed
        List<Leg> legsWithNull = new ArrayList<>();
        legsWithNull.add(null);
        journey.replaceLegs(legsWithNull);
        
        List<GeoPoint> points = service.extractRoutePoints(journey);
        assertEquals(2, points.size());
    }

    // ========== densify tests ==========

    @Test
    void densify_ShouldReturnEmpty_WhenInputIsNull() {
        assertEquals(0, service.densify(null, 100).size());
    }

    @Test
    void densify_ShouldReturnSame_WhenTooFewPoints() {
        List<GeoPoint> points = List.of(new GeoPoint(48.8, 2.3));
        assertEquals(1, service.densify(points, 100).size());
    }

    @Test
    void densify_ShouldAddPoints_WhenDistanceLarge() {
        // Paris to London ~344km
        GeoPoint p1 = new GeoPoint(48.8566, 2.3522);
        GeoPoint p2 = new GeoPoint(51.5074, -0.1278);
        
        // step 100km
        List<GeoPoint> densified = service.densify(List.of(p1, p2), 100_000);
        
        // ~344km / 100km = 3 steps -> points at 0, 0.33, 0.66, 1.0 (some deduped)
        assertTrue(densified.size() > 2);
    }
    
    @Test
    void densify_ShouldHandleNullPointsInList() {
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(48.8, 2.3));
        points.add(null);
        points.add(new GeoPoint(45.7, 4.8));
        
        List<GeoPoint> densified = service.densify(points, 1000);
        assertNotNull(densified);
        assertTrue(densified.size() >= 2);
    }

    // ========== minDistanceMetersToPolyline tests ==========

    @Test
    void minDistanceToPolyline_ShouldReturnInfinity_WhenInputInvalid() {
        GeoPoint p = new GeoPoint(48.8, 2.3);
        assertEquals(Double.POSITIVE_INFINITY, service.minDistanceMetersToPolyline(null, List.of(p)));
        assertEquals(Double.POSITIVE_INFINITY, service.minDistanceMetersToPolyline(p, null));
        assertEquals(Double.POSITIVE_INFINITY, service.minDistanceMetersToPolyline(p, List.of()));
    }

    @Test
    void minDistanceToPolyline_ShouldCalculatePointToSegment() {
        // Line from (0,0) to (0,10)
        GeoPoint a = new GeoPoint(0.0, 0.0);
        GeoPoint b = new GeoPoint(0.1, 0.0); // Roughly 11km north
        
        // Target point slightly to the side of the segment
        GeoPoint target = new GeoPoint(0.05, 0.001); // Middle lat, slightly east
        
        double dist = service.minDistanceMetersToPolyline(target, List.of(a, b));
        
        assertTrue(dist > 0);
        assertTrue(dist < 500); // Should be a few hundred meters
    }
    
    @Test
    void minDistanceToPolyline_ShouldHandleNullPointsInPolyline() {
        GeoPoint target = new GeoPoint(48.8, 2.3);
        List<GeoPoint> polyline = new ArrayList<>();
        polyline.add(null);
        polyline.add(new GeoPoint(48.8, 2.3));
        
        double dist = service.minDistanceMetersToPolyline(target, polyline);
        assertEquals(0.0, dist, 0.001);
    }

    // ========== isTaskOnRoute tests ==========

    @Test
    void isTaskOnRoute_ShouldReturnFalse_WhenTaskOrLocationNull() {
        User user = new User("e1", "e", "n");
        UserTask task = new UserTask(user, "g1", null, "t");
        
        assertFalse(service.isTaskOnRoute(null, List.of(), 100));
        assertFalse(service.isTaskOnRoute(task, List.of(), 100));
    }

    @Test
    void isTaskOnRoute_ShouldReturnTrue_WhenWithinRadius() {
        User user = new User("e1", "e", "n");
        UserTask task = new UserTask(user, "g1", null, "t");
        task.setLocationHint(new GeoPoint(48.85, 2.35));
        
        List<GeoPoint> route = List.of(new GeoPoint(48.85, 2.35));
        
        assertTrue(service.isTaskOnRoute(task, route, 10));
        assertFalse(service.isTaskOnRoute(task, route, -1));
    }

    // ========== helper tests ==========

    @Test
    void haversineMeters_ShouldBeAccurate() {
        // Paris to London
        double d = TaskOnRouteService.haversineMeters(48.8566, 2.3522, 51.5074, -0.1278);
        assertEquals(344000, d, 5000); // approx 344km
    }
}