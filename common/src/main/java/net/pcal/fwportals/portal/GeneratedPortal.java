package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;

public record GeneratedPortal(
        BlockPos anchorBlock
) {
    public GeneratedPortal {
        anchorBlock = anchorBlock.immutable();
    }
}
