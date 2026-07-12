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
                        minimumGeneratedTerrainDistanceBlocks=12345
                        """.getBytes(StandardCharsets.UTF_8)),
                ForeverWorldPortalsConfig.defaults(),
                null
        );

        assertEquals(false, config.requireEmptyInventory());
        assertEquals(ReturnPortalMode.NONE, config.returnPortalMode());
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
                        destinationSearchAttempts=zero
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
        assertEquals(64, config.destinationSearchAttempts());
        assertEquals(10000, config.minimumGeneratedTerrainDistanceBlocks());
    }
}
