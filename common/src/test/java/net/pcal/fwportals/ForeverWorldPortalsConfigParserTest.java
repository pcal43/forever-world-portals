package net.pcal.fwportals;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForeverWorldPortalsConfigParserTest {

    @Test
    void parsesExplicitReturnPortalMode() throws IOException {
        TestBootstrap.ensureBootstrapped();
        ForeverWorldPortalsConfig config = ForeverWorldPortalsConfigParser.parse(
                new ByteArrayInputStream("""
                        requireEmptyInventory=false
                        returnPortalMode=NONE
                        server.destinationPortalMode=CoMpLeTe
                        destinationSpiralSpacingBlocks=23456
                        maximumSpiralSearchPositions=123
                        maximumBiomeSearches=45
                        maximumPortalPlacementAttemptsPerBiome=67
                        minimumGeneratedTerrainDistanceBlocks=12345
                        """.getBytes(StandardCharsets.UTF_8)),
                ForeverWorldPortalsConfig.defaults(),
                null
        );

        assertEquals(false, config.requireEmptyInventory());
        assertEquals(ReturnPortalMode.NONE, config.returnPortalMode());
        assertEquals(DestinationPortalMode.COMPLETE, config.destinationPortalMode());
        assertEquals(23456, config.destinationSpiralSpacingBlocks());
        assertEquals(123, config.maximumSpiralSearchPositions());
        assertEquals(45, config.maximumBiomeSearches());
        assertEquals(67, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(12345, config.minimumGeneratedTerrainDistanceBlocks());
    }

    @Test
    void fallsBackToDefaultsForMalformedValues() throws IOException {
        TestBootstrap.ensureBootstrapped();
        ForeverWorldPortalsConfig config = ForeverWorldPortalsConfigParser.parse(
                new ByteArrayInputStream("""
                        enabled=maybe
                        requireEmptyInventory=not_boolean
                        logLevel=LOUD
                        frameBlock=not a block id
                        activationItem=minecraft:not_an_item
                        returnPortalMode=NOT_A_REAL_MODE
                        server.destinationPortalMode=glitched
                        destinationSpiralSpacingBlocks=0
                        maximumSpiralSearchPositions=-1
                        maximumBiomeSearches=zero
                        maximumPortalPlacementAttemptsPerBiome=-2
                        minimumGeneratedTerrainDistanceBlocks=-100
                        """.getBytes(StandardCharsets.UTF_8)),
                ForeverWorldPortalsConfig.defaults(),
                null
        );

        assertEquals(true, config.enabled());
        assertEquals(true, config.requireEmptyInventory());
        assertEquals(Level.INFO, config.logLevel());
        assertEquals(Blocks.DIAMOND_BLOCK, config.frameBlock());
        assertEquals(Items.FLINT_AND_STEEL, config.activationItem());
        assertEquals(ReturnPortalMode.GENERATE, config.returnPortalMode());
        assertEquals(DestinationPortalMode.BROKEN, config.destinationPortalMode());
        assertEquals(10000, config.destinationSpiralSpacingBlocks());
        assertEquals(512, config.maximumSpiralSearchPositions());
        assertEquals(64, config.maximumBiomeSearches());
        assertEquals(64, config.maximumPortalPlacementAttemptsPerBiome());
        assertEquals(10000, config.minimumGeneratedTerrainDistanceBlocks());
    }
}
