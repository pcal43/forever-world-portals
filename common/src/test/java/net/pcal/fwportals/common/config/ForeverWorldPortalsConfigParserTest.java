package net.pcal.fwportals.common.config;

import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.common.TestBootstrap;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeverWorldPortalsConfigParserTest {

    @Test
    void parsesExplicitOverridesAgainstBundledDefaults() throws IOException {
        TestBootstrap.ensureBootstrapped();
        CommonConfig defaults = CommonConfigParser.parseBundledDefaults(
                loadBundledDefaults(),
                CommonConfigLoader.DEFAULT_CONFIG_RESOURCE_NAME
        );
        CommonConfig config = CommonConfigParser.parse(
                properties("""
                        requireEmptyInventory=false
                        returnPortalMode=COMPLETE
                        spiralSearchSpacing=23456
                        maxSpiralSearchPositions=123
                        maxBiomeSearches=45
                        maxPortalPlacementAttemptsPerBiome=67
                        minGeneratedTerrainDistanceBlocks=12345
                        """),
                defaults,
                null
        );

        assertEquals(false, config.requireEmptyInventory());
        assertEquals(CommonConfig.ReturnPortalMode.COMPLETE, config.returnPortalMode());
        assertEquals(23456, config.spiralSearchSpacing());
        assertEquals(123, config.maxSpiralSearchPositions());
        assertEquals(45, config.maxBiomeSearches());
        assertEquals(67, config.maxPortalPlacementAttemptsPerBiome());
        assertEquals(12345, config.minGeneratedTerrainDistanceBlocks());
    }

    @Test
    void partialUserPropertiesUseBundledDefaultsForOmittedKeys() throws IOException {
        TestBootstrap.ensureBootstrapped();
        CommonConfig defaults = CommonConfigParser.parseBundledDefaults(
                loadBundledDefaults(),
                CommonConfigLoader.DEFAULT_CONFIG_RESOURCE_NAME
        );
        CommonConfig config = CommonConfigParser.parse(
                properties("""
                        requireEmptyInventory=false
                        maxBiomeSearches=45
                        """),
                defaults,
                null
        );

        assertEquals(false, config.requireEmptyInventory());
        assertEquals(Level.INFO, config.logLevel());
        assertEquals(CommonConfig.ReturnPortalMode.RUINED, config.returnPortalMode());
        assertEquals(10000, config.spiralSearchSpacing());
        assertEquals(512, config.maxSpiralSearchPositions());
        assertEquals(45, config.maxBiomeSearches());
        assertEquals(64, config.maxPortalPlacementAttemptsPerBiome());
        assertEquals(10000, config.minGeneratedTerrainDistanceBlocks());
    }

    @Test
    void fallsBackToBundledDefaultsForMalformedUserValues() throws IOException {
        TestBootstrap.ensureBootstrapped();
        CommonConfig defaults = CommonConfigParser.parseBundledDefaults(
                loadBundledDefaults(),
                CommonConfigLoader.DEFAULT_CONFIG_RESOURCE_NAME
        );
        CommonConfig config = CommonConfigParser.parse(
                properties("""
                        requireEmptyInventory=not_boolean
                        logLevel=LOUD
                        portalFrameBlock=not a block id
                        returnPortalMode=glitched
                        spiralSearchSpacing=0
                        maxSpiralSearchPositions=-1
                        maxBiomeSearches=zero
                        maxPortalPlacementAttemptsPerBiome=-2
                        minGeneratedTerrainDistanceBlocks=-100
                        """),
                defaults,
                null
        );

        assertEquals(true, config.requireEmptyInventory());
        assertEquals(Level.INFO, config.logLevel());
        assertEquals(Blocks.DIAMOND_BLOCK, config.portalFrameBlock());
        assertEquals(CommonConfig.ReturnPortalMode.RUINED, config.returnPortalMode());
        assertEquals(10000, config.spiralSearchSpacing());
        assertEquals(512, config.maxSpiralSearchPositions());
        assertEquals(64, config.maxBiomeSearches());
        assertEquals(64, config.maxPortalPlacementAttemptsPerBiome());
        assertEquals(10000, config.minGeneratedTerrainDistanceBlocks());
    }

    @Test
    void missingExpectedBundledDefaultKeyFailsClearly() throws IOException {
        TestBootstrap.ensureBootstrapped();
        Properties bundledDefaults = loadBundledDefaults();
        bundledDefaults.remove("maxBiomeSearches");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> CommonConfigParser.parseBundledDefaults(
                        bundledDefaults,
                        "test-defaults.properties"
                )
        );

        assertTrue(exception.getMessage().contains("maxBiomeSearches"));
        assertTrue(exception.getMessage().contains("test-defaults.properties"));
    }

    @Test
    void bundledResourceContainsEveryExpectedKey() throws IOException {
        Properties bundledDefaults = loadBundledDefaults();
        for (String key : CommonConfigParser.expectedKeys()) {
            assertTrue(bundledDefaults.containsKey(key), () -> "Missing bundled default key " + key);
        }
    }

    private static Properties loadBundledDefaults() throws IOException {
        return CommonConfigLoader.createDefault().loadBundledDefaultProperties();
    }

    private static Properties properties(String text) throws IOException {
        CommonConfigLoader loader = new CommonConfigLoader(
                "test.properties",
                () -> new java.io.ByteArrayInputStream(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        return loader.loadBundledDefaultProperties();
    }
}
