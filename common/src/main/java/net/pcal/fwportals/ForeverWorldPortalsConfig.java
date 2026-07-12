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
        DestinationPortalMode destinationPortalMode,
        int destinationSpiralSpacingBlocks,
        int maximumSpiralSearchPositions,
        int maximumBiomeSearches,
        int maximumPortalPlacementAttemptsPerBiome,
        int minimumGeneratedTerrainDistanceBlocks
) {
}
