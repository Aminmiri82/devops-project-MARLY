package org.marly.mavigo.ui.pages;

import org.openqa.selenium.By;
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

    private static final By ORIGIN_INPUT = By.cssSelector(
            "[data-testid='origin-input'], #origin, #from, input[name='origin'], input[name='from']");
    private static final By DESTINATION_INPUT = By.cssSelector(
            "[data-testid='destination-input'], #destination, #to, input[name='destination'], input[name='to']");
    private static final By DEPARTURE_TIME_INPUT = By.cssSelector(
            "[data-testid='departure-time'], #departureTime, #departure, input[name='departureTime'], input[name='departure']");
    private static final By SEARCH_BUTTON = By.cssSelector(
            "[data-testid='search-button'], #journeyForm button[type='submit'], #journeyForm .search-button");
    private static final By COMFORT_MODE_TOGGLE = By.cssSelector(
            "[data-testid='comfort-mode-toggle'], .comfort-mode-toggle, input[name='comfortMode']");
    private static final By SWAP_BUTTON = By.cssSelector("[data-testid='swap-button'], .swap-button");

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
        WebElement input = findVisible(ORIGIN_INPUT);
        if (input == null) {
            return;
        }
        input.clear();
        input.sendKeys(origin);
    }

    public void enterDestination(String destination) {
        WebElement input = findVisible(DESTINATION_INPUT);
        if (input == null) {
            return;
        }
        input.clear();
        input.sendKeys(destination);
    }

    public void selectDepartureTime(String time) {
        WebElement input = findVisible(DEPARTURE_TIME_INPUT);
        if (input == null) {
            return;
        }
        input.clear();
        input.sendKeys(time);
    }

    public void clickSearch() {
        WebElement button = findVisible(SEARCH_BUTTON);
        if (button != null && button.isEnabled()) {
            button.click();
        }
    }

    public void searchJourney(String origin, String destination) {
        enterOrigin(origin);
        enterDestination(destination);
        clickSearch();
    }

    public void enableComfortMode() {
        WebElement toggle = findVisible(COMFORT_MODE_TOGGLE);
        if (toggle != null && !toggle.isSelected()) {
            toggle.click();
        }
    }

    public void disableComfortMode() {
        WebElement toggle = findVisible(COMFORT_MODE_TOGGLE);
        if (toggle != null && toggle.isSelected()) {
            toggle.click();
        }
    }

    public void swapOriginAndDestination() {
        WebElement button = findVisible(SWAP_BUTTON);
        if (button != null) {
            button.click();
        }
    }

    public String getOriginValue() {
        WebElement input = findVisible(ORIGIN_INPUT);
        return input == null ? "" : input.getAttribute("value");
    }

    public String getDestinationValue() {
        WebElement input = findVisible(DESTINATION_INPUT);
        return input == null ? "" : input.getAttribute("value");
    }

    public boolean isSearchButtonEnabled() {
        WebElement button = findVisible(SEARCH_BUTTON);
        return button != null && button.isEnabled();
    }

    public boolean hasSearchButton() {
        return !driver.findElements(SEARCH_BUTTON).isEmpty();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    private WebElement findVisible(By selector) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
        } catch (Exception e) {
            return null;
        }
    }
}
