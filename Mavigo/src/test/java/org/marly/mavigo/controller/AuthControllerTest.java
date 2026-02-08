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
import java.util.Set;

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
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.login(request, response);

        String redirectedUrl = response.getRedirectedUrl();
        assertNotNull(redirectedUrl);
        assertTrue(redirectedUrl.startsWith("http://localhost:8080/oauth2/authorization/google"));
        assertTrue(redirectedUrl.contains("scope="));
    }

    @Test
    void login_ShouldHandleForwardedHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "prod.example.com");
        request.addHeader("X-Forwarded-Port", "443");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.login(request, response);

        String redirectedUrl = response.getRedirectedUrl();
        assertNotNull(redirectedUrl);
        assertTrue(redirectedUrl.startsWith("https://prod.example.com/oauth2/authorization/google"));
    }

    @Test
    void login_ShouldHandleDefaultPort80() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "http");
        request.addHeader("X-Forwarded-Host", "example.com");
        request.addHeader("X-Forwarded-Port", "80");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.login(request, response);

        String redirectedUrl = response.getRedirectedUrl();
        assertNotNull(redirectedUrl);
        // Should not include :80 in URL
        assertFalse(redirectedUrl.contains(":80"));
    }

    @Test
    void status_ShouldReturnAuthenticated_WhenUserIsLoggedIn() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("test@example.com");
        when(oidcUser.getFullName()).thenReturn("Test User");

        ResponseEntity<?> response = authController.status(oidcUser, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("authenticated"));
        assertEquals("test@example.com", body.get("email"));
    }

    @Test
    void status_ShouldReturnUnauthenticated_WhenNotLoggedIn() {
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseEntity<?> response = authController.status(null, authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(false, body.get("authenticated"));
    }

    @Test
    void status_ShouldReturnUnauthenticated_WhenAuthIsNull() {
        ResponseEntity<?> response = authController.status(null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void debugScopes_ShouldReturnScopes_WhenAuthenticated() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test-user");
        when(oidcUser.getEmail()).thenReturn("test@example.com");

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(client.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getScopes()).thenReturn(Set.of("openid", "email"));
        when(authorizedClientService.loadAuthorizedClient("google", "test-user")).thenReturn(client);

        ResponseEntity<?> response = authController.debugScopes(oidcUser, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("authenticated"));
        assertNotNull(body.get("scopes"));
    }

    @Test
    void debugScopes_ShouldReturnUnauthorized_WhenNotAuthenticated() {
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseEntity<?> response = authController.debugScopes(oidcUser, authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void debugScopes_ShouldReturnUnauthorized_WhenAuthIsNull() {
        ResponseEntity<?> response = authController.debugScopes(null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void debugScopes_ShouldHandleNullClient() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test-user");
        when(oidcUser.getEmail()).thenReturn("test@example.com");
        when(authorizedClientService.loadAuthorizedClient("google", "test-user")).thenReturn(null);

        ResponseEntity<?> response = authController.debugScopes(oidcUser, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(Set.of(), body.get("scopes"));
    }

    @Test
    void logout_ShouldClearCookiesAndContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authentication.getName()).thenReturn("test-user");
        
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(authorizedClientService.loadAuthorizedClient("google", "test-user")).thenReturn(client);
        
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(client.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("dummy-token");

        ResponseEntity<?> result = authController.strongLogout(request, response, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authorizedClientService).removeAuthorizedClient("google", "test-user");
        assertTrue(response.getHeader("Set-Cookie").contains("JSESSIONID=;"));
    }

    @Test
    void logout_ShouldHandleNullAuthentication() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<?> result = authController.strongLogout(request, response, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("ok"));
    }

    @Test
    void logout_ShouldHandleNullClient() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authentication.getName()).thenReturn("test-user");
        when(authorizedClientService.loadAuthorizedClient("google", "test-user")).thenReturn(null);

        ResponseEntity<?> result = authController.strongLogout(request, response, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void logout_ShouldRevokeRefreshToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authentication.getName()).thenReturn("test-user");

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(authorizedClientService.loadAuthorizedClient("google", "test-user")).thenReturn(client);

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        OAuth2RefreshToken refreshToken = mock(OAuth2RefreshToken.class);
        when(client.getAccessToken()).thenReturn(accessToken);
        when(client.getRefreshToken()).thenReturn(refreshToken);
        when(accessToken.getTokenValue()).thenReturn("access-token");
        when(refreshToken.getTokenValue()).thenReturn("refresh-token");

        ResponseEntity<?> result = authController.strongLogout(request, response, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}

