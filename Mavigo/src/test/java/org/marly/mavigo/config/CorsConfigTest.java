package org.marly.mavigo.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_ShouldBeConfigured() {
        CorsConfig config = new CorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();
        
        assertNotNull(source);
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/any");
        
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(corsConfig.getAllowCredentials());
        assertTrue(corsConfig.getAllowedMethods().contains("PATCH"));
    }
}
