package org.marly.mavigo.models.weather;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

class WeatherConditionsTest {
    @Test
    void testGetters() {
        OffsetDateTime now = OffsetDateTime.now();
        WeatherConditions wc = new WeatherConditions(20.5, 0.1, false, now);
        
        assertEquals(20.5, wc.getTemperatureCelsius());
        assertEquals(0.1, wc.getPrecipitationProbability());
        assertFalse(wc.isPrecipitationExpected());
        assertEquals(now, wc.getObservedAt());
    }
}
