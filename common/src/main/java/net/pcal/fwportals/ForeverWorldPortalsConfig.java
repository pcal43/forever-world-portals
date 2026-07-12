package net.pcal.fwportals;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.Level;

public record ForeverWorldPortalsConfig(
        boolean enabled,
        boolean requireEmptyInventory,
        Level logLevel,
        Identifier frameBlockId,
        Block frameBlock,
        Identifier activationItemId,
        Item activationItem,
        ReturnPortalMode returnPortalMode,
        int destinationSpiralSpacingBlocks,
        int maximumSpiralSearchPositions,
        int maximumBiomeSearches,
        int maximumPortalPlacementAttemptsPerBiome,
        int minimumGeneratedTerrainDistanceBlocks
) {

    static ForeverWorldPortalsConfig defaults() {
        Block frameBlock = net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK;
        Item activationItem = net.minecraft.world.item.Items.FLINT_AND_STEEL;
        return new ForeverWorldPortalsConfig(
                true,
                true,
                Level.INFO,
                Identifier.parse("minecraft:diamond_block"),
                frameBlock,
                Identifier.parse("minecraft:flint_and_steel"),
                activationItem,
                ReturnPortalMode.GENERATE,
                10000,
                512,
                64,
                64,
                10000
        );
    }
}
