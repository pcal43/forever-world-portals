package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;

public final class PortalIdentity {

    public BlockPos computeAnchorBlock(PortalFrame frame) {
        if (frame.interiorBlocks().isEmpty()) {
            throw new IllegalArgumentException("Portal frame has no interior blocks");
        }

        int lowestInteriorY = frame.interiorBlocks().stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElseThrow();

        java.util.List<BlockPos> lowestRow = frame.interiorBlocks().stream()
                .filter(pos -> pos.getY() == lowestInteriorY)
                .sorted((left, right) -> switch (frame.axis()) {
                    case X -> Integer.compare(left.getX(), right.getX());
                    case Z -> Integer.compare(left.getZ(), right.getZ());
                    default -> throw new IllegalStateException("Unexpected axis: " + frame.axis());
                })
                .toList();

        if (lowestRow.isEmpty()) {
            throw new IllegalArgumentException("Portal frame has no lowest interior row");
        }

        return lowestRow.get((lowestRow.size() - 1) / 2);
    }

    public boolean portalEnclosesAnchor(PortalFrame frame, BlockPos anchorBlock) {
        return frame.containsInterior(anchorBlock);
    }
}
