package org.marly.mavigo.client.prim;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PrimJourneyRequestTest {

    @Test
    void builderMethods_ShouldWork() {
        LocalDateTime now = LocalDateTime.now();
        PrimJourneyRequest request = new PrimJourneyRequest("from", "to", now);
        
        request.withMaxDuration(100)
               .withMaxNbTransfers(2)
               .withWheelchair(true)
               .withRealtime(false)
               .withMaxWaitingDuration(5)
               .withMaxWalkingDurationToPt(10)
               .withDirectPath("true")
               .withEquipmentDetails(true);
               
        assertEquals("from", request.getFromStopAreaId());
        assertEquals("to", request.getToStopAreaId());
        assertEquals(now, request.getDatetime());
        assertEquals("departure", request.getDatetimeRepresents());
        
        assertEquals(100, request.getMaxDuration().get());
        assertEquals(2, request.getMaxNbTransfers().get());
        assertTrue(request.getWheelchair().get());
        assertFalse(request.getRealtime().get());
        assertEquals(5, request.getMaxWaitingDuration().get());
        assertEquals(10, request.getMaxWalkingDurationToPt().get());
        assertEquals("true", request.getDirectPath().get());
        assertTrue(request.getEquipmentDetails().get());
    }
}
