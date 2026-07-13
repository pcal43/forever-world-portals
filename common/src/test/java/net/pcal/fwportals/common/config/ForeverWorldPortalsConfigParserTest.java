package net.pcal.fwportals.common.config;

import net.minecraft.world.item.Items;
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
                        server.requireEmptyInventory=false
                        server.destinationPortalMode=COMPLETE
                        server.destinationSpiralSpacingBlocks=23456
                        server.maximumSpiralSearchPositions=123
                        server.maximumBiomeSearches=45
                        server.maximumPortalPlacementAttemptsPerBiome=67
                        server.minimumGeneratedTerrainDistanceBlocks=12345
                        """),
                defaults,
                null
        );

        assertEquals(false, config.requireEmptyInventory());
        assertEquals(CommonConfig.DestinationPortalMode.COMPLETE, config.destinationPortalMode());
        assertEquals(23456, config.destinationSpiralSpacingBlocks());
        assertEquals(123, config.maximumSpiralSearchPositions());
        assertEquals(45, config.maximumBiomeSearches());
        assertEquals(67, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(12345, config.minimumGeneratedTerrainDistanceBlocks());
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
                        server.requireEmptyInventory=false
                        server.maximumBiomeSearches=45
                        """),
                defaults,
                null
        );

        assertEquals(false, config.requireEmptyInventory());
        assertEquals(true, config.enabled());
        assertEquals(Level.INFO, config.logLevel());
        assertEquals(CommonConfig.DestinationPortalMode.BROKEN, config.destinationPortalMode());
        assertEquals(10000, config.destinationSpiralSpacingBlocks());
        assertEquals(512, config.maximumSpiralSearchPositions());
        assertEquals(45, config.maximumBiomeSearches());
        assertEquals(64, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(10000, config.minimumGeneratedTerrainDistanceBlocks());
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
                        server.enabled=maybe
                        server.requireEmptyInventory=not_boolean
                        server.logLevel=LOUD
                        server.frameBlock=not a block id
                        server.activationItem=minecraft:not_an_item
                        server.destinationPortalMode=glitched
                        server.destinationSpiralSpacingBlocks=0
                        server.maximumSpiralSearchPositions=-1
                        server.maximumBiomeSearches=zero
                        server.maximumPortalPlacementAttemptsPerBiome=-2
                        server.minimumGeneratedTerrainDistanceBlocks=-100
                        """),
                defaults,
                null
        );

        assertEquals(true, config.enabled());
        assertEquals(true, config.requireEmptyInventory());
        assertEquals(Level.INFO, config.logLevel());
        assertEquals(Blocks.DIAMOND_BLOCK, config.frameBlock());
        assertEquals(Items.FLINT_AND_STEEL, config.activationItem());
        assertEquals(CommonConfig.DestinationPortalMode.BROKEN, config.destinationPortalMode());
        assertEquals(10000, config.destinationSpiralSpacingBlocks());
        assertEquals(512, config.maximumSpiralSearchPositions());
        assertEquals(64, config.maximumBiomeSearches());
        assertEquals(64, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(10000, config.minimumGeneratedTerrainDistanceBlocks());
    }

    @Test
    void missingExpectedBundledDefaultKeyFailsClearly() throws IOException {
        TestBootstrap.ensureBootstrapped();
        Properties bundledDefaults = loadBundledDefaults();
        bundledDefaults.remove("server.maximumBiomeSearches");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> CommonConfigParser.parseBundledDefaults(
                        bundledDefaults,
                        "test-defaults.properties"
                )
        );

        assertTrue(exception.getMessage().contains("server.maximumBiomeSearches"));
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
