package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Comparator;
import java.util.List;

public record PortalFrame(
        Direction.Axis axis,
        BlockPos frameBasePos,
        BlockPos interiorOrigin,
        int width,
        int height,
        List<BlockPos> frameBlocks,
        List<BlockPos> interiorBlocks
) {

    static final Comparator<BlockPos> POSITION_COMPARATOR = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());

    public boolean containsInterior(BlockPos pos) {
        return interiorBlocks.contains(pos);
    }

    public BlockPos representativePortalPosition() {
        return interiorBlocks.get(0);
    }
}
