package org.marly.mavigo.service.user;

import java.util.UUID;

import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;

public interface UserService {

    User createUser(User user);

    User loginToUserAccount(String email);

    User getUser(UUID userId);

    User updateUser(User user);

    void deleteUser(UUID userId);

    User linkGoogleAccount(UUID userId, GoogleAccountLink googleAccountLink);

    User addNamedComfortSetting(UUID userId, String name, org.marly.mavigo.models.user.ComfortProfile profile);

    User updateNamedComfortSetting(UUID userId, UUID settingId, String name,
            org.marly.mavigo.models.user.ComfortProfile profile);

    User markComfortPromptAsSeen(UUID userId);

    User deleteNamedComfortSetting(UUID userId, UUID settingId);
}
