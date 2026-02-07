package org.marly.mavigo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private Authentication authentication;

    @Mock
    private OidcUser oidcUser;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authorizedClientService);
    }

    @Test
    void login_ShouldRedirectToGoogleAuth() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        authController.login(request, response);

        // Then
        String redirectedUrl = response.getRedirectedUrl();
        assertNotNull(redirectedUrl);
        assertTrue(redirectedUrl.startsWith("http://localhost:8080/oauth2/authorization/google"));
        assertTrue(redirectedUrl.contains("scope="));
    }

    @Test
    void status_ShouldReturnAuthenticated_WhenUserIsLoggedIn() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("test@example.com");
        when(oidcUser.getFullName()).thenReturn("Test User");

        // When
        ResponseEntity<?> response = authController.status(oidcUser, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("authenticated"));
        assertEquals("test@example.com", body.get("email"));
    }

    @Test
    void status_ShouldReturnUnauthenticated_WhenNotLoggedIn() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        ResponseEntity<?> response = authController.status(null, authentication);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(false, body.get("authenticated"));
    }

    @Test
    void logout_ShouldClearCookiesAndContext() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authentication.getName()).thenReturn("test-user");
        
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(authorizedClientService.loadAuthorizedClient("google", "test-user")).thenReturn(client);
        
        // Mock token to avoid NPE if controller checks it
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(client.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("dummy-token");

        // When
        ResponseEntity<?> result = authController.strongLogout(request, response, authentication);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authorizedClientService).removeAuthorizedClient("google", "test-user");
        
        // Verify cookie clearing
        assertTrue(response.getHeader("Set-Cookie").contains("JSESSIONID=;"));
    }
}
