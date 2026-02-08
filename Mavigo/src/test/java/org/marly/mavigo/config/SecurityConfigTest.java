package org.marly.mavigo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "PRIM_API_KEY=test-key")
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
        assertNotNull(securityConfig);
    }

    @Test
    void googleAuthRequestResolver_ShouldBeCreated() {
        if (clientRegistrationRepository != null) {
            assertNotNull(securityConfig.googleAuthRequestResolver(clientRegistrationRepository));
        }
    }
}
