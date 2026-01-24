package org.marly.mavigo.service.journey;

import java.util.ArrayList;
import java.util.List;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.UserTask;
import org.springframework.stereotype.Service;

@Service
public class TaskOnRouteService {

    public List<GeoPoint> extractRoutePoints(Journey journey) {
        List<GeoPoint> points = new ArrayList<>();
        if (journey == null)
            return points;

        add(points, journey.getOriginCoordinate());

        List<JourneySegment> segments = journey.getSegments();
        if (segments != null) {
            for (JourneySegment segment : segments) {
                if (segment == null)
                    continue;
                for (JourneyPoint point : segment.getPoints()) {
                    if (point == null)
                        continue;
                    add(points, point.getCoordinates());
                }
            }
        }

        add(points, journey.getDestinationCoordinate());

        return dedupeConsecutive(points);
    }

    public List<GeoPoint> densify(List<GeoPoint> basePoints, int stepMeters) {
        if (basePoints == null || basePoints.size() < 2)
            return basePoints == null ? List.of() : basePoints;

        List<GeoPoint> out = new ArrayList<>();
        GeoPoint prev = basePoints.get(0);
        if (prev == null)
            return basePoints;

        out.add(prev);

        for (int i = 1; i < basePoints.size(); i++) {
            GeoPoint next = basePoints.get(i);
            if (next == null)
                continue;

            double d = haversineMeters(prev.getLatitude(), prev.getLongitude(), next.getLatitude(),
                    next.getLongitude());
            if (d > stepMeters && stepMeters > 0) {
                int steps = (int) Math.floor(d / stepMeters);
                for (int s = 1; s < steps; s++) {
                    double t = (double) s / (double) steps;
                    out.add(interpolate(prev, next, t));
                }
            }

            out.add(next);
            prev = next;
        }

        return dedupeConsecutive(out);
    }

    public double minDistanceMetersToPolyline(GeoPoint point, List<GeoPoint> polyline) {
        if (point == null || polyline == null || polyline.isEmpty())
            return Double.POSITIVE_INFINITY;

        double min = Double.POSITIVE_INFINITY;

        for (GeoPoint p : polyline) {
            if (p == null)
                continue;
            double d = haversineMeters(point.getLatitude(), point.getLongitude(), p.getLatitude(), p.getLongitude());
            if (d < min)
                min = d;
        }

        for (int i = 0; i < polyline.size() - 1; i++) {
            GeoPoint a = polyline.get(i);
            GeoPoint b = polyline.get(i + 1);
            if (a == null || b == null)
                continue;

            double dSeg = pointToSegmentMeters(point, a, b);
            if (dSeg < min)
                min = dSeg;
        }

        return min;
    }

    public boolean isTaskOnRoute(UserTask task, List<GeoPoint> polyline, double radiusMeters) {
        if (task == null || task.getLocationHint() == null)
            return false;
        double d = minDistanceMetersToPolyline(task.getLocationHint(), polyline);
        return d <= radiusMeters;
    }

    private void add(List<GeoPoint> points, GeoPoint p) {
        if (p == null || !p.isComplete())
            return;
        points.add(p);
    }

    private List<GeoPoint> dedupeConsecutive(List<GeoPoint> points) {
        if (points == null || points.isEmpty())
            return List.of();

        List<GeoPoint> out = new ArrayList<>(points.size());
        GeoPoint last = null;
        for (GeoPoint p : points) {
            if (p == null)
                continue;
            if (last == null || !samePoint(last, p)) {
                out.add(p);
                last = p;
            }
        }
        return out;
    }

    private boolean samePoint(GeoPoint a, GeoPoint b) {
        if (a == null || b == null)
            return false;
        return Math.abs(a.getLatitude() - b.getLatitude()) < 1e-8
                && Math.abs(a.getLongitude() - b.getLongitude()) < 1e-8;
    }

    private GeoPoint interpolate(GeoPoint a, GeoPoint b, double t) {
        double lat = a.getLatitude() + (b.getLatitude() - a.getLatitude()) * t;
        double lon = a.getLongitude() + (b.getLongitude() - a.getLongitude()) * t;
        return new GeoPoint(lat, lon);
    }

    private double pointToSegmentMeters(GeoPoint p, GeoPoint a, GeoPoint b) {
        final double R = 6371000.0;

        double lat0 = Math.toRadians((a.getLatitude() + b.getLatitude()) / 2.0);
        double lon0 = Math.toRadians((a.getLongitude() + b.getLongitude()) / 2.0);

        double ax = (Math.toRadians(a.getLongitude()) - lon0) * Math.cos(lat0) * R;
        double ay = (Math.toRadians(a.getLatitude()) - lat0) * R;

        double bx = (Math.toRadians(b.getLongitude()) - lon0) * Math.cos(lat0) * R;
        double by = (Math.toRadians(b.getLatitude()) - lat0) * R;

        double px = (Math.toRadians(p.getLongitude()) - lon0) * Math.cos(lat0) * R;
        double py = (Math.toRadians(p.getLatitude()) - lat0) * R;

        double vx = bx - ax;
        double vy = by - ay;

        double wx = px - ax;
        double wy = py - ay;

        double c1 = vx * wx + vy * wy;
        if (c1 <= 0)
            return Math.hypot(px - ax, py - ay);

        double c2 = vx * vx + vy * vy;
        if (c2 <= c1)
            return Math.hypot(px - bx, py - by);

        double t = c1 / c2;
        double projx = ax + t * vx;
        double projy = ay + t * vy;

        return Math.hypot(px - projx, py - projy);
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
