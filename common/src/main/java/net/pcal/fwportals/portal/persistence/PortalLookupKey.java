package net.pcal.fwportals.portal.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PortalLookupKey(
        ResourceKey<Level> dimension,
        BlockPos canonicalFrameAnchor
) {
}
