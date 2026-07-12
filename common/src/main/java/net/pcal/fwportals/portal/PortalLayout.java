package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public record PortalLayout(
        Direction.Axis axis,
        BlockPos frameBasePos,
        int width,
        int height,
        List<BlockPos> frameBlocks,
        List<BlockPos> interiorBlocks
) {

    public static final int STANDARD_INTERIOR_WIDTH = 2;
    public static final int STANDARD_INTERIOR_HEIGHT = 3;
    public static final int STANDARD_FRAME_WIDTH = STANDARD_INTERIOR_WIDTH + 2;
    public static final int STANDARD_FRAME_HEIGHT = STANDARD_INTERIOR_HEIGHT + 2;
    public static final int STANDARD_DEPTH = 3;

    public static PortalLayout create(Direction.Axis axis, BlockPos anchorPos, int width, int height) {
        Direction rightDir = horizontalDirection(axis);
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

    public static PortalLayout createStandardForAnchorBlock(Direction.Axis axis, BlockPos anchorBlock) {
        return createForAnchorBlock(axis, anchorBlock, STANDARD_INTERIOR_WIDTH, STANDARD_INTERIOR_HEIGHT);
    }

    public static PortalLayout createForAnchorBlock(Direction.Axis axis, BlockPos anchorBlock, int width, int height) {
        Direction horizontalDirection = horizontalDirection(axis);
        int horizontalOffset = (width - 1) / 2;
        BlockPos interiorOrigin = anchorBlock.relative(horizontalDirection, -horizontalOffset);
        BlockPos frameBasePos = interiorOrigin.relative(horizontalDirection, -1).below();
        return create(axis, frameBasePos, width, height);
    }

    public BlockPos interiorOrigin() {
        Direction rightDir = horizontalDirection(axis);
        return frameBasePos.relative(Direction.UP).relative(rightDir);
    }

    public BlockPos anchorBlock() {
        Direction rightDir = horizontalDirection(axis);
        int horizontalOffset = (width - 1) / 2;
        return interiorOrigin().relative(rightDir, horizontalOffset).immutable();
    }

    public Direction horizontalDirection() {
        return horizontalDirection(axis);
    }

    public Direction depthDirection() {
        return axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
    }

    public BlockPos foundationOrigin() {
        return frameBasePos.relative(depthDirection(), -1);
    }

    public List<BlockPos> foundationBlocks() {
        List<BlockPos> blocks = new ArrayList<>(STANDARD_FRAME_WIDTH * STANDARD_DEPTH);
        BlockPos origin = foundationOrigin();
        Direction horizontalDirection = horizontalDirection();
        Direction depthDirection = depthDirection();
        for (int depthOffset = 0; depthOffset < STANDARD_DEPTH; depthOffset++) {
            for (int widthOffset = 0; widthOffset < STANDARD_FRAME_WIDTH; widthOffset++) {
                blocks.add(origin.relative(horizontalDirection, widthOffset).relative(depthDirection, depthOffset).immutable());
            }
        }
        return blocks;
    }

    public List<BlockPos> clearanceBlocks() {
        List<BlockPos> blocks = new ArrayList<>(STANDARD_FRAME_WIDTH * STANDARD_DEPTH * STANDARD_FRAME_HEIGHT);
        BlockPos origin = foundationOrigin().above();
        Direction horizontalDirection = horizontalDirection();
        Direction depthDirection = depthDirection();
        for (int yOffset = 0; yOffset < STANDARD_FRAME_HEIGHT; yOffset++) {
            for (int depthOffset = 0; depthOffset < STANDARD_DEPTH; depthOffset++) {
                for (int widthOffset = 0; widthOffset < STANDARD_FRAME_WIDTH; widthOffset++) {
                    blocks.add(origin.relative(horizontalDirection, widthOffset)
                            .relative(depthDirection, depthOffset)
                            .relative(Direction.UP, yOffset)
                            .immutable());
                }
            }
        }
        return blocks;
    }

    public List<BlockPos> subfoundationBlocks() {
        List<BlockPos> blocks = new ArrayList<>(STANDARD_FRAME_WIDTH);
        Direction horizontalDirection = horizontalDirection();
        for (int widthOffset = 0; widthOffset < STANDARD_FRAME_WIDTH; widthOffset++) {
            blocks.add(frameBasePos.relative(horizontalDirection, widthOffset).below().immutable());
        }
        return blocks;
    }

    public ForeverWorldPortalFrame frame() {
        return new ForeverWorldPortalFrame(
                axis,
                frameBasePos,
                interiorOrigin(),
                width,
                height,
                frameBlocks,
                interiorBlocks
        );
    }

    public BlockPos representativePortalPosition() {
        return interiorBlocks.get(0);
    }

    private static Direction horizontalDirection(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }
}
