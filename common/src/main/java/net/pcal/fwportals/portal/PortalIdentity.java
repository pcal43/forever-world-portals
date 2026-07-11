package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;

import java.util.Comparator;

public final class PortalIdentity {

    private static final Comparator<BlockPos> NEGATIVE_TIE_BREAK = Comparator
            .comparingInt((BlockPos pos) -> pos.getX())
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ);

    public BlockPos computeOriginBlock(ForeverWorldPortalFrame frame) {
        if (frame.interiorBlocks().isEmpty()) {
            throw new IllegalArgumentException("Portal frame has no interior blocks");
        }

        double centerX = 0.0;
        double centerY = 0.0;
        double centerZ = 0.0;
        for (BlockPos pos : frame.interiorBlocks()) {
            centerX += pos.getX() + 0.5;
            centerY += pos.getY() + 0.5;
            centerZ += pos.getZ() + 0.5;
        }

        int count = frame.interiorBlocks().size();
        centerX /= count;
        centerY /= count;
        centerZ /= count;
        final double finalCenterX = centerX;
        final double finalCenterY = centerY;
        final double finalCenterZ = centerZ;

        return frame.interiorBlocks().stream()
                .min(Comparator
                        .comparingDouble((BlockPos pos) -> squaredDistanceToCenter(pos, finalCenterX, finalCenterY, finalCenterZ))
                        .thenComparing(NEGATIVE_TIE_BREAK))
                .orElseThrow();
    }

    public boolean portalEnclosesOrigin(ForeverWorldPortalFrame frame, BlockPos originBlock) {
        return frame.containsInterior(originBlock);
    }

    private double squaredDistanceToCenter(BlockPos pos, double centerX, double centerY, double centerZ) {
        double dx = (pos.getX() + 0.5) - centerX;
        double dy = (pos.getY() + 0.5) - centerY;
        double dz = (pos.getZ() + 0.5) - centerZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
