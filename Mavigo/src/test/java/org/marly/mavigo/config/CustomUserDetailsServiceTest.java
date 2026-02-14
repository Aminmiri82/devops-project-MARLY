package org.marly.mavigo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {
        String email = "test@example.com";
        User user = new User("ext-id", email, "Test User");
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(email);

        assertNotNull(details);
        assertEquals(email, details.getUsername());
        assertEquals("hashedPassword", details.getPassword());
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWithNoopWhenPasswordIsNull() {
        String email = "test@example.com";
        User user = new User("ext-id", email, "Test User");
        user.setEmail(email);
        user.setPasswordHash(null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(email);

        assertNotNull(details);
        assertEquals("{noop}", details.getPassword());
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionWhenUserDoesNotExist() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(email));
    }
}
