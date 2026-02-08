package org.marly.mavigo.models.alert;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TrafficAlertTest {
    @Test
    void testGettersAndSetters() {
        OffsetDateTime now = OffsetDateTime.now();
        TrafficAlert alert = new TrafficAlert("src-1", AlertSeverity.CRITICAL, "Fire", now);
        alert.setDescription("Big fire");
        alert.setLineCode("LineA");
        alert.replaceAffectedStopIds(List.of("stop1"));
        alert.setValidUntil(now.plusHours(1));
        alert.setSourceAlertId("new-src");
        alert.setSeverity(AlertSeverity.LOW);
        alert.setTitle("New Title");
        alert.setValidFrom(now.minusHours(1));
        
        assertEquals("new-src", alert.getSourceAlertId());
        assertEquals(AlertSeverity.LOW, alert.getSeverity());
        assertEquals("New Title", alert.getTitle());
        assertEquals("Big fire", alert.getDescription());
        assertEquals("LineA", alert.getLineCode());
        assertEquals(1, alert.getAffectedStopIds().size());
        assertNotNull(alert.getValidFrom());
        assertNotNull(alert.getValidUntil());
    }
}
