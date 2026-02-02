package org.marly.mavigo.ui.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.marly.mavigo.ui.BaseSeleniumTest;
import org.marly.mavigo.ui.pages.ProfilePage;

@DisplayName("Tests UI - Flux de profil de confort")
@EnabledIfEnvironmentVariable(named = "RUN_UI_TESTS", matches = "true")
class ComfortProfileFlowTest extends BaseSeleniumTest {

    @Test
    @DisplayName("La page de profil devrait rediriger vers login si non authentifié")
    void profilePage_shouldRequireAuthentication() {
        // Given
        ProfilePage profilePage = new ProfilePage(driver, baseUrl);

        // When
        profilePage.navigateTo();
        waitForPageLoad();

        // Then
        String currentUrl = profilePage.getCurrentUrl();
        assertTrue(
                currentUrl.contains("login") ||
                        currentUrl.contains("oauth2") ||
                        currentUrl.contains("unauthorized"),
                "Profile page should require authentication. Current URL: " + currentUrl);
    }

    @Test
    @DisplayName("L'API utilisateur devrait être accessible")
    void userApi_shouldBeAccessible() {
        // When
        navigateTo("/api/users");
        waitForPageLoad();

        // Then
        String pageSource = driver.getPageSource();
        // Should exist (even if returns 401)
        assertFalse(pageSource.contains("404 Not Found"),
                "User API endpoint should exist");
    }
}
