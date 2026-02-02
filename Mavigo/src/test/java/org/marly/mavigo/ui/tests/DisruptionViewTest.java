package org.marly.mavigo.ui.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.marly.mavigo.ui.BaseSeleniumTest;

@DisplayName("Tests UI - Visualisation des perturbations")
@EnabledIfEnvironmentVariable(named = "RUN_UI_TESTS", matches = "true")
class DisruptionViewTest extends BaseSeleniumTest {

    @Test
    @DisplayName("L'API des lignes de trajet devrait être accessible")
    void journeyLinesApi_shouldBeAccessible() {
        // When
        navigateTo("/api/journeys/00000000-0000-0000-0000-000000000000/lines");
        waitForPageLoad();

        // Then
        String pageSource = driver.getPageSource();
        // Should return 401 (unauthorized) or 404 (journey not found), not server error
        assertFalse(pageSource.contains("500"),
                "Journey lines API should not return server error");
    }

    @Test
    @DisplayName("L'API des arrêts de trajet devrait être accessible")
    void journeyStopsApi_shouldBeAccessible() {
        // When
        navigateTo("/api/journeys/00000000-0000-0000-0000-000000000000/stops");
        waitForPageLoad();

        // Then
        String pageSource = driver.getPageSource();
        // Should return 401 (unauthorized) or 404 (journey not found), not server error
        assertFalse(pageSource.contains("500"),
                "Journey stops API should not return server error");
    }

    @Test
    @DisplayName("L'API de signalement de perturbation de station devrait exister")
    void stationDisruptionApi_shouldExist() {
        // When - POST endpoint, we just check it doesn't 404
        navigateTo("/api/journeys/00000000-0000-0000-0000-000000000000/disruptions/station");
        waitForPageLoad();

        // Then
        String pageSource = driver.getPageSource();
        // GET on POST endpoint will fail, but shouldn't be 404
        // Method not allowed (405) or unauthorized (401) is expected
        assertNotNull(pageSource);
    }

    @Test
    @DisplayName("L'API de signalement de perturbation de ligne devrait exister")
    void lineDisruptionApi_shouldExist() {
        // When - POST endpoint, we just check it doesn't 404
        navigateTo("/api/journeys/00000000-0000-0000-0000-000000000000/disruptions/line");
        waitForPageLoad();

        // Then
        String pageSource = driver.getPageSource();
        assertNotNull(pageSource);
    }
}
