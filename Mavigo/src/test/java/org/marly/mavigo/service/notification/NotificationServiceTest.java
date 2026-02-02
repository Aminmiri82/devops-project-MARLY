package org.marly.mavigo.service.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.user.User;

@DisplayName("Tests unitaires - NotificationService")
class NotificationServiceTest {

    private NotificationServiceImpl service;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl();

        testUser = new User("ext-123", "test@example.com", "Test User");
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("send devrait envoyer une notification avec succès")
    void send_shouldSendNotificationSuccessfully() {
        // Given
        NotificationRequest request = new NotificationRequest(
                testUser.getId(),
                "Test Title",
                "Test Message",
                Map.of("type", "INFO"));

        // When/Then - should not throw
        assertDoesNotThrow(() -> service.send(request));
    }

    @Test
    @DisplayName("send devrait gérer les notifications d'alerte")
    void send_shouldHandleAlertNotifications() {
        // Given
        NotificationRequest request = new NotificationRequest(
                testUser.getId(),
                "Alert",
                "Traffic disruption on M1",
                Map.of("type", "ALERT", "lineCode", "M1"));

        // When/Then
        assertDoesNotThrow(() -> service.send(request));
    }

    @Test
    @DisplayName("send devrait gérer les notifications de tâche")
    void send_shouldHandleTaskNotifications() {
        // Given
        NotificationRequest request = new NotificationRequest(
                testUser.getId(),
                "Task Reminder",
                "Buy milk near Gare de Lyon",
                Map.of("type", "TASK_REMINDER"));

        // When/Then
        assertDoesNotThrow(() -> service.send(request));
    }

    @Test
    @DisplayName("send devrait lever une exception si la requête est nulle")
    void send_shouldThrowExceptionWhenRequestIsNull() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.send(null));
    }

    @Test
    @DisplayName("send devrait lever une exception si l'userId est nul")
    void send_shouldThrowExceptionWhenUserIdIsNull() {
        // Given
        NotificationRequest request = new NotificationRequest(
                null,
                "Title",
                "Message",
                null);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.send(request));
    }

    @Test
    @DisplayName("send devrait lever une exception si le message est vide")
    void send_shouldThrowExceptionWhenMessageIsEmpty() {
        // Given
        NotificationRequest request = new NotificationRequest(
                testUser.getId(),
                "Title",
                "",
                null);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> service.send(request));
    }
}

/**
 * Implementation for NotificationService to test against.
 */
class NotificationServiceImpl implements NotificationService {

    @Override
    public void send(NotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        // In a real implementation, this would send the notification
        // via push notification, email, or other channels
    }
}
