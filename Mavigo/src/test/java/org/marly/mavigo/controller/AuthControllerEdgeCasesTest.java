package org.marly.mavigo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AuthControllerEdgeCasesTest {

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authorizedClientService);
    }

    @Test
    void login_buildsRedirectUsingForwardedHeaders() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(request.getHeader("X-Forwarded-Host")).thenReturn("app.example.com");
        when(request.getHeader("X-Forwarded-Port")).thenReturn("443");

        controller.login(request, response);

        verify(response).sendRedirect(contains("https://app.example.com/oauth2/authorization/google"));
    }

    @Test
    void status_returnsUnauthorizedWhenMissingAuthOrUser() {
        ResponseEntity<?> response = controller.status(null, null);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void status_returnsUserInfoWhenAuthenticated() {
        Authentication auth = mock(Authentication.class);
        OidcUser user = mock(OidcUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getFullName()).thenReturn("Example User");

        ResponseEntity<?> response = controller.status(user, auth);

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("authenticated"));
        assertEquals("user@example.com", body.get("email"));
    }

    @Test
    void debugScopes_returnsUnauthorizedWhenNotAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        ResponseEntity<?> response = controller.debugScopes(null, auth);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void debugScopes_returnsEmptyScopesWhenClientMissing() {
        Authentication auth = mock(Authentication.class);
        OidcUser user = mock(OidcUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("principal");
        when(user.getEmail()).thenReturn("user@example.com");
        when(authorizedClientService.loadAuthorizedClient("google", "principal")).thenReturn(null);

        ResponseEntity<?> response = controller.debugScopes(user, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(Set.of(), body.get("scopes"));
    }

    @Test
    void strongLogout_returnsOkWhenAuthenticationIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        ResponseEntity<?> result = controller.strongLogout(request, response, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addHeader(eq(org.springframework.http.HttpHeaders.SET_COOKIE), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void strongLogout_revokesAccessAndRefreshTokens_evenWhenRevokeFails() throws Exception {
        AtomicInteger revokeCalls = new AtomicInteger(0);
        ExchangeFunction failingExchange = req -> {
            revokeCalls.incrementAndGet();
            return Mono.error(new RuntimeException("revoke failed"));
        };
        WebClient failingClient = WebClient.builder().exchangeFunction(failingExchange).build();
        injectGoogleOAuthClient(controller, failingClient);

        Authentication auth = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(auth.getName()).thenReturn("principal");

        OAuth2AuthorizedClient client = buildClientWithRefreshToken("principal");
        when(authorizedClientService.loadAuthorizedClient("google", "principal")).thenReturn(client);

        ResponseEntity<?> result = controller.strongLogout(request, response, auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, revokeCalls.get());
        verify(authorizedClientService).removeAuthorizedClient("google", "principal");
    }

    private void injectGoogleOAuthClient(AuthController target, WebClient webClient) throws Exception {
        Field field = AuthController.class.getDeclaredField("googleOAuthClient");
        field.setAccessible(true);
        field.set(target, webClient);
    }

    private OAuth2AuthorizedClient buildClientWithRefreshToken(String principalName) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/user")
                .userNameAttributeName("sub")
                .clientName("google")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(1800),
                Set.of("openid"));

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token",
                Instant.now());

        return new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
    }
}
