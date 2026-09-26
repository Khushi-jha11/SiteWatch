package com.sitewatch.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** One entry of sites.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteConfig {
    public String name;
    public String url;
    /** Page title must contain this text (case-insensitive). Leave empty to skip. */
    public String expectedTitle = "";
    /** CSS selectors that must exist on the page. */
    public List<String> requiredSelectors = new ArrayList<>();

    public SiteConfig() {
    }

    @Override
    public String toString() {
        return name; // shows a readable name in TestNG reports
    }
}
