// src/main/java/org/marly/mavigo/config/SecurityConfig.java
package org.marly.mavigo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public OAuth2AuthorizationRequestResolver googleAuthRequestResolver(ClientRegistrationRepository clientRepo) {
        DefaultOAuth2AuthorizationRequestResolver base =
                new DefaultOAuth2AuthorizationRequestResolver(clientRepo, "/oauth2/authorization");

        base.setAuthorizationRequestCustomizer(builder -> {
            Set<String> scopes = new HashSet<>(Arrays.asList(
                    "openid", "profile", "email", "https://www.googleapis.com/auth/tasks"
            ));
            builder.scopes(scopes);

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("prompt", "consent select_account");
            params.put("access_type", "offline");
            params.put("include_granted_scopes", "true");
            builder.additionalParameters(params);
        });

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return base.resolve(request);
            }
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return base.resolve(request, clientRegistrationId);
            }
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    OAuth2AuthorizationRequestResolver googleAuthRequestResolver) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/google/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(ae -> ae.authorizationRequestResolver(googleAuthRequestResolver))
                        // After successful OAuth, redirect to saved request (e.g. /api/google/tasks/link) or homepage
                        .defaultSuccessUrl("/", false)
                )
                .oauth2Client(withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                )
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults());

        return http.build();
    }
}