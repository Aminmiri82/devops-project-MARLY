package org.marly.mavigo.service.accessibility;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccessibilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessibilityService.class);


    public boolean isStationWheelchairAccessible(String stationId) {
        // 👇 VALIDATION AJOUTÉE
        if (stationId == null || stationId.trim().isEmpty()) {
            LOGGER.warn("Station ID is null or empty");
            return false;
        }

        try {
            LOGGER.debug("Vérification accessibilité station: {}", stationId);

            // TODO: Implémenter l'appel API IDFM réel
            // Pour l'instant, retourne true par défaut pour les IDs valides
            return true;

        } catch (Exception e) {
            LOGGER.error("Erreur lors de la vérification d'accessibilité de la station {}", stationId, e);
            return false;
        }
    }

    public boolean isLineWheelchairAccessible(String lineCode) {
        // 👇 VALIDATION AJOUTÉE
        if (lineCode == null || lineCode.trim().isEmpty()) {
            LOGGER.warn("Line code is null or empty");
            return false;
        }

        try {
            LOGGER.debug("Vérification accessibilité ligne: {}", lineCode);

            // TODO: Implémenter l'appel API IDFM réel
            // Pour l'instant, retourne true par défaut pour les codes valides
            return true;

        } catch (Exception e) {
            LOGGER.error("Erreur lors de la vérification d'accessibilité de la ligne {}", lineCode, e);
            return false;
        }
    }
}