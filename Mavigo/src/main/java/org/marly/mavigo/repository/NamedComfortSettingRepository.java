package org.marly.mavigo.repository;

import java.util.UUID;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NamedComfortSettingRepository extends JpaRepository<NamedComfortSetting, UUID> {
}
