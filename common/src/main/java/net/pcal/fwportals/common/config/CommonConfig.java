package net.pcal.fwportals.common.config;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.Level;

public record CommonConfig(
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
    public enum DestinationPortalMode {
        NONE,
        BROKEN,
        COMPLETE
    }
}
