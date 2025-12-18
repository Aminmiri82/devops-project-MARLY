package org.marly.mavigo.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.error.include-message=always",
                "server.error.include-binding-errors=always",
                "server.error.include-exception=true",
                "server.error.include-stacktrace=never"
        }
)
class JourneyControllerTasksOnRouteTest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired UserTaskRepository userTaskRepository;

    @Test
    void planJourney_shouldReturnTasksOnRoute_whenTaskNearRoute() {
        // 1) créer un user via l'API
        Map<String, Object> createUserPayload = Map.of(
                "displayName", "TestUser",
                "email", "testuser+" + System.currentTimeMillis() + "@example.com",
                "externalId", "ext-" + System.currentTimeMillis()
        );

        ResponseEntity<Map> createdUserResp = rest.postForEntity(
                "/api/users",
                createUserPayload,
                Map.class
        );

        assertTrue(createdUserResp.getStatusCode().is2xxSuccessful(),
                "User creation failed. Status=" + createdUserResp.getStatusCode() + " body=" + createdUserResp.getBody());

        Map userBody = createdUserResp.getBody();
        assertNotNull(userBody);

        String userId = (String) userBody.get("userId");
        assertNotNull(userId, "userId missing in /api/users response. Body=" + userBody);

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalStateException("User not found in DB after creation"));

        // 2) insérer une task locale proche de Gare de Lyon
        UserTask task = new UserTask(user, "local-1", TaskSource.GOOGLE_TASKS, "Acheter du lait");
        task.setLocationHint(new GeoPoint(48.8443, 2.3730)); // Gare de Lyon approx
        task.setCompleted(false);
        userTaskRepository.save(task);

        // 3) departureTime (LocalDateTime sans timezone, sans secondes)
        String departureTime = LocalDateTime.now()
                .plusHours(1)
                .withSecond(0)
                .withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        // 4) appeler /api/journeys
        Map<String, Object> payload = Map.of(
                "journey", Map.of(
                        "userId", userId,
                        "originQuery", "Gare de Lyon",
                        "destinationQuery", "Bastille",
                        "departureTime", departureTime
                ),
                "preferences", Map.of(
                        "comfortMode", false,
                        "touristicMode", false
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = rest.exchange(
                "/api/journeys",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class
        );

        assertEquals(HttpStatus.CREATED, resp.getStatusCode(), "Expected 201 but got " + resp.getStatusCode());

        Map body = resp.getBody();
        assertNotNull(body, "Response body should not be null");

        Object torObj = body.get("tasksOnRoute");
        assertNotNull(torObj, "tasksOnRoute should be present");

        assertTrue(torObj instanceof List, "tasksOnRoute should be a list but was: " + torObj.getClass());
        List tor = (List) torObj;

        assertFalse(tor.isEmpty(), "tasksOnRoute should not be empty. Full body=" + body);

        // chaque item est normalement une Map (JSON object)
        boolean found = tor.stream().anyMatch(it -> {
            if (!(it instanceof Map)) return false;
            Object title = ((Map) it).get("title");
            return "Acheter du lait".equals(String.valueOf(title));
        });

        assertTrue(found, "Expected to find task 'Acheter du lait' in tasksOnRoute. tasksOnRoute=" + tor);
    }
}