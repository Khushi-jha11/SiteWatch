package com.sitewatch.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Reads config.properties. Any value can be overridden from the command line,
 * e.g. mvn test -Dbrowser=firefox -Dheadless=false
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ConfigReader() {
    }

    public static String get(String key, String defaultValue) {
        String fromCli = System.getProperty(key);
        if (fromCli != null && !fromCli.isBlank()) {
            return fromCli.trim();
        }
        return PROPS.getProperty(key, defaultValue).trim();
    }

    public static int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    /** Loads sites.json from the classpath, or from -Dsites.file=/path/to/file.json */
    public static List<SiteConfig> loadSites() {
        String file = get("sites.file", "");
        try (InputStream in = file.isBlank()
                ? ConfigReader.class.getClassLoader().getResourceAsStream("sites.json")
                : Files.newInputStream(Paths.get(file))) {
            if (in == null) {
                throw new IllegalStateException("sites.json not found");
            }
            return new ObjectMapper().readValue(in, new TypeReference<List<SiteConfig>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
