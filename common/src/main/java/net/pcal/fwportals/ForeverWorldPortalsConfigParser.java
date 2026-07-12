package net.pcal.fwportals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

final class ForeverWorldPortalsConfigParser {

    private static final Set<String> EXPECTED_KEYS = Set.of(
            "server.enabled",
            "server.requireEmptyInventory",
            "server.logLevel",
            "server.frameBlock",
            "server.activationItem",
            "server.destinationPortalMode",
            "server.destinationSpiralSpacingBlocks",
            "server.maximumSpiralSearchPositions",
            "server.maximumBiomeSearches",
            "server.maximumPortalPlacementAttemptsPerBiome",
            "server.minimumGeneratedTerrainDistanceBlocks"
    );

    static ForeverWorldPortalsConfig parseBundledDefaults(Properties bundledDefaults, String defaultResourceName) {
        for (String key : EXPECTED_KEYS) {
            requireDefaultValue(bundledDefaults, defaultResourceName, key);
        }

        return parse(
                bundledDefaults,
                buildInternalDefaults(bundledDefaults, defaultResourceName),
                null
        );
    }

    static ForeverWorldPortalsConfig parse(
            Properties properties,
            ForeverWorldPortalsConfig defaults,
            Logger logger
    ) {
        boolean enabled = parseBoolean(
                properties,
                "server.enabled",
                defaults.enabled(),
                logger
        );
        boolean requireEmptyInventory = parseBoolean(
                properties,
                "server.requireEmptyInventory",
                defaults.requireEmptyInventory(),
                logger
        );
        Level logLevel = parseLevel(properties, "server.logLevel", defaults.logLevel(), logger);
        Block frameBlock = parseBlock(properties, "server.frameBlock", defaults.frameBlock(), logger);
        Item activationItem = parseItem(
                properties,
                "server.activationItem",
                defaults.activationItem(),
                logger
        );
        DestinationPortalMode destinationPortalMode = parseDestinationPortalMode(
                properties,
                "server.destinationPortalMode",
                defaults.destinationPortalMode(),
                logger
        );
        int destinationSpiralSpacingBlocks = parsePositiveInt(
                properties,
                "server.destinationSpiralSpacingBlocks",
                defaults.destinationSpiralSpacingBlocks(),
                logger
        );
        int maximumSpiralSearchPositions = parsePositiveInt(
                properties,
                "server.maximumSpiralSearchPositions",
                defaults.maximumSpiralSearchPositions(),
                logger
        );
        int maximumBiomeSearches = parsePositiveInt(
                properties,
                "server.maximumBiomeSearches",
                defaults.maximumBiomeSearches(),
                logger
        );
        int maximumPortalPlacementAttemptsPerBiome = parsePositiveInt(
                properties,
                "server.maximumPortalPlacementAttemptsPerBiome",
                defaults.maximumPortalPlacementAttemptsPerBiome(),
                logger
        );
        int minimumGeneratedTerrainDistanceBlocks = parsePositiveInt(
                properties,
                "server.minimumGeneratedTerrainDistanceBlocks",
                defaults.minimumGeneratedTerrainDistanceBlocks(),
                logger
        );
        return new ForeverWorldPortalsConfig(
                enabled,
                requireEmptyInventory,
                logLevel,
                BuiltInRegistries.BLOCK.getKey(frameBlock),
                frameBlock,
                BuiltInRegistries.ITEM.getKey(activationItem),
                activationItem,
                destinationPortalMode,
                destinationSpiralSpacingBlocks,
                maximumSpiralSearchPositions,
                maximumBiomeSearches,
                maximumPortalPlacementAttemptsPerBiome,
                minimumGeneratedTerrainDistanceBlocks
        );
    }

    static Set<String> expectedKeys() {
        return new LinkedHashSet<>(EXPECTED_KEYS);
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

    private static DestinationPortalMode parseDestinationPortalMode(
            Properties properties,
            String key,
            DestinationPortalMode defaultValue,
            Logger logger
    ) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return DestinationPortalMode.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
        }
        if (logger != null) {
            logger.warn(
                    ForeverWorldPortalsService.LOG_PREFIX
                            + "Invalid destination portal mode '{}' for '{}'; using default {}. Accepted values: NONE, BROKEN, COMPLETE",
                    value,
                    key,
                    defaultValue.name()
            );
        }
        return defaultValue;
    }

    private static String requireDefaultValue(Properties bundledDefaults, String defaultResourceName, String key) {
        String value = bundledDefaults.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Missing required default config key '" + key + "' in bundled resource '" + defaultResourceName + "'"
            );
        }
        return value;
    }

    private static IllegalStateException invalidBundledDefault(
            String defaultResourceName,
            String key,
            String value,
            String expectedType
    ) {
        return new IllegalStateException(
                "Invalid bundled default value '" + value + "' for config key '" + key + "' in resource '"
                        + defaultResourceName + "'; expected " + expectedType
        );
    }

    private static ForeverWorldPortalsConfig buildInternalDefaults(Properties bundledDefaults, String defaultResourceName) {
        return new ForeverWorldPortalsConfig(
                requireParsedBooleanDefault(
                        defaultResourceName,
                        "server.enabled",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.enabled")
                ),
                requireParsedBooleanDefault(
                        defaultResourceName,
                        "server.requireEmptyInventory",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.requireEmptyInventory")
                ),
                requireParsedLevelDefault(
                        defaultResourceName,
                        "server.logLevel",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.logLevel")
                ),
                BuiltInRegistries.BLOCK.getKey(
                        requireParsedBlockDefault(
                                defaultResourceName,
                                "server.frameBlock",
                                requireDefaultValue(bundledDefaults, defaultResourceName, "server.frameBlock")
                        )
                ),
                requireParsedBlockDefault(
                        defaultResourceName,
                        "server.frameBlock",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.frameBlock")
                ),
                BuiltInRegistries.ITEM.getKey(
                        requireParsedItemDefault(
                                defaultResourceName,
                                "server.activationItem",
                                requireDefaultValue(bundledDefaults, defaultResourceName, "server.activationItem")
                        )
                ),
                requireParsedItemDefault(
                        defaultResourceName,
                        "server.activationItem",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.activationItem")
                ),
                requireParsedDestinationPortalModeDefault(
                        defaultResourceName,
                        "server.destinationPortalMode",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.destinationPortalMode")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "server.destinationSpiralSpacingBlocks",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.destinationSpiralSpacingBlocks")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "server.maximumSpiralSearchPositions",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.maximumSpiralSearchPositions")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "server.maximumBiomeSearches",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.maximumBiomeSearches")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "server.maximumPortalPlacementAttemptsPerBiome",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.maximumPortalPlacementAttemptsPerBiome")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "server.minimumGeneratedTerrainDistanceBlocks",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "server.minimumGeneratedTerrainDistanceBlocks")
                )
        );
    }

    private static boolean requireParsedBooleanDefault(String defaultResourceName, String key, String defaultValue) {
        String normalized = defaultValue.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw invalidBundledDefault(defaultResourceName, key, defaultValue, "boolean");
    }

    private static Level requireParsedLevelDefault(String defaultResourceName, String key, String defaultValue) {
        Level parsed = Level.getLevel(defaultValue.trim().toUpperCase(Locale.ROOT));
        if (parsed != null) {
            return parsed;
        }
        throw invalidBundledDefault(defaultResourceName, key, defaultValue, "log level");
    }

    private static Block requireParsedBlockDefault(String defaultResourceName, String key, String defaultValue) {
        try {
            Identifier id = Identifier.parse(defaultValue.trim());
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block != null) {
                return block;
            }
            throw invalidBundledDefault(defaultResourceName, key, defaultValue, "known block");
        } catch (RuntimeException ex) {
            throw invalidBundledDefault(defaultResourceName, key, defaultValue, "block identifier");
        }
    }

    private static Item requireParsedItemDefault(String defaultResourceName, String key, String defaultValue) {
        try {
            Identifier id = Identifier.parse(defaultValue.trim());
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null) {
                return item;
            }
            throw invalidBundledDefault(defaultResourceName, key, defaultValue, "known item");
        } catch (RuntimeException ex) {
            throw invalidBundledDefault(defaultResourceName, key, defaultValue, "item identifier");
        }
    }

    private static int requireParsedPositiveIntDefault(String defaultResourceName, String key, String defaultValue) {
        try {
            int parsed = Integer.parseInt(defaultValue.trim());
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        throw invalidBundledDefault(defaultResourceName, key, defaultValue, "positive integer");
    }

    private static DestinationPortalMode requireParsedDestinationPortalModeDefault(
            String defaultResourceName,
            String key,
            String defaultValue
    ) {
        try {
            return DestinationPortalMode.valueOf(defaultValue.trim());
        } catch (IllegalArgumentException ignored) {
        }
        throw invalidBundledDefault(defaultResourceName, key, defaultValue, "destination portal mode");
    }

    private ForeverWorldPortalsConfigParser() {
    }
}
