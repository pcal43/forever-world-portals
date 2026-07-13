package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;

public record DestinationPortalCandidate(BlockPos requestedAnchor) {
    public DestinationPortalCandidate {
        requestedAnchor = requestedAnchor.immutable();
    }
}
