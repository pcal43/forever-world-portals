package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public record PortalLayout(
        Direction.Axis axis,
        BlockPos anchorPos,
        int width,
        int height,
        List<BlockPos> frameBlocks,
        List<BlockPos> interiorBlocks
) {

    public static PortalLayout create(Direction.Axis axis, BlockPos anchorPos, int width, int height) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos interiorOrigin = anchorPos.relative(Direction.UP).relative(rightDir);

        List<BlockPos> interiorBlocks = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int offset = 0; offset < width; offset++) {
                interiorBlocks.add(interiorOrigin.relative(Direction.UP, y).relative(rightDir, offset).immutable());
            }
        }

        List<BlockPos> frameBlocks = new ArrayList<>((width + 2) * 2 + height * 2);
        for (int offset = 0; offset < width + 2; offset++) {
            frameBlocks.add(anchorPos.relative(rightDir, offset).immutable());
            frameBlocks.add(anchorPos.relative(Direction.UP, height + 1).relative(rightDir, offset).immutable());
        }
        for (int y = 1; y <= height; y++) {
            frameBlocks.add(anchorPos.relative(Direction.UP, y).immutable());
            frameBlocks.add(anchorPos.relative(Direction.UP, y).relative(rightDir, width + 1).immutable());
        }

        return new PortalLayout(
                axis,
                anchorPos.immutable(),
                width,
                height,
                frameBlocks.stream().sorted(ForeverWorldPortalFrame.POSITION_COMPARATOR).distinct().toList(),
                interiorBlocks.stream().sorted(ForeverWorldPortalFrame.POSITION_COMPARATOR).toList()
        );
    }

    public BlockPos representativePortalPosition() {
        return interiorBlocks.get(0);
    }
}
