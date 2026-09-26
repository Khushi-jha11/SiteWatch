package com.sitewatch.utils;

import com.sitewatch.config.ConfigReader;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    /**
     * Saves a screenshot to docs/screenshots and returns its path relative to docs/
     * (so the dashboard can link to it). Returns "" if it could not be taken.
     */
    public static String capture(WebDriver driver, String siteName, String tag) {
        try {
            if (!(driver instanceof TakesScreenshot)) {
                return "";
            }
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String slug = siteName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String fileName = slug + "_" + tag + "_" + stamp + ".png";

            Path dir = Paths.get(ConfigReader.get("output.dir", "docs"), "screenshots");
            Files.createDirectories(dir);
            Files.copy(source.toPath(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return "screenshots/" + fileName;
        } catch (Exception e) {
            return "";
        }
    }
}
