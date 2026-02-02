package org.marly.mavigo.ui.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.marly.mavigo.ui.BaseSeleniumTest;
import org.marly.mavigo.ui.pages.LoginPage;

@DisplayName("Tests UI - Flux de connexion")
@EnabledIfEnvironmentVariable(named = "RUN_UI_TESTS", matches = "true")
class LoginFlowTest extends BaseSeleniumTest {

    @Test
    @DisplayName("La page de login devrait être accessible")
    void loginPage_shouldBeAccessible() {
        // Given
        LoginPage loginPage = new LoginPage(driver, baseUrl);

        // When
        loginPage.navigateToLogin();

        // Then
        // The page should redirect to OAuth2 or show a login option
        assertTrue(loginPage.isOnLoginPage() || loginPage.getCurrentUrl().contains("oauth2"),
                "Should be on login page or redirected to OAuth");
    }

    @Test
    @DisplayName("L'endpoint /auth/status devrait être accessible")
    void authStatus_shouldBeAccessible() {
        // When
        navigateTo("/auth/status");

        // Then
        waitForPageLoad();
        // API endpoint should return JSON (not a 404)
        assertFalse(driver.getPageSource().contains("404"),
                "Auth status endpoint should be accessible");
    }

    @Test
    @DisplayName("Redirection vers OAuth2 depuis /auth/login")
    void authLogin_shouldRedirectToOAuth2() {
        // Given
        LoginPage loginPage = new LoginPage(driver, baseUrl);

        // When
        loginPage.navigateToLogin();
        waitForPageLoad();

        // Then
        String currentUrl = loginPage.getCurrentUrl();
        assertTrue(
                currentUrl.contains("oauth2") ||
                        currentUrl.contains("google") ||
                        currentUrl.contains("accounts.google"),
                "Should redirect to OAuth2 provider. Current URL: " + currentUrl);
    }
}
