package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;

public record DestinationPortalCandidate(BlockPos requestedAnchor) {
    public DestinationPortalCandidate {
        requestedAnchor = requestedAnchor.immutable();
    }
}
