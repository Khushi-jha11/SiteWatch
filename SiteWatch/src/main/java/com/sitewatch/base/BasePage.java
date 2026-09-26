package com.sitewatch.base;

import com.sitewatch.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Parent of every page object: shared driver, explicit wait and small helpers. */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait.sec", 10)));
    }

    public String getTitle() {
        String title = driver.getTitle();
        return title == null ? "" : title;
    }

    /** True if an element matching the CSS selector shows up within the explicit wait. */
    public boolean isPresent(String cssSelector) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)));
            return true;
        } catch (WebDriverException e) { // timeout or invalid selector
            return false;
        }
    }

    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    public void scrollToBottom() {
        js("window.scrollTo(0, document.body.scrollHeight);");
    }

    public void scrollToTop() {
        js("window.scrollTo(0, 0);");
    }
}
