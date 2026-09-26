package com.sitewatch.tests;

import com.sitewatch.config.SiteConfig;
import com.sitewatch.core.SiteChecker;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Checks that the key elements each site is expected to have (nav, search box, footer...) are present.
 * Declared to depend on the "smoke" group, so element checks only run after page-load checks
 * have finished for the suite - a simple example of TestNG's group dependencies.
 */
@Test(dependsOnGroups = "smoke")
public class ElementTest extends BaseTest {

    @Test(dataProvider = "sites", groups = {"regression"})
    public void requiredElementsArePresent(SiteConfig site) {
        var result = new SiteChecker(driver, site).checkElements();
        Assert.assertTrue(Boolean.TRUE.equals(result.elementsOk),
                site.name + " is missing elements: " + result.notesText());
    }
}
