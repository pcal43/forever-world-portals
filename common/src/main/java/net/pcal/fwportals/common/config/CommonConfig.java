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
        int spiralSearchSpacing,
        int maxSpiralSearchPositions,
        int maxBiomeSearches,
        int maxPortalPlacementAttemptsPerBiome,
        int minGeneratedTerrainDistanceBlocks
) {
    public enum ReturnPortalMode {
        NONE,
        RUINED,
        COMPLETE
    }
}
