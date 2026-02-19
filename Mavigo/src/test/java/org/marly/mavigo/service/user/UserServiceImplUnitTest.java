package org.marly.mavigo.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NamedComfortSettingRepository namedComfortSettingRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, namedComfortSettingRepository, passwordEncoder);
    }

    @Test
    void createUser_rejectsMissingRequiredFields() {
        User missingExternalId = new User(" ", "a@example.com", "User A");
        assertThrows(IllegalArgumentException.class, () -> service.createUser(missingExternalId));
    }

    @Test
    void createUser_rejectsDuplicateExternalIdAndEmail() {
        User user = new User("ext-1", "a@example.com", "User A");
        User existingExternal = new User("ext-1", "other@example.com", "Existing Ext");
        existingExternal.setId(UUID.randomUUID());
        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.of(existingExternal));

        assertThrows(UserAlreadyExistsException.class, () -> service.createUser(user));

        User existingEmail = new User("ext-2", "a@example.com", "Existing Mail");
        existingEmail.setId(UUID.randomUUID());
        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(existingEmail));

        assertThrows(UserAlreadyExistsException.class, () -> service.createUser(user));
    }

    @Test
    void login_rejectsBlankOrWrongCredentials() {
        assertThrows(IllegalArgumentException.class, () -> service.login(" ", "pwd"));

        User user = new User("ext-1", "a@example.com", "User A");
        user.setPasswordHash("encoded");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.login("a@example.com", "bad"));
    }

    @Test
    void linkGoogleAccount_returnsSameUserWhenSubjectAlreadyLinkedToUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        user.setGoogleAccountSubject("sub-123");
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));

        User result = service.linkGoogleAccount(userId, new GoogleAccountLink("sub-123", "google@example.com"));

        assertEquals(user, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void linkGoogleAccount_rejectsSubjectAlreadyUsedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));

        User existing = new User("ext-2", "b@example.com", "User B");
        existing.setId(UUID.randomUUID());
        when(userRepository.findByGoogleAccountSubject("sub-dup")).thenReturn(Optional.of(existing));

        GoogleAccountLink dupLink = new GoogleAccountLink("sub-dup", "dup@example.com");
        assertThrows(GoogleAccountAlreadyLinkedException.class,
                () -> service.linkGoogleAccount(userId, dupLink));
    }

    @Test
    void addNamedComfortSetting_appendsToExistingSettings() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        user.addNamedComfortSetting(new NamedComfortSetting("Existing", new ComfortProfile(), user));
        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.addNamedComfortSetting(userId, "New", new ComfortProfile());

        assertEquals(2, saved.getNamedComfortSettings().size());
        assertTrue(saved.getNamedComfortSettings().stream().anyMatch(s -> "Existing".equals(s.getName())));
        assertTrue(saved.getNamedComfortSettings().stream().anyMatch(s -> "New".equals(s.getName())));
    }

    @Test
    void updateNamedComfortSetting_rejectsOwnershipMismatch() {
        UUID userId = UUID.randomUUID();
        UUID settingId = UUID.randomUUID();

        User owner = new User("ext-owner", "owner@example.com", "Owner");
        owner.setId(userId);
        User otherUser = new User("ext-other", "other@example.com", "Other");
        otherUser.setId(UUID.randomUUID());

        NamedComfortSetting setting = new NamedComfortSetting("Work", new ComfortProfile(), otherUser);

        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(owner));
        when(namedComfortSettingRepository.findById(settingId)).thenReturn(Optional.of(setting));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateNamedComfortSetting(userId, settingId, "Updated", new ComfortProfile()));
    }

    @Test
    void deleteNamedComfortSetting_deletesAndPersistsUser() {
        UUID userId = UUID.randomUUID();
        UUID settingId = UUID.randomUUID();

        User user = new User("ext-1", "a@example.com", "User A");
        user.setId(userId);
        NamedComfortSetting setting = new NamedComfortSetting("Home", new ComfortProfile(), user);
        user.addNamedComfortSetting(setting);

        when(userRepository.findWithSettingsById(userId)).thenReturn(Optional.of(user));
        when(namedComfortSettingRepository.findById(settingId)).thenReturn(Optional.of(setting));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.deleteNamedComfortSetting(userId, settingId);

        assertNotNull(saved);
        assertFalse(saved.getNamedComfortSettings().contains(setting));
        verify(namedComfortSettingRepository, times(1)).delete(setting);
        verify(userRepository, times(1)).save(user);
    }
}
