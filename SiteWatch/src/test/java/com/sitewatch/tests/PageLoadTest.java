package com.sitewatch.tests;

import com.sitewatch.config.SiteConfig;
import com.sitewatch.core.SiteChecker;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Checks that each site loads, has the expected title, and loads within the time budget. */
public class PageLoadTest extends BaseTest {

    @Test(dataProvider = "sites", groups = {"smoke", "performance"})
    public void siteLoadsSuccessfully(SiteConfig site) {
        var result = new SiteChecker(driver, site).checkPage();
        Assert.assertTrue(Boolean.TRUE.equals(result.pageOk),
                site.name + " failed page check: " + result.notesText());
    }
}
