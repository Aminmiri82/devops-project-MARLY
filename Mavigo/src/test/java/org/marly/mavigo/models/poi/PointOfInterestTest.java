package org.marly.mavigo.models.poi;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PointOfInterestTest {
    @Test
    void testGettersAndSetters() {
        PointOfInterest poi = new PointOfInterest("ext-1", "Louvre", PointOfInterestCategory.MUSEUM);
        poi.setLocation(new GeoPoint(48.86, 2.33));
        poi.setAverageRating(new BigDecimal("4.8"));
        poi.setReviewCount(5000);
        poi.setSource("Google");
        poi.setPrimaryPhotoUrl("http://img.jpg");
        poi.setShortDescription("Museum");
        poi.setExternalId("new-ext");
        poi.setName("New Name");
        poi.setCategory(PointOfInterestCategory.RESTAURANT);
        
        assertEquals("new-ext", poi.getExternalId());
        assertEquals("New Name", poi.getName());
        assertEquals(PointOfInterestCategory.RESTAURANT, poi.getCategory());
        assertNotNull(poi.getLocation());
        assertEquals(new BigDecimal("4.8"), poi.getAverageRating());
        assertEquals(5000, poi.getReviewCount());
        assertEquals("Google", poi.getSource());
        assertEquals("http://img.jpg", poi.getPrimaryPhotoUrl());
        assertEquals("Museum", poi.getShortDescription());
    }
}
