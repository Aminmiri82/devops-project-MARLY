package org.marly.mavigo.service.user;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.NamedComfortSettingRepository;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final NamedComfortSettingRepository namedComfortSettingRepository;

    public UserServiceImpl(UserRepository userRepository, NamedComfortSettingRepository namedComfortSettingRepository) {
        this.userRepository = userRepository;
        this.namedComfortSettingRepository = namedComfortSettingRepository;
    }

    @Override
    public User createUser(User user) {
        Objects.requireNonNull(user, "user must not be null");
        validateRequiredFields(user);
        ensureExternalIdAvailable(user.getExternalId(), null);
        ensureEmailAvailable(user.getEmail(), null);

        User saved = userRepository.save(user);

        return saved;
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
        User user = userRepository.findWithSettingsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        org.hibernate.Hibernate.initialize(user.getNamedComfortSettings());
        return user;
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

        // If this user already has this Google account linked, just return (idempotent)
        if (subject.equals(user.getGoogleAccountSubject())) {
            return user;
        }

        ensureGoogleSubjectAvailable(subject, userId);

        user.setGoogleAccountSubject(subject);
        user.setGoogleAccountEmail(googleAccountLink.email());
        user.setGoogleLinkedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public User addNamedComfortSetting(UUID userId, String name, ComfortProfile profile) {
        User user = getUser(userId);
        NamedComfortSetting setting = new NamedComfortSetting(name, profile, user);
        user.addNamedComfortSetting(setting);
        User saved = userRepository.save(user);
        org.hibernate.Hibernate.initialize(saved.getNamedComfortSettings());
        return saved;
    }

    @Override
    public User updateNamedComfortSetting(UUID userId, UUID settingId, String name, ComfortProfile profile) {
        User user = getUser(userId);
        NamedComfortSetting setting = namedComfortSettingRepository.findById(settingId)
                .orElseThrow(() -> new IllegalArgumentException("Named comfort setting not found: " + settingId));

        if (!setting.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Setting does not belong to user");
        }

        setting.setName(name);
        setting.setComfortProfile(profile);
        User saved = userRepository.save(user);
        org.hibernate.Hibernate.initialize(saved.getNamedComfortSettings());
        return saved;
    }

    @Override
    public User markComfortPromptAsSeen(UUID userId) {
        User user = getUser(userId);
        user.setHasSeenComfortPrompt(true);
        return userRepository.save(user);
    }

    @Override
    public User deleteNamedComfortSetting(UUID userId, UUID settingId) {
        User user = getUser(userId);
        NamedComfortSetting setting = namedComfortSettingRepository.findById(settingId)
                .orElseThrow(() -> new IllegalArgumentException("Named comfort setting not found: " + settingId));

        if (!setting.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Setting does not belong to user");
        }

        user.removeNamedComfortSetting(setting);
        namedComfortSettingRepository.delete(setting);
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
