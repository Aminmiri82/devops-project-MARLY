package org.marly.mavigo.service.notification;

import java.util.Map;
import java.util.UUID;

public class NotificationRequest {
    //email notifications are gonna be sent to the user's email address
    private final UUID userId;
    private final String title;
    private final String body;
    private final Map<String, Object> metadata;

    public NotificationRequest(UUID userId, String title, String body, Map<String, Object> metadata) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.metadata = metadata;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

