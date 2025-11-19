package org.marly.mavigo.service.user;

import java.util.UUID;

import org.marly.mavigo.models.user.User;

public interface UserService {

    User createUser(User user);

    User getUser(UUID userId);

    User updateUser(User user);

    void deleteUser(UUID userId);

}
