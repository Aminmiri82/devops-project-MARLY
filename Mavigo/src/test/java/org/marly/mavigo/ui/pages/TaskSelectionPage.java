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

public class TaskSelectionPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    @FindBy(css = "[data-testid='task-list'], .task-list, .task-selection-list, #tasksResults, .tasks-results")
    private WebElement taskList;

    @FindBy(css = "[data-testid='task-item'], .task-item, .task-selection-item, .task-card")
    private List<WebElement> taskItems;

    @FindBy(css = "[data-testid='optimization-toggle'], .optimization-toggle, input[name='enableOptimization']")
    private WebElement optimizationToggle;

    @FindBy(css = "[data-testid='select-all-tasks'], .select-all-tasks")
    private WebElement selectAllButton;

    @FindBy(css = "[data-testid='clear-selection'], .clear-selection")
    private WebElement clearSelectionButton;

    @FindBy(css = "[data-testid='selected-task-count'], .selected-count")
    private WebElement selectedTaskCount;

    @FindBy(css = "[data-testid='optimize-journey-button'], .optimize-journey-button")
    private WebElement optimizeJourneyButton;

    public TaskSelectionPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(baseUrl + "/tasks");
    }

    public boolean isTaskListVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(taskList)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getTaskCount() {
        try {
            wait.until(ExpectedConditions.visibilityOf(taskList));
            return taskItems.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public void selectTask(int index) {
        if (index < taskItems.size()) {
            WebElement task = taskItems.get(index);
            WebElement checkbox = task.findElement(By.cssSelector("input[type='checkbox'], .task-checkbox"));
            if (!checkbox.isSelected()) {
                checkbox.click();
            }
        }
    }

    public void selectTaskByTitle(String title) {
        for (WebElement task : taskItems) {
            String taskTitle = task.findElement(By.cssSelector(".task-title, [data-testid='task-title']")).getText();
            if (taskTitle.contains(title)) {
                WebElement checkbox = task.findElement(By.cssSelector("input[type='checkbox'], .task-checkbox"));
                if (!checkbox.isSelected()) {
                    checkbox.click();
                }
                break;
            }
        }
    }

    public void deselectTask(int index) {
        if (index < taskItems.size()) {
            WebElement task = taskItems.get(index);
            WebElement checkbox = task.findElement(By.cssSelector("input[type='checkbox'], .task-checkbox"));
            if (checkbox.isSelected()) {
                checkbox.click();
            }
        }
    }

    public void selectAllTasks() {
        wait.until(ExpectedConditions.elementToBeClickable(selectAllButton)).click();
    }

    public void clearSelection() {
        wait.until(ExpectedConditions.elementToBeClickable(clearSelectionButton)).click();
    }

    public int getSelectedTaskCount() {
        try {
            String countText = wait.until(ExpectedConditions.visibilityOf(selectedTaskCount)).getText();
            return Integer.parseInt(countText.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public void enableOptimization() {
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(optimizationToggle));
        if (!toggle.isSelected()) {
            toggle.click();
        }
    }

    public void disableOptimization() {
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(optimizationToggle));
        if (toggle.isSelected()) {
            toggle.click();
        }
    }

    public boolean isOptimizationEnabled() {
        try {
            WebElement toggle = wait.until(ExpectedConditions.visibilityOf(optimizationToggle));
            return toggle.isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickOptimizeJourney() {
        wait.until(ExpectedConditions.elementToBeClickable(optimizeJourneyButton)).click();
    }

    public boolean isOptimizeButtonEnabled() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(optimizeJourneyButton)).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasTaskWithLocation(String locationHint) {
        for (WebElement task : taskItems) {
            try {
                String location = task.findElement(By.cssSelector(".task-location, [data-testid='task-location']"))
                        .getText();
                if (location.contains(locationHint)) {
                    return true;
                }
            } catch (Exception e) {
                // Task might not have location element
            }
        }
        return false;
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
