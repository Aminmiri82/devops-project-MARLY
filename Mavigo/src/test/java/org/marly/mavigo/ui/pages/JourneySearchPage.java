package org.marly.mavigo.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class JourneySearchPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    @FindBy(css = "[data-testid='origin-input'], #origin, input[name='origin']")
    private WebElement originInput;

    @FindBy(css = "[data-testid='destination-input'], #destination, input[name='destination']")
    private WebElement destinationInput;

    @FindBy(css = "[data-testid='departure-time'], #departureTime, input[name='departureTime']")
    private WebElement departureTimeInput;

    @FindBy(css = "[data-testid='search-button'], button[type='submit'], .search-button")
    private WebElement searchButton;

    @FindBy(css = "[data-testid='comfort-mode-toggle'], .comfort-mode-toggle, input[name='comfortMode']")
    private WebElement comfortModeToggle;

    @FindBy(css = "[data-testid='swap-button'], .swap-button")
    private WebElement swapButton;

    public JourneySearchPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(baseUrl + "/search");
    }

    public void enterOrigin(String origin) {
        wait.until(ExpectedConditions.visibilityOf(originInput)).clear();
        originInput.sendKeys(origin);
    }

    public void enterDestination(String destination) {
        wait.until(ExpectedConditions.visibilityOf(destinationInput)).clear();
        destinationInput.sendKeys(destination);
    }

    public void selectDepartureTime(String time) {
        wait.until(ExpectedConditions.visibilityOf(departureTimeInput)).clear();
        departureTimeInput.sendKeys(time);
    }

    public void clickSearch() {
        wait.until(ExpectedConditions.elementToBeClickable(searchButton)).click();
    }

    public void searchJourney(String origin, String destination) {
        enterOrigin(origin);
        enterDestination(destination);
        clickSearch();
    }

    public void enableComfortMode() {
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(comfortModeToggle));
        if (!toggle.isSelected()) {
            toggle.click();
        }
    }

    public void disableComfortMode() {
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(comfortModeToggle));
        if (toggle.isSelected()) {
            toggle.click();
        }
    }

    public void swapOriginAndDestination() {
        wait.until(ExpectedConditions.elementToBeClickable(swapButton)).click();
    }

    public String getOriginValue() {
        return wait.until(ExpectedConditions.visibilityOf(originInput)).getAttribute("value");
    }

    public String getDestinationValue() {
        return wait.until(ExpectedConditions.visibilityOf(destinationInput)).getAttribute("value");
    }

    public boolean isSearchButtonEnabled() {
        return wait.until(ExpectedConditions.visibilityOf(searchButton)).isEnabled();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
