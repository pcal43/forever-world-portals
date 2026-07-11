package net.pcal.fwportals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
        Block frameBlock = parseBlock(properties, "frameBlock", defaults.frameBlock(), logger);
        Item activationItem = parseItem(properties, "activationItem", defaults.activationItem(), logger);
        ReturnPortalMode returnPortalMode = parseReturnPortalMode(
                properties,
                "returnPortalMode",
                defaults.returnPortalMode(),
                logger
        );
        int minimumPortalSeparationBlocks = parsePositiveInt(
                properties,
                "minimumPortalSeparationBlocks",
                defaults.minimumPortalSeparationBlocks(),
                logger
        );
        int destinationSearchAttempts = parsePositiveInt(
                properties,
                "destinationSearchAttempts",
                defaults.destinationSearchAttempts(),
                logger
        );
        return new ForeverWorldPortalsConfig(
                enabled,
                logLevel,
                BuiltInRegistries.BLOCK.getKey(frameBlock),
                frameBlock,
                BuiltInRegistries.ITEM.getKey(activationItem),
                activationItem,
                returnPortalMode,
                minimumPortalSeparationBlocks,
                destinationSearchAttempts
        );
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

    private static Block parseBlock(Properties properties, String key, Block defaultValue, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        Identifier id = parseIdentifier(key, value, logger);
        if (id == null) {
            return defaultValue;
        }
        return BuiltInRegistries.BLOCK.getOptional(id).orElseGet(() -> {
            if (logger != null) {
                logger.warn(
                        ForeverWorldPortalsService.LOG_PREFIX + "Unknown block '{}' for '{}'; using default {}",
                        value,
                        key,
                        BuiltInRegistries.BLOCK.getKey(defaultValue)
                );
            }
            return defaultValue;
        });
    }

    private static Item parseItem(Properties properties, String key, Item defaultValue, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        Identifier id = parseIdentifier(key, value, logger);
        if (id == null) {
            return defaultValue;
        }
        return BuiltInRegistries.ITEM.getOptional(id).orElseGet(() -> {
            if (logger != null) {
                logger.warn(
                        ForeverWorldPortalsService.LOG_PREFIX + "Unknown item '{}' for '{}'; using default {}",
                        value,
                        key,
                        BuiltInRegistries.ITEM.getKey(defaultValue)
                );
            }
            return defaultValue;
        });
    }

    private static Identifier parseIdentifier(String key, String value, Logger logger) {
        try {
            return Identifier.parse(value.trim());
        } catch (RuntimeException ex) {
            if (logger != null) {
                logger.warn(
                        ForeverWorldPortalsService.LOG_PREFIX + "Invalid identifier '{}' for '{}'; using default",
                        value,
                        key
                );
            }
            return null;
        }
    }

    private static int parsePositiveInt(Properties properties, String key, int defaultValue, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        if (logger != null) {
            logger.warn(
                    ForeverWorldPortalsService.LOG_PREFIX + "Invalid positive integer '{}' for '{}'; using default {}",
                    value,
                    key,
                    defaultValue
            );
        }
        return defaultValue;
    }

    private static ReturnPortalMode parseReturnPortalMode(Properties properties, String key, ReturnPortalMode defaultValue, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return ReturnPortalMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
        }

        if (logger != null) {
            logger.warn(
                    ForeverWorldPortalsService.LOG_PREFIX + "Invalid return portal mode '{}' for '{}'; using default {}",
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
