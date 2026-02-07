package org.marly.mavigo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.controller.dto.*;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.user.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User sampleUser;
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sampleUser = new User("ext-123", "test@example.com", "Test User");
        setEntityId(sampleUser, USER_ID);
        sampleUser.setComfortProfile(new ComfortProfile());
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

    @Test
    void createUser_ShouldReturnCreatedUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest("ext-123", "test@example.com", "Test User", null, null);
        when(userService.createUser(any(User.class))).thenReturn(sampleUser);

        // When
        ResponseEntity<UserResponse> response = userController.createUser(request);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(USER_ID, response.getBody().userId());
        assertEquals("test@example.com", response.getBody().email());
    }

    @Test
    void getUser_ShouldReturnUser() {
        // Given
        when(userService.getUser(USER_ID)).thenReturn(sampleUser);

        // When
        UserResponse response = userController.getUser(USER_ID);

        // Then
        assertNotNull(response);
        assertEquals(USER_ID, response.userId());
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest("ext-123", "test@example.com", "Updated Name", null, null);
        when(userService.getUser(USER_ID)).thenReturn(sampleUser);
        // Using any() because the service updates the user object in place usually
        when(userService.updateUser(any(User.class))).thenReturn(sampleUser);

        // When
        UserResponse response = userController.updateUser(USER_ID, request);

        // Then
        assertNotNull(response);
        verify(userService).updateUser(any(User.class));
    }

    @Test
    void deleteUser_ShouldCallService() {
        // When
        userController.deleteUser(USER_ID);

        // Then
        verify(userService).deleteUser(USER_ID);
    }

    @Test
    void getComfortProfile_ShouldReturnProfile() {
        // Given
        when(userService.getUser(USER_ID)).thenReturn(sampleUser);

        // When
        ComfortProfileResponse response = userController.getComfortProfile(USER_ID);

        // Then
        assertNotNull(response);
    }

    @Test
    void updateComfortProfile_ShouldUpdateAndReturnProfile() {
        // Given
        // "none" for directPath (String), false for requireAirConditioning, ...
        ComfortProfileRequest request = new ComfortProfileRequest("none", false, 2, 10, 15);
        when(userService.getUser(USER_ID)).thenReturn(sampleUser);
        when(userService.updateUser(any(User.class))).thenReturn(sampleUser);

        // When
        ComfortProfileResponse response = userController.updateComfortProfile(USER_ID, request);

        // Then
        assertNotNull(response);
        verify(userService).updateUser(any(User.class));
    }

    @Test
    void deleteComfortProfile_ShouldResetProfile() {
        // Given
        when(userService.getUser(USER_ID)).thenReturn(sampleUser);

        // When
        userController.deleteComfortProfile(USER_ID);

        // Then
        verify(userService).updateUser(any(User.class));
    }
}
