package org.marly.mavigo.service.tasks;

import java.util.ArrayList;
import java.util.List;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.shared.GeoPoint;
import org.springframework.stereotype.Service;

@Service("taskOnRouteServiceTasks")
public class TaskOnRouteService {

    /**
     * Version simple: on approxime le "chemin" par une liste de points:
     * - originCoordinate / destinationCoordinate
     * - origin/destination de chaque leg
     */
    public List<GeoPoint> extractRoutePoints(Journey journey) {
        List<GeoPoint> points = new ArrayList<>();

        add(points, journey.getOriginCoordinate());

        List<Leg> legs = journey.getLegs();
        if (legs != null) {
            for (Leg leg : legs) {
                if (leg == null) continue;
                add(points, leg.getOriginCoordinate());
                add(points, leg.getDestinationCoordinate());
            }
        }

        add(points, journey.getDestinationCoordinate());

        return points;
    }

    public double minDistanceMeters(GeoPoint target, List<GeoPoint> routePoints) {
        if (target == null || routePoints == null || routePoints.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        double min = Double.POSITIVE_INFINITY;
        for (GeoPoint p : routePoints) {
            if (p == null) continue;
            double d = distanceMeters(target, p);
            if (d < min) min = d;
        }
        return min;
    }

    private void add(List<GeoPoint> points, GeoPoint p) {
        if (p == null) return;
        points.add(p);
    }

    /**
     * Haversine distance (mètres)
     */
    private double distanceMeters(GeoPoint a, GeoPoint b) {
        double lat1 = a.getLatitude();
        double lon1 = a.getLongitude();
        double lat2 = b.getLatitude();
        double lon2 = b.getLongitude();

        final double R = 6371000.0; // Earth radius (m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double s1 = Math.sin(dLat / 2);
        double s2 = Math.sin(dLon / 2);

        double aa = s1 * s1
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * s2 * s2;

        double c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa));
        return R * c;
    }
}