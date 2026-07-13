package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface DestinationConstraint {

    @Nullable String rejectionReason(BlockPos candidateAnchor);
}
