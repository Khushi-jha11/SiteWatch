package com.sitewatch;

import com.sitewatch.base.DriverFactory;
import com.sitewatch.config.ConfigReader;
import com.sitewatch.config.SiteConfig;
import com.sitewatch.core.SiteChecker;
import com.sitewatch.utils.ResultRecorder;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Entry point of the fat JAR (no Maven or TestNG needed):
 *   java -jar target/sitewatch-1.0.0-fat.jar [path/to/sites.json]
 */
public class SiteWatchCli {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.setProperty("sites.file", args[0]);
        }

        List<SiteConfig> sites = ConfigReader.loadSites();
        System.out.println("SiteWatch: checking " + sites.size() + " site(s)...");

        for (SiteConfig site : sites) {
            try {
                WebDriver driver = DriverFactory.createDriver();
                SiteChecker checker = new SiteChecker(driver, site);
                checker.checkPage();
                checker.checkElements();
                checker.checkLinks();
            } catch (Exception e) {
                var result = ResultRecorder.forSite(site);
                result.pageOk = false;
                result.addNote("Run error: " + e.getClass().getSimpleName());
            } finally {
                DriverFactory.quitDriver();
            }
        }
        ResultRecorder.flush();
    }
}
