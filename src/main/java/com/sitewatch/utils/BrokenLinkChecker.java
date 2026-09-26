package com.sitewatch.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Checks URLs in parallel and reports the ones that are broken (404, 5xx, timeout...). */
public class BrokenLinkChecker {

    public static class Report {
        public int checked;
        public final List<String> broken = new ArrayList<>();
    }

    /** Sites often answer these to bots even when the page is fine, so they do not count as broken. */
    private static final Set<Integer> NOT_BROKEN = Set.of(401, 403, 429, 999);

    private final HttpClient client;
    private final Duration timeout;

    public BrokenLinkChecker(int timeoutMs) {
        this.timeout = Duration.ofMillis(timeoutMs);
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout)
                .build();
    }

    public Report check(Collection<String> urls) {
        Report report = new Report();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            Map<String, Future<Integer>> futures = new LinkedHashMap<>();
            for (String url : urls) {
                futures.put(url, pool.submit(() -> statusOf(url)));
            }
            for (Map.Entry<String, Future<Integer>> entry : futures.entrySet()) {
                int code;
                try {
                    code = entry.getValue().get();
                } catch (Exception e) {
                    code = -1;
                }
                report.checked++;
                if (isBroken(code)) {
                    report.broken.add((code < 0 ? "ERR" : String.valueOf(code)) + " " + entry.getKey());
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return report;
    }

    private boolean isBroken(int code) {
        return code < 0 || (code >= 400 && !NOT_BROKEN.contains(code));
    }

    private int statusOf(String url) {
        try {
            int code = send(url, "HEAD");
            if (code >= 400) { // some servers reject HEAD, so retry once with GET
                code = send(url, "GET");
            }
            return code;
        } catch (Exception e) {
            try {
                return send(url, "GET");
            } catch (Exception ex) {
                return -1;
            }
        }
    }

    private int send(String url, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", "Mozilla/5.0 (SiteWatch link checker)")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
