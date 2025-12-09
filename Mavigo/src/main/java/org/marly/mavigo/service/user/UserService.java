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
}
