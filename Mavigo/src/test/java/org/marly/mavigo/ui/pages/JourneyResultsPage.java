package org.marly.mavigo.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class JourneyResultsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    @FindBy(css = "[data-testid='journey-results'], .journey-results")
    private WebElement resultsContainer;

    @FindBy(css = "[data-testid='journey-option'], .journey-option")
    private List<WebElement> journeyOptions;

    @FindBy(css = "[data-testid='loading-spinner'], .loading")
    private WebElement loadingSpinner;

    @FindBy(css = "[data-testid='no-results'], .no-results")
    private WebElement noResultsMessage;

    @FindBy(css = "[data-testid='error-message'], .error-message")
    private WebElement errorMessage;

    @FindBy(css = "[data-testid='back-button'], .back-button")
    private WebElement backButton;

    public JourneyResultsPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    public void waitForResults() {
        wait.until(ExpectedConditions.invisibilityOf(loadingSpinner));
        wait.until(ExpectedConditions.visibilityOf(resultsContainer));
    }

    public boolean hasResults() {
        try {
            wait.until(ExpectedConditions.visibilityOf(resultsContainer));
            return !journeyOptions.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public int getResultCount() {
        try {
            wait.until(ExpectedConditions.visibilityOf(resultsContainer));
            return journeyOptions.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public void selectJourneyOption(int index) {
        wait.until(ExpectedConditions.visibilityOfAllElements(journeyOptions));
        if (index < journeyOptions.size()) {
            journeyOptions.get(index).click();
        } else {
            throw new IndexOutOfBoundsException("Journey option index " + index + " out of bounds");
        }
    }

    public WebElement getJourneyOption(int index) {
        wait.until(ExpectedConditions.visibilityOfAllElements(journeyOptions));
        return journeyOptions.get(index);
    }

    public String getJourneyDuration(int index) {
        WebElement option = getJourneyOption(index);
        WebElement duration = option.findElement(By.cssSelector("[data-testid='duration'], .duration"));
        return duration.getText();
    }

    public String getJourneyDepartureTime(int index) {
        WebElement option = getJourneyOption(index);
        WebElement departure = option.findElement(By.cssSelector("[data-testid='departure-time'], .departure-time"));
        return departure.getText();
    }

    public boolean isNoResultsDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(noResultsMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isErrorDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(errorMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        return wait.until(ExpectedConditions.visibilityOf(errorMessage)).getText();
    }

    public void clickBack() {
        wait.until(ExpectedConditions.elementToBeClickable(backButton)).click();
    }

    public boolean isLoading() {
        try {
            return loadingSpinner.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
