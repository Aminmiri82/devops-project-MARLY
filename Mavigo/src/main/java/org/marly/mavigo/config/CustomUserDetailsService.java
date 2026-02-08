package org.marly.mavigo.config;

import java.util.Collections;

import org.marly.mavigo.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(u -> new User(
                        u.getEmail(),
                        u.getPasswordHash() != null ? u.getPasswordHash() : "{noop}",
                        Collections.emptyList()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
