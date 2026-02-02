package org.marly.mavigo.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    @FindBy(css = "[data-testid='journey-search'], .journey-search, #journey-search")
    private WebElement journeySearchSection;

    @FindBy(css = "[data-testid='recent-journeys'], .recent-journeys")
    private WebElement recentJourneysSection;

    @FindBy(css = "[data-testid='alerts-panel'], .alerts-panel")
    private WebElement alertsPanel;

    @FindBy(css = "[data-testid='user-profile-link'], .user-profile, a[href*='profile']")
    private WebElement profileLink;

    @FindBy(css = "[data-testid='tasks-panel'], .tasks-panel")
    private WebElement tasksPanel;

    public HomePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(baseUrl);
    }

    public boolean isJourneySearchDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(journeySearchSection)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRecentJourneysDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(recentJourneysSection)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAlertsPanelDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(alertsPanel)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickProfileLink() {
        wait.until(ExpectedConditions.elementToBeClickable(profileLink)).click();
    }

    public boolean isTasksPanelDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(tasksPanel)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public boolean isLoaded() {
        return driver.getCurrentUrl().equals(baseUrl) ||
                driver.getCurrentUrl().equals(baseUrl + "/");
    }
}
