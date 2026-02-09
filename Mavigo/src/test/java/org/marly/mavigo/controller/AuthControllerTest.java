package org.marly.mavigo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.config.CustomUserDetailsService;
import org.marly.mavigo.config.JwtUtils;
import org.marly.mavigo.security.JwtAuthenticationFilter;
import org.marly.mavigo.security.JwtTokenService;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@DisplayName("Tests unitaires - AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private UserRepository userRepository;

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
    @DisplayName("POST /auth/logout avec CSRF devrait retourner une réponse réussie ou redirection")
    void logout_withCsrf_shouldReturnSuccessfulOrRedirect() throws Exception {
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

    @Test
    @DisplayName("POST /auth/logout sans CSRF devrait échouer")
    void logout_withoutCsrf_shouldFail() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isForbidden());
    }
}
