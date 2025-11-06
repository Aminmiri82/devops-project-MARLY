package org.marly.mavigo.service.user;

import org.marly.mavigo.models.user.User;
import java.util.UUID;

public interface UserService {

    User createUser(User user);

    User getUser(UUID userId);

    User updateUser(User user);

    void deleteUser(UUID userId);

}
