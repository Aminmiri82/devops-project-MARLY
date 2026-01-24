package org.marly.mavigo.service.accessibility;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class AccessibilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessibilityService.class);

    public boolean isStationWheelchairAccessible(String stationId) {
        try {
            LOGGER.debug("Vérification accessibilité station: {}", stationId);

            return true;

        } catch (Exception e) {
            LOGGER.error("Erreur lors de la vérification d'accessibilité de la station {}", stationId, e);
            return false;
        }
    }

    public boolean isLineWheelchairAccessible(String lineCode) {
        try {
            LOGGER.debug("Vérification accessibilité ligne: {}", lineCode);

            return true;

        } catch (Exception e) {
            LOGGER.error("Erreur lors de la vérification d'accessibilité de la ligne {}", lineCode, e);
            return false;
        }
    }
}