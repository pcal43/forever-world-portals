package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;

public record GeneratedPortal(
        ForeverWorldPortalFrame frame,
        BlockPos anchorBlock
) {
    public GeneratedPortal {
        anchorBlock = anchorBlock.immutable();
    }
}
