package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;

public record GeneratedPortal(
        BlockPos anchorBlock
) {
    public GeneratedPortal {
        anchorBlock = anchorBlock.immutable();
    }
}
