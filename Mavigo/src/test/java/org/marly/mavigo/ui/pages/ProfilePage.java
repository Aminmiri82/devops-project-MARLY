package org.marly.mavigo.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ProfilePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    @FindBy(css = "[data-testid='user-name'], .user-name")
    private WebElement userName;

    @FindBy(css = "[data-testid='user-email'], .user-email")
    private WebElement userEmail;

    @FindBy(css = "[data-testid='comfort-settings'], .comfort-settings")
    private WebElement comfortSettingsSection;

    @FindBy(css = "[data-testid='wheelchair-toggle'], input[name='wheelchair']")
    private WebElement wheelchairToggle;

    @FindBy(css = "[data-testid='max-transfers'], select[name='maxTransfers'], input[name='maxTransfers']")
    private WebElement maxTransfersInput;

    @FindBy(css = "[data-testid='save-button'], button[type='submit'], .save-button")
    private WebElement saveButton;

    @FindBy(css = "[data-testid='success-message'], .success-message")
    private WebElement successMessage;

    @FindBy(css = "[data-testid='google-link-button'], .google-link")
    private WebElement googleLinkButton;

    public ProfilePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(baseUrl + "/profile");
    }

    public String getUserName() {
        return wait.until(ExpectedConditions.visibilityOf(userName)).getText();
    }

    public String getUserEmail() {
        return wait.until(ExpectedConditions.visibilityOf(userEmail)).getText();
    }

    public boolean isComfortSettingsDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(comfortSettingsSection)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void enableWheelchairAccess() {
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(wheelchairToggle));
        if (!toggle.isSelected()) {
            toggle.click();
        }
    }

    public void disableWheelchairAccess() {
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(wheelchairToggle));
        if (toggle.isSelected()) {
            toggle.click();
        }
    }

    public boolean isWheelchairAccessEnabled() {
        return wait.until(ExpectedConditions.visibilityOf(wheelchairToggle)).isSelected();
    }

    public void setMaxTransfers(int maxTransfers) {
        WebElement input = wait.until(ExpectedConditions.visibilityOf(maxTransfersInput));
        input.clear();
        input.sendKeys(String.valueOf(maxTransfers));
    }

    public String getMaxTransfers() {
        return wait.until(ExpectedConditions.visibilityOf(maxTransfersInput)).getAttribute("value");
    }

    public void clickSave() {
        wait.until(ExpectedConditions.elementToBeClickable(saveButton)).click();
    }

    public boolean isSuccessMessageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(successMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickLinkGoogleAccount() {
        wait.until(ExpectedConditions.elementToBeClickable(googleLinkButton)).click();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
