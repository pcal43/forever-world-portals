package net.pcal.fwportals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ForeverWorldPortalsConfigLoaderTest {

    private static final String DEFAULT_TEMPLATE = """
            # Example config
            enabled=true
            requireEmptyInventory=true
            logLevel=INFO
            frameBlock=minecraft:diamond_block
            activationItem=minecraft:flint_and_steel
            server.destinationPortalMode=BROKEN
            destinationSpiralSpacingBlocks=10000
            maximumSpiralSearchPositions=512
            maximumBiomeSearches=64
            maximumPortalPlacementAttemptsPerBiome=64
            minimumGeneratedTerrainDistanceBlocks=10000
            """;

    @Test
    void copiesBundledTemplateVerbatimWhenUserConfigIsMissing(@TempDir Path tempDir) throws IOException {
        ForeverWorldPortalsConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(ForeverWorldPortalsConfigLoader.CONFIG_FILENAME);

        loader.ensureUserConfigExists(configPath, null);

        assertEquals(DEFAULT_TEMPLATE, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    @Test
    void existingUserConfigIsNotRewrittenOrAugmented(@TempDir Path tempDir) throws IOException {
        ForeverWorldPortalsConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(ForeverWorldPortalsConfigLoader.CONFIG_FILENAME);
        Files.createDirectories(configPath.getParent());
        String partialConfig = """
                requireEmptyInventory=false
                maximumBiomeSearches=5
                """;
        Files.writeString(configPath, partialConfig, StandardCharsets.UTF_8);
        long originalModifiedTime = Files.getLastModifiedTime(configPath).toMillis();

        loader.ensureUserConfigExists(configPath, null);

        assertEquals(partialConfig, Files.readString(configPath, StandardCharsets.UTF_8));
        assertEquals(originalModifiedTime, Files.getLastModifiedTime(configPath).toMillis());
    }

    @Test
    void fullyPopulatedUserConfigOverridesBundledDefaults(@TempDir Path tempDir) throws IOException {
        TestBootstrap.ensureBootstrapped();
        ForeverWorldPortalsConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve(ForeverWorldPortalsConfigLoader.CONFIG_FILENAME);
        Files.writeString(configPath, """
                enabled=false
                requireEmptyInventory=false
                logLevel=DEBUG
                frameBlock=minecraft:emerald_block
                activationItem=minecraft:fire_charge
                server.destinationPortalMode=COMPLETE
                destinationSpiralSpacingBlocks=20000
                maximumSpiralSearchPositions=100
                maximumBiomeSearches=50
                maximumPortalPlacementAttemptsPerBiome=25
                minimumGeneratedTerrainDistanceBlocks=30000
                """, StandardCharsets.UTF_8);

        ForeverWorldPortalsConfig config = loader.load(configPath, null);

        assertFalse(config.enabled());
        assertFalse(config.requireEmptyInventory());
        assertEquals(org.apache.logging.log4j.Level.DEBUG, config.logLevel());
        assertEquals("minecraft:emerald_block", config.frameBlockId().toString());
        assertEquals("minecraft:fire_charge", config.activationItemId().toString());
        assertEquals(DestinationPortalMode.COMPLETE, config.destinationPortalMode());
        assertEquals(20000, config.destinationSpiralSpacingBlocks());
        assertEquals(100, config.maximumSpiralSearchPositions());
        assertEquals(50, config.maximumBiomeSearches());
        assertEquals(25, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(30000, config.minimumGeneratedTerrainDistanceBlocks());
    }

    @Test
    void missingUserConfigReturnsParsedBundledDefaults(@TempDir Path tempDir) throws IOException {
        TestBootstrap.ensureBootstrapped();
        ForeverWorldPortalsConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(ForeverWorldPortalsConfigLoader.CONFIG_FILENAME);

        ForeverWorldPortalsConfig config = loader.load(configPath, null);

        assertEquals(true, config.enabled());
        assertEquals(true, config.requireEmptyInventory());
        assertEquals(org.apache.logging.log4j.Level.INFO, config.logLevel());
        assertEquals("minecraft:diamond_block", config.frameBlockId().toString());
        assertEquals("minecraft:flint_and_steel", config.activationItemId().toString());
        assertEquals(DestinationPortalMode.BROKEN, config.destinationPortalMode());
        assertEquals(DEFAULT_TEMPLATE, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    private static ForeverWorldPortalsConfigLoader loaderFor(String text) {
        return new ForeverWorldPortalsConfigLoader(
                "test-defaults.properties",
                () -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))
        );
    }
}
