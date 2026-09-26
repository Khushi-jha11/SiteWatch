package com.sitewatch.utils;

import com.sitewatch.config.ConfigReader;
import com.sitewatch.config.SiteConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Collects results from all tests and appends them to docs/data/history.csv at the end of the run. */
public final class ResultRecorder {

    private static final String HEADER =
            "timestamp,site,url,status,load_time_ms,title,elements_found,elements_total,"
                    + "links_checked,links_broken,notes,screenshot";

    private static final Map<String, SiteResult> RESULTS = new ConcurrentHashMap<>();

    private ResultRecorder() {
    }

    public static SiteResult forSite(SiteConfig site) {
        return RESULTS.computeIfAbsent(site.name, name -> new SiteResult(site.name, site.url));
    }

    public static synchronized void flush() {
        if (RESULTS.isEmpty()) {
            return;
        }
        Path csv = Paths.get(ConfigReader.get("output.dir", "docs"), "data", "history.csv");
        try {
            Files.createDirectories(csv.getParent());
            boolean needsHeader = !Files.exists(csv) || Files.size(csv) == 0;

            List<SiteResult> sorted = RESULTS.values().stream()
                    .sorted((a, b) -> a.site.compareToIgnoreCase(b.site))
                    .collect(Collectors.toList());

            StringBuilder out = new StringBuilder();
            if (needsHeader) {
                out.append(HEADER).append('\n');
            }
            for (SiteResult r : sorted) {
                out.append(row(r)).append('\n');
            }
            Files.writeString(csv, out.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            System.out.println("\n===== SiteWatch summary =====");
            for (SiteResult r : sorted) {
                System.out.printf("%-14s %-4s load=%5d ms  elements=%d/%d  broken links=%d/%d%n",
                        r.site, r.status(), r.loadTimeMs, r.elementsFound, r.elementsTotal,
                        r.linksBroken, r.linksChecked);
            }
            System.out.println("Saved to " + csv.toAbsolutePath());
            RESULTS.clear();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String row(SiteResult r) {
        return String.join(",",
                q(r.timestamp), q(r.site), q(r.url), q(r.status()),
                String.valueOf(r.loadTimeMs), q(r.title),
                String.valueOf(r.elementsFound), String.valueOf(r.elementsTotal),
                String.valueOf(r.linksChecked), String.valueOf(r.linksBroken),
                q(r.notesText()), q(r.screenshot));
    }

    /** CSV-safe quoting. */
    private static String q(String value) {
        String clean = value == null ? "" : value.replaceAll("[\\r\\n]+", " ");
        return "\"" + clean.replace("\"", "\"\"") + "\"";
    }
}
