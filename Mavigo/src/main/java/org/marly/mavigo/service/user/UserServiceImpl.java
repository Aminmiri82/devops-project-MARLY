package org.marly.mavigo.service.user;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
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
    public User loginToUserAccount(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required for login");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No user found with email: " + email));
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

    @Override
    public User linkGoogleAccount(UUID userId, GoogleAccountLink googleAccountLink) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(googleAccountLink, "googleAccountLink must not be null");

        String subject = googleAccountLink.subject();
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Google account subject is required");
        }

        User user = getUser(userId);
        ensureGoogleSubjectAvailable(subject, userId);

        user.setGoogleAccountSubject(subject);
        user.setGoogleAccountEmail(googleAccountLink.email());
        user.setGoogleLinkedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    private void ensureGoogleSubjectAvailable(String subject, UUID ignoreUserId) {
        userRepository.findByGoogleAccountSubject(subject)
                .filter(existing -> isDifferentUser(existing.getId(), ignoreUserId))
                .ifPresent(existing -> {
                    throw new GoogleAccountAlreadyLinkedException("Google account already linked to another user");
                });
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

