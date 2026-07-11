package net.pcal.fwportals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

final class ForeverWorldPortalsConfigParser {

    static ForeverWorldPortalsConfig parse(InputStream in, ForeverWorldPortalsConfig defaults, Logger logger) throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));

        boolean enabled = parseBoolean(properties, "enabled", defaults.enabled(), logger);
        Level logLevel = parseLevel(properties, "logLevel", defaults.logLevel(), logger);
        return new ForeverWorldPortalsConfig(enabled, logLevel);
    }

    private static boolean parseBoolean(Properties properties, String key, boolean defaultValue, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        if (logger != null) {
            logger.warn(
                    ForeverWorldPortalsService.LOG_PREFIX + "Invalid boolean value '{}' for '{}'; using default {}",
                    value,
                    key,
                    defaultValue
            );
        }
        return defaultValue;
    }

    private static Level parseLevel(Properties properties, String key, Level defaultValue, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        Level parsed = Level.getLevel(value.trim().toUpperCase(Locale.ROOT));
        if (parsed != null) {
            return parsed;
        }
        if (logger != null) {
            logger.warn(
                    ForeverWorldPortalsService.LOG_PREFIX + "Invalid log level '{}' for '{}'; using default {}",
                    value,
                    key,
                    defaultValue
            );
        }
        return defaultValue;
    }

    private ForeverWorldPortalsConfigParser() {
    }
}
