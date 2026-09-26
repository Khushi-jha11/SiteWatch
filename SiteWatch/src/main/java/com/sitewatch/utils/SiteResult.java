package com.sitewatch.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Everything measured for one site in one run. Fields are volatile because tests run in parallel. */
public class SiteResult {

    public final String site;
    public final String url;
    public final String timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

    public volatile long loadTimeMs;
    public volatile String title = "";
    public volatile int elementsFound;
    public volatile int elementsTotal;
    public volatile int linksChecked;
    public volatile int linksBroken;
    public volatile String screenshot = "";

    /** null = check was not run, TRUE = passed, FALSE = failed. */
    public volatile Boolean pageOk;
    public volatile Boolean elementsOk;
    public volatile Boolean linksOk;

    private final List<String> notes = Collections.synchronizedList(new ArrayList<>());

    public SiteResult(String site, String url) {
        this.site = site;
        this.url = url;
    }

    public void addNote(String note) {
        notes.add(note);
    }

    public String notesText() {
        synchronized (notes) {
            return String.join(" | ", notes);
        }
    }

    public String status() {
        boolean anyFailed = Boolean.FALSE.equals(pageOk)
                || Boolean.FALSE.equals(elementsOk)
                || Boolean.FALSE.equals(linksOk);
        boolean anyPassed = Boolean.TRUE.equals(pageOk)
                || Boolean.TRUE.equals(elementsOk)
                || Boolean.TRUE.equals(linksOk);
        return (!anyFailed && anyPassed) ? "PASS" : "FAIL";
    }
}
