package org.marly.mavigo.service.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;

class TaskOnRouteServiceAdvancedTest {

    private final TaskOnRouteService service = new TaskOnRouteService();

    @Test
    void extractRoutePoints_returnsEmptyForNullJourney() {
        assertTrue(service.extractRoutePoints(null).isEmpty());
    }

    @Test
    void extractRoutePoints_skipsNullAndIncompletePoints_andDedupeConsecutive() {
        Journey journey = mock(Journey.class);
        JourneySegment segment = mock(JourneySegment.class);
        JourneyPoint nullPoint = null;
        JourneyPoint duplicateOriginPoint = mock(JourneyPoint.class);
        JourneyPoint incompletePoint = mock(JourneyPoint.class);
        JourneyPoint validPoint = mock(JourneyPoint.class);

        GeoPoint origin = new GeoPoint(48.8566, 2.3522);
        GeoPoint destination = new GeoPoint(48.8666, 2.3622);
        GeoPoint intermediate = new GeoPoint(48.8610, 2.3570);

        when(duplicateOriginPoint.getCoordinates()).thenReturn(new GeoPoint(48.8566, 2.3522));
        when(incompletePoint.getCoordinates()).thenReturn(new GeoPoint(48.8610, null));
        when(validPoint.getCoordinates()).thenReturn(intermediate);
        List<JourneyPoint> segmentPoints = new ArrayList<>();
        segmentPoints.add(nullPoint);
        segmentPoints.add(duplicateOriginPoint);
        segmentPoints.add(incompletePoint);
        segmentPoints.add(validPoint);
        when(segment.getPoints()).thenReturn(segmentPoints);
        when(journey.getOriginCoordinate()).thenReturn(origin);
        List<JourneySegment> segments = new ArrayList<>();
        segments.add(null);
        segments.add(segment);
        when(journey.getSegments()).thenReturn(segments);
        when(journey.getDestinationCoordinate()).thenReturn(destination);

        List<GeoPoint> points = service.extractRoutePoints(journey);

        assertEquals(3, points.size());
        assertEquals(origin.getLatitude(), points.get(0).getLatitude());
        assertEquals(intermediate.getLatitude(), points.get(1).getLatitude());
        assertEquals(destination.getLatitude(), points.get(2).getLatitude());
    }

    @Test
    void densify_handlesNullOrSinglePointInputs() {
        assertTrue(service.densify(null, 100).isEmpty());

        GeoPoint only = new GeoPoint(48.0, 2.0);
        List<GeoPoint> onePoint = service.densify(List.of(only), 100);
        assertEquals(1, onePoint.size());
        assertEquals(48.0, onePoint.get(0).getLatitude());
    }

    @Test
    void densify_withNonPositiveStepDoesNotInterpolate() {
        GeoPoint a = new GeoPoint(48.8566, 2.3522);
        GeoPoint b = new GeoPoint(48.8766, 2.3722);

        List<GeoPoint> out = service.densify(List.of(a, b), 0);

        assertEquals(2, out.size());
        assertEquals(a.getLatitude(), out.get(0).getLatitude());
        assertEquals(b.getLatitude(), out.get(1).getLatitude());
    }

    @Test
    void densify_interpolatesWhenDistanceGreaterThanStep() {
        GeoPoint a = new GeoPoint(48.8566, 2.3522);
        GeoPoint b = new GeoPoint(48.8566, 2.3722); // ~1.46km east

        List<GeoPoint> out = service.densify(List.of(a, b), 400);

        assertTrue(out.size() >= 4, "Expected interpolated points between endpoints");
        assertEquals(a.getLongitude(), out.get(0).getLongitude());
        assertEquals(b.getLongitude(), out.get(out.size() - 1).getLongitude());
    }

    @Test
    void minDistanceMetersToPolyline_handlesNullInputs() {
        assertTrue(Double.isInfinite(service.minDistanceMetersToPolyline(null, List.of())));
        assertTrue(Double.isInfinite(service.minDistanceMetersToPolyline(new GeoPoint(48.0, 2.0), null)));
        assertTrue(Double.isInfinite(service.minDistanceMetersToPolyline(new GeoPoint(48.0, 2.0), List.of())));
    }

    @Test
    void minDistanceMetersToPolyline_usesSegmentProjection() {
        GeoPoint point = new GeoPoint(48.8580, 2.3600);
        GeoPoint a = new GeoPoint(48.8560, 2.3500);
        GeoPoint b = new GeoPoint(48.8560, 2.3700);

        double distance = service.minDistanceMetersToPolyline(point, List.of(a, b));

        assertTrue(distance < 300, "Projection onto segment should be near the line");
    }

    @Test
    void minDistanceMetersToPolyline_beforeSegmentFallsBackToEndpoint() {
        GeoPoint point = new GeoPoint(48.8560, 2.3490);
        GeoPoint a = new GeoPoint(48.8560, 2.3500);
        GeoPoint b = new GeoPoint(48.8560, 2.3600);

        double distance = service.minDistanceMetersToPolyline(point, List.of(a, b));

        assertTrue(distance > 50);
        assertTrue(distance < 150);
    }

    @Test
    void isTaskOnRoute_returnsTrueWhenInsideRadiusAndFalseOtherwise() {
        UserTask onRouteTask = new UserTask(new User("ext", "u@example.com", "User"), "src-1", TaskSource.MANUAL,
                "Task");
        onRouteTask.setLocationHint(new GeoPoint(48.8561, 2.3505));

        List<GeoPoint> polyline = List.of(
                new GeoPoint(48.8560, 2.3500),
                new GeoPoint(48.8560, 2.3600));

        assertTrue(service.isTaskOnRoute(onRouteTask, polyline, 100));

        onRouteTask.setLocationHint(new GeoPoint(48.8700, 2.4200));
        assertFalse(service.isTaskOnRoute(onRouteTask, polyline, 100));
        assertFalse(service.isTaskOnRoute(null, polyline, 100));
    }
}
