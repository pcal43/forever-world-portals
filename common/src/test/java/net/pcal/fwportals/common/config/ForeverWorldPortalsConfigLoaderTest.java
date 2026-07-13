package net.pcal.fwportals.common.config;

import net.pcal.fwportals.common.TestBootstrap;
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
            server.enabled=true
            server.requireEmptyInventory=true
            server.logLevel=INFO
            server.frameBlock=minecraft:diamond_block
            server.activationItem=minecraft:flint_and_steel
            server.destinationPortalMode=BROKEN
            server.destinationSpiralSpacingBlocks=10000
            server.maximumSpiralSearchPositions=512
            server.maximumBiomeSearches=64
            server.maximumPortalPlacementAttemptsPerBiome=64
            server.minimumGeneratedTerrainDistanceBlocks=10000
            """;

    @Test
    void copiesBundledTemplateVerbatimWhenUserConfigIsMissing(@TempDir Path tempDir) throws IOException {
        CommonConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(CommonConfigLoader.CONFIG_FILENAME);

        loader.ensureUserConfigExists(configPath, null);

        assertEquals(DEFAULT_TEMPLATE, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    @Test
    void existingUserConfigIsNotRewrittenOrAugmented(@TempDir Path tempDir) throws IOException {
        CommonConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(CommonConfigLoader.CONFIG_FILENAME);
        Files.createDirectories(configPath.getParent());
        String partialConfig = """
                server.requireEmptyInventory=false
                server.maximumBiomeSearches=5
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
        CommonConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve(CommonConfigLoader.CONFIG_FILENAME);
        Files.writeString(configPath, """
                server.enabled=false
                server.requireEmptyInventory=false
                server.logLevel=DEBUG
                server.frameBlock=minecraft:emerald_block
                server.activationItem=minecraft:fire_charge
                server.destinationPortalMode=COMPLETE
                server.destinationSpiralSpacingBlocks=20000
                server.maximumSpiralSearchPositions=100
                server.maximumBiomeSearches=50
                server.maximumPortalPlacementAttemptsPerBiome=25
                server.minimumGeneratedTerrainDistanceBlocks=30000
                """, StandardCharsets.UTF_8);

        CommonConfig config = loader.load(configPath, null);

        assertFalse(config.enabled());
        assertFalse(config.requireEmptyInventory());
        assertEquals(org.apache.logging.log4j.Level.DEBUG, config.logLevel());
        assertEquals("minecraft:emerald_block", config.frameBlockId().toString());
        assertEquals("minecraft:fire_charge", config.activationItemId().toString());
        assertEquals(CommonConfig.DestinationPortalMode.COMPLETE, config.destinationPortalMode());
        assertEquals(20000, config.destinationSpiralSpacingBlocks());
        assertEquals(100, config.maximumSpiralSearchPositions());
        assertEquals(50, config.maximumBiomeSearches());
        assertEquals(25, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(30000, config.minimumGeneratedTerrainDistanceBlocks());
    }

    @Test
    void missingUserConfigReturnsParsedBundledDefaults(@TempDir Path tempDir) throws IOException {
        TestBootstrap.ensureBootstrapped();
        CommonConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(CommonConfigLoader.CONFIG_FILENAME);

        CommonConfig config = loader.load(configPath, null);

        assertEquals(true, config.enabled());
        assertEquals(true, config.requireEmptyInventory());
        assertEquals(org.apache.logging.log4j.Level.INFO, config.logLevel());
        assertEquals("minecraft:diamond_block", config.frameBlockId().toString());
        assertEquals("minecraft:flint_and_steel", config.activationItemId().toString());
        assertEquals(CommonConfig.DestinationPortalMode.BROKEN, config.destinationPortalMode());
        assertEquals(DEFAULT_TEMPLATE, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    private static CommonConfigLoader loaderFor(String text) {
        return new CommonConfigLoader(
                "test-defaults.properties",
                () -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))
        );
    }
}
