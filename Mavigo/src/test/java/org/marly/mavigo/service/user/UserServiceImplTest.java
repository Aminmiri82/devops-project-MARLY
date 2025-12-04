package org.marly.mavigo.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserServiceImpl.class)
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUserPersistsEntity() {
        User user = new User("ext-create", "create@example.com", "Create Test");

        User saved = userService.createUser(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        userService.createUser(new User("ext-1", "duplicate@example.com", "First"));

        User duplicate = new User("ext-2", "duplicate@example.com", "Second");

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(duplicate));
    }
}

