package net.pcal.fwportals.client.config;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

public final class ClientConfigLoader {

    static final String DEFAULT_CONFIG_RESOURCE_NAME = "fwportals-default.properties";
    static final String CONFIG_FILENAME = "fwportals.properties";
    static final String PORTAL_COLOR_KEY = "client.portalColor";
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public static ClientConfig load(Logger logger) {
        Path configPath = Paths.get("config").resolve(CONFIG_FILENAME);
        try {
            Properties bundledDefaults = loadBundledDefaults();
            int defaultPortalColor = parseBundledDefaultColor(
                    bundledDefaults,
                    PORTAL_COLOR_KEY,
                    DEFAULT_CONFIG_RESOURCE_NAME
            );
            Properties userProperties = Files.exists(configPath) ? loadProperties(Files.newInputStream(configPath)) : new Properties();
            return new ClientConfig(
                    parseUserColor(userProperties.getProperty(PORTAL_COLOR_KEY), defaultPortalColor, logger)
            );
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load Forever World Portals client configuration", ioe);
        }
    }

    public static int parseColor(String value) {
        if (!HEX_COLOR_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Expected #RRGGBB color value");
        }
        return Integer.parseInt(value.substring(1), 16);
    }

    private static Properties loadBundledDefaults() throws IOException {
        try (InputStream in = ClientConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME)) {
            if (in == null) {
                throw new IllegalStateException("Unable to load bundled configuration resource '" + DEFAULT_CONFIG_RESOURCE_NAME + "'");
            }
            return loadProperties(in);
        }
    }

    private static Properties loadProperties(InputStream in) throws IOException {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    static int parseBundledDefaultColor(Properties properties, String key, String resourceName) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Missing required default config key '" + key + "' in bundled resource '" + resourceName + "'"
            );
        }
        try {
            return parseColor(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid bundled default value '" + value + "' for config key '" + key + "' in resource '" + resourceName
                            + "'; expected #RRGGBB",
                    ex
            );
        }
    }

    static int parseUserColor(String value, int defaultPortalColor, Logger logger) {
        if (value == null) {
            return defaultPortalColor;
        }
        try {
            return parseColor(value.trim());
        } catch (IllegalArgumentException ex) {
            if (logger != null) {
                logger.warn(
                        "[fwportals] Invalid color value '{}' for '{}'; using default #{}",
                        value,
                        PORTAL_COLOR_KEY,
                        String.format("%06X", defaultPortalColor)
                );
            }
            return defaultPortalColor;
        }
    }

    private ClientConfigLoader() {
    }
}
