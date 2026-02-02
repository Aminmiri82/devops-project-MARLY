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
        // The page should redirect to OAuth2 provider (Google)
        String currentUrl = loginPage.getCurrentUrl();
        assertTrue(
                loginPage.isOnLoginPage() ||
                        currentUrl.contains("oauth2") ||
                        currentUrl.contains("google") ||
                        currentUrl.contains("accounts.google.com"),
                "Should be on login page or redirected to OAuth. Current URL: " + currentUrl);
    }

    @Test
    @DisplayName("L'endpoint /auth/status devrait rediriger vers OAuth quand non authentifié")
    void authStatus_shouldRedirectToOAuthWhenUnauthenticated() {
        // When
        navigateTo("/auth/status");
        waitForPageLoad();

        // Then
        // Should redirect to OAuth provider (Google) when not authenticated
        String currentUrl = driver.getCurrentUrl();
        assertTrue(
                currentUrl.contains("oauth2") ||
                        currentUrl.contains("google") ||
                        currentUrl.contains("accounts.google.com"),
                "Auth status should redirect to OAuth when unauthenticated. Current URL: " + currentUrl);
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
