package org.marly.mavigo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Test
    void generateToken_andParseClaims_roundTripsSubjectAndEmail() {
        JwtTokenService service = new JwtTokenService(SECRET, 3600);
        User user = new User("ext-1", "user@example.com", "User");
        user.setId(UUID.randomUUID());

        String token = service.generateToken(user);
        Claims claims = service.parseClaims(token);

        assertNotNull(token);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("user@example.com", claims.get("email", String.class));
    }

    @Test
    void parseClaims_throwsForInvalidToken() {
        JwtTokenService service = new JwtTokenService(SECRET, 3600);

        assertThrows(JwtException.class, () -> service.parseClaims("not-a-jwt"));
    }

    @Test
    void generateToken_setsExpirationBasedOnConfiguredSeconds() {
        long expirationSeconds = 120;
        JwtTokenService service = new JwtTokenService(SECRET, expirationSeconds);
        User user = new User("ext-2", "time@example.com", "Time User");
        user.setId(UUID.randomUUID());

        Instant before = Instant.now();
        Claims claims = service.parseClaims(service.generateToken(user));
        Instant after = Instant.now();

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(!claims.getIssuedAt().toInstant().isBefore(before.minusSeconds(1)));
        assertTrue(!claims.getIssuedAt().toInstant().isAfter(after.plusSeconds(1)));
        assertTrue(claims.getExpiration().toInstant().isAfter(claims.getIssuedAt().toInstant()));
    }
}
