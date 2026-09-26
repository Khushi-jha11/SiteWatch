package com.sitewatch.tests;

import com.sitewatch.base.DriverFactory;
import com.sitewatch.config.ConfigReader;
import com.sitewatch.config.SiteConfig;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.List;

/** Shared setup/teardown for every test class: one browser per test method, one @DataProvider for sites. */
public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    @Parameters("browser")
    public void setUp(@Optional("") String browser) {
        driver = browser.isBlank() ? DriverFactory.createDriver() : DriverFactory.createDriver(browser);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    /** Feeds every site from sites.json into each @Test(dataProvider = "sites") method. */
    @DataProvider(name = "sites", parallel = true)
    public Object[][] sites() {
        List<SiteConfig> sites = ConfigReader.loadSites();
        Object[][] data = new Object[sites.size()][1];
        for (int i = 0; i < sites.size(); i++) {
            data[i][0] = sites.get(i);
        }
        return data;
    }
}
