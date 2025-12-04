package org.marly.mavigo.service.user;

import java.util.Objects;
import java.util.UUID;

import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user) {
        Objects.requireNonNull(user, "user must not be null");
        validateRequiredFields(user);
        ensureExternalIdAvailable(user.getExternalId(), null);
        ensureEmailAvailable(user.getEmail(), null);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public User updateUser(User user) {
        Objects.requireNonNull(user, "user must not be null");
        UUID userId = Objects.requireNonNull(user.getId(), "user.id must not be null");
        validateRequiredFields(user);
        ensureExternalIdAvailable(user.getExternalId(), userId);
        ensureEmailAvailable(user.getEmail(), userId);
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID userId) {
        User existing = getUser(userId);
        userRepository.delete(existing);
    }

    private void ensureEmailAvailable(String email, UUID ignoreUserId) {
        userRepository.findByEmail(email)
                .filter(existing -> isDifferentUser(existing.getId(), ignoreUserId))
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("Email already registered: " + email);
                });
    }

    private void ensureExternalIdAvailable(String externalId, UUID ignoreUserId) {
        userRepository.findByExternalId(externalId)
                .filter(existing -> isDifferentUser(existing.getId(), ignoreUserId))
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("External ID already registered: " + externalId);
                });
    }

    private boolean isDifferentUser(UUID existingId, UUID ignoreUserId) {
        return ignoreUserId == null || !ignoreUserId.equals(existingId);
    }

    private void validateRequiredFields(User user) {
        requireText(user.getExternalId(), "externalId");
        requireText(user.getEmail(), "email");
        requireText(user.getDisplayName(), "displayName");
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("User " + fieldName + " is required");
        }
    }
}

