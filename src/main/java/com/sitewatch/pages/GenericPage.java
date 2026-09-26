package com.sitewatch.pages;

import com.sitewatch.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.ArrayList;
import java.util.List;

/** Page object that works for any website. */
public class GenericPage extends BasePage {

    public GenericPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens the URL and returns the load time in ms, measured by the browser's
     * Navigation Timing API (falls back to wall-clock time).
     */
    public long open(String url) {
        long start = System.nanoTime();
        driver.get(url);
        long wallClockMs = (System.nanoTime() - start) / 1_000_000;
        try {
            wait.until((ExpectedCondition<Boolean>) d -> "complete".equals(js("return document.readyState")));
            Object value = js("var t = performance.timing;"
                    + "return t.loadEventEnd > 0 ? (t.loadEventEnd - t.navigationStart) : 0;");
            long navMs = value instanceof Number ? ((Number) value).longValue() : 0;
            return navMs > 0 ? navMs : wallClockMs;
        } catch (WebDriverException e) {
            return wallClockMs;
        }
    }

    /** All href values of every anchor tag on the page. */
    public List<String> getLinkHrefs() {
        List<String> hrefs = new ArrayList<>();
        for (WebElement anchor : driver.findElements(By.tagName("a"))) {
            try {
                String href = anchor.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    hrefs.add(href);
                }
            } catch (StaleElementReferenceException ignored) {
                // element vanished while the page was updating
            }
        }
        return hrefs;
    }
}
