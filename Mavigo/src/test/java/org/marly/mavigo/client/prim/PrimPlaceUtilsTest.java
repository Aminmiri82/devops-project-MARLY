package org.marly.mavigo.client.prim;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.client.prim.model.PrimStopPoint;

import static org.junit.jupiter.api.Assertions.*;

class PrimPlaceUtilsTest {

    @Test
    void hasStopAreaOrPoint_shouldReturnTrueForStopArea() {
        PrimStopArea sa = new PrimStopArea("id", "name", null);
        PrimPlace place = new PrimPlace("id", "name", "stop_area", sa, null, null);
        assertTrue(PrimPlaceUtils.hasStopAreaOrPoint(place));
    }

    @Test
    void hasStopAreaOrPoint_shouldReturnTrueForStopPoint() {
        PrimStopPoint sp = new PrimStopPoint("id", "name", null, null);
        PrimPlace place = new PrimPlace("id", "name", "stop_point", null, sp, null);
        assertTrue(PrimPlaceUtils.hasStopAreaOrPoint(place));
    }

    @Test
    void hasStopAreaOrPoint_shouldReturnFalseForNullOrEmpty() {
        assertFalse(PrimPlaceUtils.hasStopAreaOrPoint(null));
        PrimPlace place = new PrimPlace("id", "name", "address", null, null, null);
        assertFalse(PrimPlaceUtils.hasStopAreaOrPoint(place));
    }

    @Test
    void placeCoordinates_shouldReturnCoordinatesFromPlace() {
        PrimCoordinates coords = new PrimCoordinates(1.0, 2.0);
        PrimPlace place = new PrimPlace("id", "name", "address", null, null, coords);
        assertEquals(coords, PrimPlaceUtils.placeCoordinates(place));
    }

    @Test
    void placeCoordinates_shouldReturnCoordinatesFromStopArea() {
        PrimCoordinates coords = new PrimCoordinates(1.0, 2.0);
        PrimStopArea sa = new PrimStopArea("id", "name", coords);
        PrimPlace place = new PrimPlace("id", "name", "stop_area", sa, null, null);
        assertEquals(coords, PrimPlaceUtils.placeCoordinates(place));
    }
    
    @Test
    void placeCoordinates_shouldReturnCoordinatesFromStopPoint() {
        PrimCoordinates coords = new PrimCoordinates(1.0, 2.0);
        PrimStopPoint sp = new PrimStopPoint("id", "name", coords, null);
        PrimPlace place = new PrimPlace("id", "name", "stop_point", null, sp, null);
        assertEquals(coords, PrimPlaceUtils.placeCoordinates(place));
    }

    @Test
    void calculateDistance_shouldReturnCorrectDistance() {
        double dist = PrimPlaceUtils.calculateDistance(48.8566, 2.3522, 48.8584, 2.2945); // Paris center to Eiffel
        // Approx 4.2km
        assertTrue(dist > 4000 && dist < 4500); 
    }
}
