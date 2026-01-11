package org.marly.mavigo.controller.dto;

import org.marly.mavigo.models.user.ComfortProfile;

public record ComfortProfileResponse(
        String directPath,
        Boolean requireAirConditioning,
        Integer maxNbTransfers,
        Integer maxWaitingDuration,
        Integer maxWalkingDuration) {

    public static ComfortProfileResponse from(ComfortProfile profile) {
        if (profile == null) {
            return new ComfortProfileResponse(null, null, null, null, null);
        }
        return new ComfortProfileResponse(
                profile.getDirectPath(),
                profile.getRequireAirConditioning(),
                profile.getMaxNbTransfers(),
                profile.getMaxWaitingDuration(),
                profile.getMaxWalkingDuration());
    }
}
