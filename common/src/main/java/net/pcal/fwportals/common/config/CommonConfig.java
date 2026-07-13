package net.pcal.fwportals.common.config;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.Level;

public record CommonConfig(
        boolean requireEmptyInventory,
        Level logLevel,
        Identifier portalFrameBlockId,
        Block portalFrameBlock,
        ReturnPortalMode returnPortalMode,
        int destinationSpiralSpacingBlocks,
        int maximumSpiralSearchPositions,
        int maximumBiomeSearches,
        int maximumPortalPlacementAttemptsPerBiome,
        int minimumGeneratedTerrainDistanceBlocks
) {
    public enum ReturnPortalMode {
        NONE,
        RUINED,
        COMPLETE
    }
}
