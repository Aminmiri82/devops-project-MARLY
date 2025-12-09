package org.marly.mavigo.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.user.GoogleAccountAlreadyLinkedException;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
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

    @Test
    void linkGoogleAccountStoresSubjectAndEmail() {
        User saved = userService.createUser(new User("ext-link", "link@example.com", "Linkable"));

        User linked = userService.linkGoogleAccount(saved.getId(), new GoogleAccountLink("sub-123", "google@example.com"));

        assertThat(linked.getGoogleAccountSubject()).isEqualTo("sub-123");
        assertThat(linked.getGoogleAccountEmail()).isEqualTo("google@example.com");
        assertThat(linked.getGoogleLinkedAt()).isNotNull();
    }

    @Test
    void linkGoogleAccountRejectsDuplicateSubject() {
        User first = userService.createUser(new User("ext-a", "a@example.com", "First"));
        User second = userService.createUser(new User("ext-b", "b@example.com", "Second"));

        userService.linkGoogleAccount(first.getId(), new GoogleAccountLink("sub-dup", "google+a@example.com"));

        assertThrows(GoogleAccountAlreadyLinkedException.class,
                () -> userService.linkGoogleAccount(second.getId(), new GoogleAccountLink("sub-dup", "google+b@example.com")));
    }
}

