package org.marly.mavigo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/h2-console/**", "/").permitAll()
                        .requestMatchers("/api/google/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(o -> o.defaultSuccessUrl("/api/google/tasks/me", false))
                .oauth2Client(withDefaults())
                .headers(h -> h.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}