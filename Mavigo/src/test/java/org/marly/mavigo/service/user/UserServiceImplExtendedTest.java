package org.marly.mavigo.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.NamedComfortSettingRepository;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplExtendedTest {

    @Mock private UserRepository userRepository;
    @Mock private NamedComfortSettingRepository namedComfortSettingRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, namedComfortSettingRepository, passwordEncoder);
    }

    // ── createUserFromRegistration ──

    @Test
    void createUserFromRegistration_happyPath() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            return u;
        });

        User result = service.createUserFromRegistration("John", "Doe", "john@example.com", "Password1!", "123 St");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John Doe", result.getDisplayName());
        assertNotNull(result.getHomeAddress());
        verify(passwordEncoder).encode("Password1!");
    }

    @Test
    void createUserFromRegistration_withoutHomeAddress() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            return u;
        });

        User result = service.createUserFromRegistration("Jane", "Smith", "jane@example.com", "Password1!", null);
        assertNotNull(result);
        assertNull(result.getHomeAddress());
    }

    @Test
    void createUserFromRegistration_rejectsMissingFields() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createUserFromRegistration("", "Doe", "a@b.com", "p", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createUserFromRegistration("John", "", "a@b.com", "p", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createUserFromRegistration("John", "Doe", "", "p", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createUserFromRegistration("John", "Doe", "a@b.com", "", null));
    }

    // ── login ──

    @Test
    void login_happyPath() {
        User user = new User("ext-1", "a@example.com", "User A");
        user.setPasswordHash("encoded");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPwd", "encoded")).thenReturn(true);

        User result = service.login("a@example.com", "correctPwd");
        assertEquals(user, result);
    }

    @Test
    void login_rejectsNonExistentUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.login("missing@example.com", "pwd"));
    }

    @Test
    void login_rejectsNullPasswordHash() {
        User user = new User("ext-1", "a@example.com", "User A");
        // passwordHash is null by default
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> service.login("a@example.com", "pwd"));
    }

    // ── updateUser ──

    @Test
    void updateUser_happyPath() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User result = service.updateUser(user);
        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_rejectsNullUser() {
        assertThrows(NullPointerException.class, () -> service.updateUser(null));
    }

    @Test
    void updateUser_rejectsNullUserId() {
        User user = new User("ext-1", "a@example.com", "User A");
        // id is null
        assertThrows(NullPointerException.class, () -> service.updateUser(user));
    }

    // ── deleteUser ──

    @Test
    void deleteUser_happyPath() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));

        service.deleteUser(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_throwsOnMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.deleteUser(userId));
    }

    // ── markComfortPromptAsSeen ──

    @Test
    void markComfortPromptAsSeen_setsFlag() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.markComfortPromptAsSeen(userId);
        assertTrue(result.getHasSeenComfortPrompt());
    }

    // ── updateNamedComfortSetting success ──

    @Test
    void updateNamedComfortSetting_happyPath() {
        UUID userId = UUID.randomUUID();
        UUID settingId = UUID.randomUUID();

        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        NamedComfortSetting setting = new NamedComfortSetting("Work", new ComfortProfile(), user);

        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));
        when(namedComfortSettingRepository.findById(settingId)).thenReturn(Optional.of(setting));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ComfortProfile newProfile = new ComfortProfile();
        newProfile.setWheelchairAccessible(true);

        User result = service.updateNamedComfortSetting(userId, settingId, "Updated", newProfile);
        assertNotNull(result);
        assertEquals("Updated", setting.getName());
    }

    // ── linkGoogleAccount fresh link ──

    @Test
    void linkGoogleAccount_freshLinkSavesUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByGoogleAccountSubject("sub-new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkGoogleAccount(userId, new GoogleAccountLink("sub-new", "google@example.com"));

        assertEquals("sub-new", result.getGoogleAccountSubject());
        assertEquals("google@example.com", result.getGoogleAccountEmail());
        assertNotNull(result.getGoogleLinkedAt());
        verify(userRepository).save(user);
    }

    @Test
    void linkGoogleAccount_rejectsNullUserId() {
        assertThrows(NullPointerException.class,
                () -> service.linkGoogleAccount(null, new GoogleAccountLink("sub", "e@x.com")));
    }

    @Test
    void linkGoogleAccount_rejectsBlankSubject() {
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> service.linkGoogleAccount(userId, new GoogleAccountLink("", "e@x.com")));
    }

    // ── getUser ──

    @Test
    void getUser_throwsOnNullUserId() {
        assertThrows(NullPointerException.class, () -> service.getUser(null));
    }

    @Test
    void getUser_throwsOnNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.getUser(userId));
    }

    // ── createUser happy path ──

    @Test
    void createUser_happyPath() {
        User user = new User("ext-1", "a@example.com", "User A");
        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            return u;
        });

        User result = service.createUser(user);
        assertNotNull(result.getId());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void createUser_rejectsNullUser() {
        assertThrows(NullPointerException.class, () -> service.createUser(null));
    }
}
