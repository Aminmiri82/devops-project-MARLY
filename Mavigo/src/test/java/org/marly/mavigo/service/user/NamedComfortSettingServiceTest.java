package org.marly.mavigo.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.MavigoApplication;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.NamedComfortSettingRepository;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MavigoApplication.class)
@ActiveProfiles("test")
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
        // User now starts with 1 default "Accessibility" setting
        int initialCount = user.getNamedComfortSettings().size();
        ComfortProfile profile = new ComfortProfile();
        profile.setDirectPath("only");

        User updated = userService.addNamedComfortSetting(user.getId(), "Commute", profile);

        assertThat(updated.getNamedComfortSettings()).hasSize(initialCount + 1);
        NamedComfortSetting saved = updated.getNamedComfortSettings().stream()
                .filter(s -> "Commute".equals(s.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getName()).isEqualTo("Commute");
        assertThat(saved.getComfortProfile().getDirectPath()).isEqualTo("only");
        assertThat(namedComfortSettingRepository.count()).isEqualTo(initialCount + 1);
    }

    @Test
    void deleteNamedComfortSettingRemovesSetting() {
        User user = userService.createUser(new User("ext-del", "del@example.com", "Del Test"));
        // User starts with default "Accessibility" setting
        int initialCount = user.getNamedComfortSettings().size();
        ComfortProfile profile = new ComfortProfile();
        profile.setDirectPath("none");
        User updated = userService.addNamedComfortSetting(user.getId(), "Temporary", profile);
        UUID settingId = updated.getNamedComfortSettings().stream()
                .filter(s -> "Temporary".equals(s.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        User afterDelete = userService.deleteNamedComfortSetting(user.getId(), settingId);

        assertThat(afterDelete.getNamedComfortSettings()).hasSize(initialCount);
    }
}
