package com.sitewatch.tests;

import com.sitewatch.config.SiteConfig;
import com.sitewatch.core.SiteChecker;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Crawls each site's links and fails if too many of them are broken. */
public class LinkTest extends BaseTest {

    @Test(dataProvider = "sites", groups = {"links", "regression"})
    public void linksAreHealthy(SiteConfig site) {
        var result = new SiteChecker(driver, site).checkLinks();
        Assert.assertTrue(Boolean.TRUE.equals(result.linksOk),
                site.name + " has too many broken links: " + result.notesText());
    }
}
