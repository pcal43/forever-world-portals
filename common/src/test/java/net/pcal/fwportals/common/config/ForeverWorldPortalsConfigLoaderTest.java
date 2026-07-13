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
            requireEmptyInventory=true
            logLevel=INFO
            portalFrameBlock=minecraft:diamond_block
            returnPortalMode=RUINED
            spiralSearchSpacing=10000
            maxSpiralSearchPositions=512
            maxBiomeSearches=64
            maxPortalPlacementAttemptsPerBiome=64
            minGeneratedTerrainDistanceBlocks=10000
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
                requireEmptyInventory=false
                maxBiomeSearches=5
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
                requireEmptyInventory=false
                logLevel=DEBUG
                portalFrameBlock=minecraft:emerald_block
                returnPortalMode=COMPLETE
                spiralSearchSpacing=20000
                maxSpiralSearchPositions=100
                maxBiomeSearches=50
                maxPortalPlacementAttemptsPerBiome=25
                minGeneratedTerrainDistanceBlocks=30000
                """, StandardCharsets.UTF_8);

        CommonConfig config = loader.load(configPath, null);

        assertFalse(config.requireEmptyInventory());
        assertEquals(org.apache.logging.log4j.Level.DEBUG, config.logLevel());
        assertEquals("minecraft:emerald_block", config.portalFrameBlockId().toString());
        assertEquals(CommonConfig.ReturnPortalMode.COMPLETE, config.returnPortalMode());
        assertEquals(20000, config.spiralSearchSpacing());
        assertEquals(100, config.maxSpiralSearchPositions());
        assertEquals(50, config.maxBiomeSearches());
        assertEquals(25, config.maxPortalPlacementAttemptsPerBiome());
        assertEquals(30000, config.minGeneratedTerrainDistanceBlocks());
    }

    @Test
    void missingUserConfigReturnsParsedBundledDefaults(@TempDir Path tempDir) throws IOException {
        TestBootstrap.ensureBootstrapped();
        CommonConfigLoader loader = loaderFor(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve(CommonConfigLoader.CONFIG_FILENAME);

        CommonConfig config = loader.load(configPath, null);

        assertEquals(true, config.requireEmptyInventory());
        assertEquals(org.apache.logging.log4j.Level.INFO, config.logLevel());
        assertEquals("minecraft:diamond_block", config.portalFrameBlockId().toString());
        assertEquals(CommonConfig.ReturnPortalMode.RUINED, config.returnPortalMode());
        assertEquals(DEFAULT_TEMPLATE, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    private static CommonConfigLoader loaderFor(String text) {
        return new CommonConfigLoader(
                "test-defaults.properties",
                () -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))
        );
    }
}
