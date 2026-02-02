package org.marly.mavigo.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    @FindBy(css = "[data-testid='login-button'], .login-button, a[href*='login']")
    private WebElement loginButton;

    @FindBy(css = "[data-testid='logout-button'], .logout-button, button[type='submit']")
    private WebElement logoutButton;

    @FindBy(css = "[data-testid='user-email'], .user-email")
    private WebElement userEmailDisplay;

    public LoginPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(baseUrl);
    }

    public void navigateToLogin() {
        driver.get(baseUrl + "/auth/login");
    }

    public boolean isLoginButtonDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(loginButton)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickLoginButton() {
        wait.until(ExpectedConditions.elementToBeClickable(loginButton)).click();
    }

    public boolean isLoggedIn() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(userEmailDisplay)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserEmail() {
        return wait.until(ExpectedConditions.visibilityOf(userEmailDisplay)).getText();
    }

    public void clickLogout() {
        wait.until(ExpectedConditions.elementToBeClickable(logoutButton)).click();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("/login") ||
                driver.getCurrentUrl().contains("/oauth2");
    }
}
