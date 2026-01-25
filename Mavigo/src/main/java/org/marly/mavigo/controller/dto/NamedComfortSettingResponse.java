package org.marly.mavigo.controller.dto;

import java.util.UUID;
import org.marly.mavigo.models.user.NamedComfortSetting;

public record NamedComfortSettingResponse(
        UUID id,
        String name,
        ComfortProfileResponse comfortProfile) {

    public static NamedComfortSettingResponse from(NamedComfortSetting setting) {
        return new NamedComfortSettingResponse(
                setting.getId(),
                setting.getName(),
                ComfortProfileResponse.from(setting.getComfortProfile()));
    }
}
