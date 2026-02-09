package org.marly.mavigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.marly.mavigo.service.journey.JourneyActionResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.config.CustomUserDetailsService;
import org.marly.mavigo.config.JwtUtils;
import org.marly.mavigo.security.JwtAuthenticationFilter;
import org.marly.mavigo.security.JwtTokenService;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.journey.JourneyManagementService;
import org.marly.mavigo.service.journey.JourneyOptimizationService;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.TaskOnRouteService;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(JourneyController.class)
@DisplayName("Tests unitaires - JourneyController")
class JourneyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JourneyPlanningService journeyPlanningService;

    @MockitoBean
    private UserTaskRepository userTaskRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TaskOnRouteService taskOnRouteService;

    @MockitoBean
    private JourneyManagementService journeyManagementService;

    @MockitoBean
    private JourneyOptimizationService journeyOptimizationService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    @DisplayName("POST /api/journeys devrait créer un nouveau trajet")
    void planJourney_shouldCreateNewJourney() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User("ext-123", "test@example.com", "Test User");
        user.setId(userId);

        Journey mockJourney = createMockJourney(user);

        when(journeyPlanningService.planAndPersist(any(JourneyPlanningParameters.class)))
                .thenReturn(List.of(mockJourney));
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());

        String requestBody = """
                {
                    "journey": {
                        "userId": "%s",
                        "originQuery": "Gare de Lyon",
                        "destinationQuery": "Châtelet",
                        "departureTime": "2025-12-14T18:00:00"
                    },
                    "preferences": {
                        "comfortMode": false
                    }
                }
                """.formatted(userId);

        // When/Then
        mockMvc.perform(post("/api/journeys")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/journeys avec préférences de confort devrait créer un trajet confort")
    void planJourney_withComfortPreferences_shouldCreateComfortJourney() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID comfortSettingId = UUID.randomUUID();
        User user = new User("ext-123", "test@example.com", "Test User");
        user.setId(userId);

        Journey mockJourney = createMockJourney(user);
        mockJourney.setComfortModeEnabled(true);

        when(journeyPlanningService.planAndPersist(any(JourneyPlanningParameters.class)))
                .thenReturn(List.of(mockJourney));
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());

        String requestBody = """
                {
                    "journey": {
                        "userId": "%s",
                        "originQuery": "Gare de Lyon",
                        "destinationQuery": "Châtelet",
                        "departureTime": "2025-12-14T18:00:00"
                    },
                    "preferences": {
                        "comfortMode": true,
                        "namedComfortSettingId": "%s"
                    }
                }
                """.formatted(userId, comfortSettingId);

        // When/Then
        mockMvc.perform(post("/api/journeys")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/journeys/{id}/start devrait démarrer un trajet")
    void startJourney_shouldStartJourney() throws Exception {
        // Given
        UUID journeyId = UUID.randomUUID();
        User user = new User("ext-123", "test@example.com", "Test User");
        Journey mockJourney = createMockJourney(user);
        mockJourney.setStatus(JourneyStatus.IN_PROGRESS);

        when(journeyManagementService.startJourney(journeyId))
                .thenReturn(new JourneyActionResult(mockJourney, java.util.Collections.emptyList()));

        // When/Then
        mockMvc.perform(post("/api/journeys/{id}/start", journeyId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/journeys/{id}/complete devrait compléter un trajet")
    void completeJourney_shouldCompleteJourney() throws Exception {
        // Given
        UUID journeyId = UUID.randomUUID();
        User user = new User("ext-123", "test@example.com", "Test User");
        Journey mockJourney = createMockJourney(user);
        mockJourney.setStatus(JourneyStatus.COMPLETED);

        when(journeyManagementService.completeJourney(journeyId))
                .thenReturn(new JourneyActionResult(mockJourney, java.util.Collections.emptyList()));

        // When/Then
        mockMvc.perform(post("/api/journeys/{id}/complete", journeyId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/journeys/{id}/cancel devrait annuler un trajet")
    void cancelJourney_shouldCancelJourney() throws Exception {
        // Given
        UUID journeyId = UUID.randomUUID();
        User user = new User("ext-123", "test@example.com", "Test User");
        Journey mockJourney = createMockJourney(user);
        mockJourney.setStatus(JourneyStatus.CANCELLED);

        when(journeyManagementService.cancelJourney(journeyId)).thenReturn(mockJourney);

        // When/Then
        mockMvc.perform(post("/api/journeys/{id}/cancel", journeyId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/journeys/{id} devrait retourner un trajet")
    void getJourney_shouldReturnJourney() throws Exception {
        // Given
        UUID journeyId = UUID.randomUUID();
        User user = new User("ext-123", "test@example.com", "Test User");
        Journey mockJourney = createMockJourney(user);

        when(journeyManagementService.getJourney(journeyId)).thenReturn(mockJourney);

        // When/Then
        mockMvc.perform(get("/api/journeys/{id}", journeyId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/journeys sans authentification ni CSRF devrait être rejeté")
    void planJourney_withoutAuthAndCsrf_shouldBeForbidden() throws Exception {
        String requestBody = """
                {
                    "journey": {
                        "userId": "123e4567-e89b-12d3-a456-426614174000",
                        "originQuery": "Gare de Lyon",
                        "destinationQuery": "Châtelet",
                        "departureTime": "2025-12-14T18:00:00"
                    }
                }
                """;

        // Without CSRF token, POST returns 403 Forbidden
        mockMvc.perform(post("/api/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/journeys sans CSRF devrait retourner 403")
    void planJourney_withoutCsrf_shouldReturn403() throws Exception {
        String requestBody = """
                {
                    "journey": {
                        "userId": "123e4567-e89b-12d3-a456-426614174000",
                        "originQuery": "Gare de Lyon",
                        "destinationQuery": "Châtelet",
                        "departureTime": "2025-12-14T18:00:00"
                    }
                }
                """;

        mockMvc.perform(post("/api/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/journeys/debug/user-tasks devrait retourner les tâches utilisateur")
    void debugUserTasks_shouldReturnUserTasks() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        when(userTaskRepository.findByUser_Id(userId)).thenReturn(List.of());

        // When/Then
        mockMvc.perform(get("/api/journeys/debug/user-tasks")
                .param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    // Helper methods

    private Journey createMockJourney(User user) {
        Journey journey = new Journey(
                user,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        journey.setStatus(JourneyStatus.PLANNED);
        return journey;
    }
}
