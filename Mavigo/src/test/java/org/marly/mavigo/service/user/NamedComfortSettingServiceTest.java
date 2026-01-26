package org.marly.mavigo.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.NamedComfortSettingRepository;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserServiceImpl.class)
class NamedComfortSettingServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NamedComfortSettingRepository namedComfortSettingRepository;

    @Test
    void addNamedComfortSettingStoresSetting() {
        User user = userService.createUser(new User("ext-named", "named@example.com", "Named Test"));
        ComfortProfile profile = new ComfortProfile();
        profile.setDirectPath("only");

        User updated = userService.addNamedComfortSetting(user.getId(), "Commute", profile);

        assertThat(updated.getNamedComfortSettings()).hasSize(1);
        NamedComfortSetting saved = updated.getNamedComfortSettings().get(0);
        assertThat(saved.getName()).isEqualTo("Commute");
        assertThat(saved.getComfortProfile().getDirectPath()).isEqualTo("only");
        assertThat(namedComfortSettingRepository.count()).isEqualTo(1);
    }

    @Test
    void deleteNamedComfortSettingRemovesSetting() {
        User user = userService.createUser(new User("ext-del", "del@example.com", "Del Test"));
        ComfortProfile profile = new ComfortProfile();
        profile.setDirectPath("none");
        User updated = userService.addNamedComfortSetting(user.getId(), "Temporary", profile);
        UUID settingId = updated.getNamedComfortSettings().get(0).getId();

        User afterDelete = userService.deleteNamedComfortSetting(user.getId(), settingId);

        assertThat(afterDelete.getNamedComfortSettings()).isEmpty();
        assertThat(namedComfortSettingRepository.count()).isEqualTo(0);
    }
}
