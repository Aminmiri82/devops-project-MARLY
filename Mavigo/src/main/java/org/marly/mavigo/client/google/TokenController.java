package org.marly.mavigo.client.google;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

    @GetMapping("/api/google/tasks/token")
    public Map<String, Object> token(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        Set<String> scopes = client.getAccessToken().getScopes();
        Instant expiresAt = client.getAccessToken().getExpiresAt();
        return Map.of("scopes", scopes, "expiresAt", expiresAt);
    }
}
