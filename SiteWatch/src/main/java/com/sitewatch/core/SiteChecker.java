package com.sitewatch.core;

import com.sitewatch.config.ConfigReader;
import com.sitewatch.config.SiteConfig;
import com.sitewatch.pages.GenericPage;
import com.sitewatch.utils.BrokenLinkChecker;
import com.sitewatch.utils.ResultRecorder;
import com.sitewatch.utils.ScreenshotUtil;
import com.sitewatch.utils.SiteResult;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The three health checks. Each one records its outcome in a SiteResult and never throws,
 * so both the TestNG tests and the command-line runner can use it.
 */
public class SiteChecker {

    private final WebDriver driver;
    private final GenericPage page;
    private final SiteConfig site;
    private final SiteResult result;

    public SiteChecker(WebDriver driver, SiteConfig site) {
        this.driver = driver;
        this.site = site;
        this.page = new GenericPage(driver);
        this.result = ResultRecorder.forSite(site);
    }

    /** Check 1: page loads, title matches, and it loads fast enough. */
    public SiteResult checkPage() {
        try {
            long loadMs = page.open(site.url);
            String title = page.getTitle();
            long maxMs = ConfigReader.getInt("max.load.time.ms", 8000);

            boolean titleOk = site.expectedTitle == null || site.expectedTitle.isBlank()
                    || title.toLowerCase().contains(site.expectedTitle.toLowerCase());

            result.loadTimeMs = loadMs;
            result.title = title;
            if (!titleOk) {
                result.addNote("Title mismatch: expected text '" + site.expectedTitle + "'");
            }
            if (loadMs > maxMs) {
                result.addNote("Slow page: " + loadMs + " ms (limit " + maxMs + " ms)");
            }
            result.pageOk = titleOk && loadMs <= maxMs;
        } catch (Exception e) {
            result.pageOk = false;
            result.addNote("Page failed to load: " + e.getClass().getSimpleName());
        }
        if (!Boolean.TRUE.equals(result.pageOk)) {
            screenshot("page");
        }
        return result;
    }

    /** Check 2: every required element (CSS selector) is present. */
    public SiteResult checkElements() {
        List<String> selectors = site.requiredSelectors;
        int found = 0;
        try {
            ensureLoaded();
            for (String css : selectors) {
                if (page.isPresent(css)) {
                    found++;
                } else {
                    result.addNote("Missing element: " + css);
                }
            }
            result.elementsOk = (found == selectors.size());
        } catch (Exception e) {
            result.elementsOk = false;
            result.addNote("Element check error: " + e.getClass().getSimpleName());
        }
        result.elementsFound = found;
        result.elementsTotal = selectors.size();
        if (!Boolean.TRUE.equals(result.elementsOk)) {
            screenshot("elements");
        }
        return result;
    }

    /** Check 3: crawl every link on the page and report broken ones. */
    public SiteResult checkLinks() {
        try {
            ensureLoaded();
            page.scrollToBottom();

            int maxLinks = ConfigReader.getInt("link.max.per.site", 60);
            Set<String> urls = new LinkedHashSet<>();
            for (String href : page.getLinkHrefs()) {
                String link = href.trim();
                int hash = link.indexOf('#');
                if (hash >= 0) {
                    link = link.substring(0, hash);
                }
                if (link.startsWith("http://") || link.startsWith("https://")) {
                    urls.add(link);
                }
                if (urls.size() >= maxLinks) {
                    break;
                }
            }

            BrokenLinkChecker.Report report =
                    new BrokenLinkChecker(ConfigReader.getInt("link.timeout.ms", 8000)).check(urls);
            result.linksChecked = report.checked;
            result.linksBroken = report.broken.size();

            double brokenPercent = report.checked == 0 ? 0 : 100.0 * report.broken.size() / report.checked;
            result.linksOk = brokenPercent <= ConfigReader.getInt("max.broken.percent", 10);

            if (!report.broken.isEmpty()) {
                int shown = Math.min(3, report.broken.size());
                result.addNote("Broken links: " + String.join(", ", report.broken.subList(0, shown)));
            }
        } catch (Exception e) {
            result.linksOk = false;
            result.addNote("Link check error: " + e.getClass().getSimpleName());
        }
        if (!Boolean.TRUE.equals(result.linksOk)) {
            screenshot("links");
        }
        return result;
    }

    /** Each test uses a fresh browser, so open the site if nothing is loaded yet. */
    private void ensureLoaded() {
        String current = driver.getCurrentUrl();
        if (current == null || current.startsWith("about:") || current.startsWith("data:")) {
            page.open(site.url);
        }
    }

    private void screenshot(String tag) {
        String path = ScreenshotUtil.capture(driver, site.name, tag);
        if (!path.isEmpty()) {
            result.screenshot = path;
        }
    }
}
