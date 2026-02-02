package org.marly.mavigo.Integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - AuthController")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /auth/login devrait rediriger vers OAuth2")
    void login_shouldRedirectToOAuth2() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /auth/status sans authentification devrait rediriger vers login")
    void status_withoutAuth_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/auth/status"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /auth/debug/scopes sans authentification devrait rediriger vers login")
    void debugScopes_withoutAuth_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/auth/debug/scopes"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /auth/logout avec CSRF devrait réussir ou rediriger")
    void logout_withCsrf_shouldSucceedOrRedirect() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    boolean isSuccessOrRedirect = (status >= 200 && status < 400);
                    if (!isSuccessOrRedirect) {
                        throw new AssertionError("Expected status 2xx or 3xx but was: " + status);
                    }
                });
    }
}
