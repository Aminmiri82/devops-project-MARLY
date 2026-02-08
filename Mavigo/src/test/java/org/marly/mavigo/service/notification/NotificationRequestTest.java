package org.marly.mavigo.service.notification;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NotificationRequestTest {
    @Test
    void testGetters() {
        UUID userId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(userId, "Title", "Body", Map.of("k", "v"));
        
        assertEquals(userId, request.getUserId());
        assertEquals("Title", request.getTitle());
        assertEquals("Body", request.getBody());
        assertEquals("v", request.getMetadata().get("k"));
    }
}
