package net.pcal.fwportals.common.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.pcal.fwportals.CommonService;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class CommonConfigParser {

    private static final Set<String> EXPECTED_KEYS = Set.of(
            "requireEmptyInventory",
            "logLevel",
            "frameBlock",
            "destinationPortalMode",
            "destinationSpiralSpacingBlocks",
            "maximumSpiralSearchPositions",
            "maximumBiomeSearches",
            "maximumPortalPlacementAttemptsPerBiome",
            "minimumGeneratedTerrainDistanceBlocks"
    );

    static CommonConfig parseBundledDefaults(Properties bundledDefaults, String defaultResourceName) {
        for (String key : EXPECTED_KEYS) {
            requireDefaultValue(bundledDefaults, defaultResourceName, key);
        }

        return parse(
                bundledDefaults,
                buildInternalDefaults(bundledDefaults, defaultResourceName),
                null
        );
    }

    static CommonConfig parse(
            Properties properties,
            CommonConfig defaults,
            Logger logger
    ) {
        boolean requireEmptyInventory = parseBoolean(
                properties,
                "requireEmptyInventory",
                defaults.requireEmptyInventory(),
                logger
        );
        Level logLevel = parseLevel(properties, "logLevel", defaults.logLevel(), logger);
        Block frameBlock = parseBlock(properties, "frameBlock", defaults.frameBlock(), logger);
        CommonConfig.DestinationPortalMode destinationPortalMode = parseDestinationPortalMode(
                properties,
                "destinationPortalMode",
                defaults.destinationPortalMode(),
                logger
        );
        int destinationSpiralSpacingBlocks = parsePositiveInt(
                properties,
                "destinationSpiralSpacingBlocks",
                defaults.destinationSpiralSpacingBlocks(),
                logger
        );
        int maximumSpiralSearchPositions = parsePositiveInt(
                properties,
                "maximumSpiralSearchPositions",
                defaults.maximumSpiralSearchPositions(),
                logger
        );
        int maximumBiomeSearches = parsePositiveInt(
                properties,
                "maximumBiomeSearches",
                defaults.maximumBiomeSearches(),
                logger
        );
        int maximumPortalPlacementAttemptsPerBiome = parsePositiveInt(
                properties,
                "maximumPortalPlacementAttemptsPerBiome",
                defaults.maximumPortalPlacementAttemptsPerBiome(),
                logger
        );
        int minimumGeneratedTerrainDistanceBlocks = parsePositiveInt(
                properties,
                "minimumGeneratedTerrainDistanceBlocks",
                defaults.minimumGeneratedTerrainDistanceBlocks(),
                logger
        );
        return new CommonConfig(
                requireEmptyInventory,
                logLevel,
                BuiltInRegistries.BLOCK.getKey(frameBlock),
                frameBlock,
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
                        CommonService.LOG_PREFIX + "Invalid identifier '{}' for '{}'; using default",
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
                    CommonService.LOG_PREFIX + "Invalid boolean value '{}' for '{}'; using default {}",
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
                    CommonService.LOG_PREFIX + "Invalid log level '{}' for '{}'; using default {}",
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
                        CommonService.LOG_PREFIX + "Unknown block '{}' for '{}'; using default {}",
                        value,
                        key,
                        BuiltInRegistries.BLOCK.getKey(defaultValue)
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
                    CommonService.LOG_PREFIX + "Invalid positive integer '{}' for '{}'; using default {}",
                    value,
                    key,
                    defaultValue
            );
        }
        return defaultValue;
    }

    private static CommonConfig.DestinationPortalMode parseDestinationPortalMode(
            Properties properties,
            String key,
            CommonConfig.DestinationPortalMode defaultValue,
            Logger logger
    ) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return CommonConfig.DestinationPortalMode.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
        }
        if (logger != null) {
            logger.warn(
                    CommonService.LOG_PREFIX
                            + "Invalid destination portal mode '{}' for '{}'; using default {}. Accepted values: NONE, RUINED, COMPLETE",
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

    private static CommonConfig buildInternalDefaults(Properties bundledDefaults, String defaultResourceName) {
        return new CommonConfig(
                requireParsedBooleanDefault(
                        defaultResourceName,
                        "requireEmptyInventory",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "requireEmptyInventory")
                ),
                requireParsedLevelDefault(
                        defaultResourceName,
                        "logLevel",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "logLevel")
                ),
                BuiltInRegistries.BLOCK.getKey(
                        requireParsedBlockDefault(
                                defaultResourceName,
                                "frameBlock",
                                requireDefaultValue(bundledDefaults, defaultResourceName, "frameBlock")
                        )
                ),
                requireParsedBlockDefault(
                        defaultResourceName,
                        "frameBlock",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "frameBlock")
                ),
                requireParsedDestinationPortalModeDefault(
                        defaultResourceName,
                        "destinationPortalMode",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "destinationPortalMode")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "destinationSpiralSpacingBlocks",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "destinationSpiralSpacingBlocks")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "maximumSpiralSearchPositions",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "maximumSpiralSearchPositions")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "maximumBiomeSearches",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "maximumBiomeSearches")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "maximumPortalPlacementAttemptsPerBiome",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "maximumPortalPlacementAttemptsPerBiome")
                ),
                requireParsedPositiveIntDefault(
                        defaultResourceName,
                        "minimumGeneratedTerrainDistanceBlocks",
                        requireDefaultValue(bundledDefaults, defaultResourceName, "minimumGeneratedTerrainDistanceBlocks")
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

    private static CommonConfig.DestinationPortalMode requireParsedDestinationPortalModeDefault(
            String defaultResourceName,
            String key,
            String defaultValue
    ) {
        try {
            return CommonConfig.DestinationPortalMode.valueOf(defaultValue.trim());
        } catch (IllegalArgumentException ignored) {
        }
        throw invalidBundledDefault(defaultResourceName, key, defaultValue, "destination portal mode");
    }

    private CommonConfigParser() {
    }
}
