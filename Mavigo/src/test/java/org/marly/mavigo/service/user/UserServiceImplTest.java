package org.marly.mavigo.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    private void setEntityId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== createUser tests ==========

    @Test
    void createUser_ShouldCreateAndReturnUser() {
        User user = new User("ext-1", "test@test.com", "Test User");
        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createUser(user);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void createUser_ShouldThrowWhenUserIsNull() {
        assertThrows(NullPointerException.class, () -> userService.createUser(null));
    }

    @Test
    void createUser_ShouldThrowWhenExternalIdExists() {
        User existing = new User("ext-1", "other@test.com", "Other User");
        setEntityId(existing, UUID.randomUUID());
        User newUser = new User("ext-1", "new@test.com", "New User");

        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.of(existing));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(newUser));
    }

    @Test
    void createUser_ShouldThrowWhenEmailExists() {
        User existing = new User("ext-2", "test@test.com", "Other User");
        setEntityId(existing, UUID.randomUUID());
        User newUser = new User("ext-1", "test@test.com", "New User");

        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(newUser));
    }

    @Test
    void createUser_ShouldThrowWhenExternalIdBlank() {
        User user = new User("", "test@test.com", "Test User");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(user));
    }

    @Test
    void createUser_ShouldThrowWhenEmailBlank() {
        User user = new User("ext-1", "", "Test User");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(user));
    }

    @Test
    void createUser_ShouldThrowWhenDisplayNameBlank() {
        User user = new User("ext-1", "test@test.com", "");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(user));
    }

    // ========== loginToUserAccount tests ==========

    @Test
    void loginToUserAccount_ShouldReturnUser() {
        User user = new User("ext-1", "test@test.com", "Test User");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = userService.loginToUserAccount("test@test.com");

        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void loginToUserAccount_ShouldThrowWhenEmailBlank() {
        assertThrows(IllegalArgumentException.class, () -> userService.loginToUserAccount(""));
    }

    @Test
    void loginToUserAccount_ShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.loginToUserAccount("unknown@test.com"));
    }

    // ========== getUser tests ==========

    @Test
    void getUser_ShouldReturnUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUser(userId);

        assertEquals(userId, result.getId());
    }

    @Test
    void getUser_ShouldThrowWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> userService.getUser(null));
    }

    @Test
    void getUser_ShouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(userId));
    }

    // ========== updateUser tests ==========

    @Test
    void updateUser_ShouldUpdateAndReturnUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, userId);

        when(userRepository.findByExternalId("ext-1")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateUser(user);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_ShouldThrowWhenUserIsNull() {
        assertThrows(NullPointerException.class, () -> userService.updateUser(null));
    }

    @Test
    void updateUser_ShouldThrowWhenIdIsNull() {
        User user = new User("ext-1", "test@test.com", "Test User");
        // id is null by default
        assertThrows(NullPointerException.class, () -> userService.updateUser(user));
    }

    // ========== deleteUser tests ==========

    @Test
    void deleteUser_ShouldDeleteUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository).delete(user);
    }

    // ========== linkGoogleAccount tests ==========

    @Test
    void linkGoogleAccount_ShouldLinkAccount() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, userId);
        
        GoogleAccountLink link = new GoogleAccountLink("google-sub-123", "google@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByGoogleAccountSubject("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.linkGoogleAccount(userId, link);

        assertEquals("google-sub-123", result.getGoogleAccountSubject());
        assertEquals("google@gmail.com", result.getGoogleAccountEmail());
    }

    @Test
    void linkGoogleAccount_ShouldReturnWithoutChangeIfAlreadyLinked() {
        UUID userId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, userId);
        user.setGoogleAccountSubject("google-sub-123");
        
        GoogleAccountLink link = new GoogleAccountLink("google-sub-123", "google@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.linkGoogleAccount(userId, link);

        assertEquals("google-sub-123", result.getGoogleAccountSubject());
        verify(userRepository, never()).save(any());
    }

    @Test
    void linkGoogleAccount_ShouldThrowWhenAccountAlreadyLinkedToOtherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User user = new User("ext-1", "test@test.com", "Test User");
        setEntityId(user, userId);
        User otherUser = new User("ext-2", "other@test.com", "Other User");
        setEntityId(otherUser, otherUserId);
        otherUser.setGoogleAccountSubject("google-sub-123");
        
        GoogleAccountLink link = new GoogleAccountLink("google-sub-123", "google@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByGoogleAccountSubject("google-sub-123")).thenReturn(Optional.of(otherUser));

        assertThrows(GoogleAccountAlreadyLinkedException.class, 
            () -> userService.linkGoogleAccount(userId, link));
    }

    @Test
    void linkGoogleAccount_ShouldThrowWhenGoogleAccountLinkIsNull() {
        UUID userId = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> userService.linkGoogleAccount(userId, null));
    }

    @Test
    void linkGoogleAccount_ShouldThrowWhenSubjectBlank() {
        UUID userId = UUID.randomUUID();
        GoogleAccountLink link = new GoogleAccountLink("", "google@gmail.com");

        assertThrows(IllegalArgumentException.class, 
            () -> userService.linkGoogleAccount(userId, link));
    }
}

