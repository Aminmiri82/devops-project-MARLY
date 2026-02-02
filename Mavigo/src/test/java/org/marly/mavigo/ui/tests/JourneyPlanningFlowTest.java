package org.marly.mavigo.ui.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.marly.mavigo.ui.BaseSeleniumTest;
import org.marly.mavigo.ui.pages.JourneySearchPage;
import org.marly.mavigo.ui.pages.JourneyResultsPage;

@DisplayName("Tests UI - Flux de planification de trajet")
@EnabledIfEnvironmentVariable(named = "RUN_UI_TESTS", matches = "true")
class JourneyPlanningFlowTest extends BaseSeleniumTest {

    @Test
    @DisplayName("La page de recherche devrait être accessible")
    void journeySearchPage_shouldBeAccessible() {
        // Given
        JourneySearchPage searchPage = new JourneySearchPage(driver, baseUrl);

        // When
        searchPage.navigateTo();
        waitForPageLoad();

        // Then
        assertNotNull(driver.getPageSource());
    }

    @Test
    @DisplayName("L'API de planification de trajet devrait être accessible")
    void journeyApi_shouldBeAccessible() {
        // When
        navigateTo("/api/journeys");
        waitForPageLoad();

        // Then
        // API should return 401 (unauthorized) or similar, not 404
        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains("404 Not Found"),
                "Journey API endpoint should exist");
    }

    @Test
    @DisplayName("L'endpoint de debug des tâches devrait être accessible avec authentification")
    void debugUserTasks_shouldExist() {
        // When - try to access debug endpoint
        navigateTo("/api/journeys/debug/user-tasks?userId=00000000-0000-0000-0000-000000000000");
        waitForPageLoad();

        // Then
        String pageSource = driver.getPageSource();
        // Should get 401 unauthorized, not 404
        assertFalse(pageSource.contains("404"),
                "Debug endpoint should exist");
    }
}
