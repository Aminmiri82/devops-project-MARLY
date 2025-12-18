package org.marly.mavigo.service.journey;

import java.util.ArrayList;
import java.util.List;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.UserTask;
import org.springframework.stereotype.Service;

@Service
public class TaskOnRouteService {

    public List<GeoPoint> extractRoutePoints(Journey journey) {
        List<GeoPoint> points = new ArrayList<>();
        if (journey == null) return points;

        if (journey.getOriginCoordinate() != null) points.add(journey.getOriginCoordinate());
        if (journey.getDestinationCoordinate() != null) points.add(journey.getDestinationCoordinate());

        List<Leg> legs = journey.getLegs();
        if (legs != null) {
            for (Leg leg : legs) {
                if (leg.getOriginCoordinate() != null) points.add(leg.getOriginCoordinate());
                if (leg.getDestinationCoordinate() != null) points.add(leg.getDestinationCoordinate());
            }
        }
        return points;
    }

    public double minDistanceMeters(GeoPoint point, List<GeoPoint> routePoints) {
        if (point == null || routePoints == null || routePoints.isEmpty()) return Double.POSITIVE_INFINITY;

        double min = Double.POSITIVE_INFINITY;
        for (GeoPoint rp : routePoints) {
            if (rp == null) continue;
            double d = haversineMeters(
                    point.getLatitude(), point.getLongitude(),
                    rp.getLatitude(), rp.getLongitude()
            );
            if (d < min) min = d;
        }
        return min;
    }

    public boolean isTaskOnRoute(UserTask task, List<GeoPoint> routePoints, double radiusMeters) {
        if (task == null || task.getLocationHint() == null) return false;
        double d = minDistanceMeters(task.getLocationHint(), routePoints);
        return d <= radiusMeters;
    }

    // Haversine distance (meters)
    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth radius meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}