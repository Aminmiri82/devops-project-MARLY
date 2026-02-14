package org.marly.mavigo.service.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

    @Test
    void validate_acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordValidator.validate("Abcd1234"));
    }

    @Test
    void validate_rejectsBlankPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordValidator.validate("   "));
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    void validate_rejectsTooShortPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordValidator.validate("Abc123"));
        assertTrue(ex.getMessage().contains("at least"));
    }

    @Test
    void validate_rejectsPasswordWithoutUppercase() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordValidator.validate("abcd1234"));
        assertTrue(ex.getMessage().contains("uppercase"));
    }

    @Test
    void validate_rejectsPasswordWithoutLowercase() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordValidator.validate("ABCD1234"));
        assertTrue(ex.getMessage().contains("lowercase"));
    }

    @Test
    void validate_rejectsPasswordWithoutDigit() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordValidator.validate("Abcdefgh"));
        assertTrue(ex.getMessage().contains("digit"));
    }
}
