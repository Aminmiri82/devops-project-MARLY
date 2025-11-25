package org.marly.mavigo.client.google;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/google/tasks/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        return Map.of(
                "sub", principal.getAttribute("sub"),
                "email", principal.getAttribute("email"),
                "name", principal.getAttribute("name"),
                "issuer", principal.getAttribute("iss")
        );
    }
}
