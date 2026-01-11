package org.marly.mavigo.controller;

import java.util.UUID;

import org.marly.mavigo.controller.dto.ComfortProfileRequest;
import org.marly.mavigo.controller.dto.ComfortProfileResponse;
import org.marly.mavigo.controller.dto.CreateUserRequest;
import org.marly.mavigo.controller.dto.UpdateUserRequest;
import org.marly.mavigo.controller.dto.UserResponse;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.createUser(request.toUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request) {
        User user = userService.loginToUserAccount(request.email());
        return UserResponse.from(user);
    }

    public record LoginRequest(String email) {}

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable UUID userId) {
        return UserResponse.from(userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        User existing = userService.getUser(userId);
        request.apply(existing);
        User updated = userService.updateUser(existing);
        return UserResponse.from(updated);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
    }

    @GetMapping("/{userId}/comfort-profile")
    public ComfortProfileResponse getComfortProfile(@PathVariable UUID userId) {
        User user = userService.getUser(userId);
        return ComfortProfileResponse.from(user.getComfortProfile());
    }

    @PutMapping("/{userId}/comfort-profile")
    public ComfortProfileResponse updateComfortProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody ComfortProfileRequest request) {
        User user = userService.getUser(userId);

        ComfortProfile profile = user.getComfortProfile();
        if (profile == null) {
            profile = new ComfortProfile();
            user.setComfortProfile(profile);
        }

        profile.setDirectPath(request.directPath());
        profile.setRequireAirConditioning(request.requireAirConditioning());
        profile.setMaxNbTransfers(request.maxNbTransfers());
        profile.setMaxWaitingDuration(request.maxWaitingDuration());
        profile.setMaxWalkingDuration(request.maxWalkingDuration());

        User updated = userService.updateUser(user);
        return ComfortProfileResponse.from(updated.getComfortProfile());
    }

    @DeleteMapping("/{userId}/comfort-profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComfortProfile(@PathVariable UUID userId) {
        User user = userService.getUser(userId);
        user.setComfortProfile(new ComfortProfile());
        userService.updateUser(user);
    }
}

