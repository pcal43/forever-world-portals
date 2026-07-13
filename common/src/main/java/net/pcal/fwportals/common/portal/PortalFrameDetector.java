package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PortalFrameDetector {

    private static final int MIN_WIDTH = 2;
    private static final int MIN_HEIGHT = 3;

    public Optional<PortalFrame> findEmptyFrame(BlockGetter level, BlockPos pos, Direction.Axis preferredAxis, BlockState frameState) {
        return findFrame(level, pos, preferredAxis, frameState, true);
    }

    public Optional<PortalFrame> findPortalFrame(BlockGetter level, BlockPos pos, Direction.Axis preferredAxis, BlockState frameState) {
        return findFrame(level, pos, preferredAxis, frameState, false);
    }

    public Optional<PortalFrame> findPortalFrame(BlockGetter level, BlockPos pos, BlockState frameState) {
        BlockState state = level.getBlockState(pos);
        Direction.Axis preferredAxis = state.hasProperty(NetherPortalBlock.AXIS)
                ? state.getValue(NetherPortalBlock.AXIS)
                : Direction.Axis.X;
        return findFrame(level, pos, preferredAxis, frameState, false);
    }

    private Optional<PortalFrame> findFrame(BlockGetter level, BlockPos pos, Direction.Axis preferredAxis, BlockState frameState, boolean requireEmptyInterior) {
        Optional<PortalFrame> firstAxis = Optional.of(findAnyShape(level, pos, preferredAxis, frameState, requireEmptyInterior))
                .filter(frame -> frame.width() >= MIN_WIDTH && frame.height() >= MIN_HEIGHT);
        if (firstAxis.isPresent()) {
            return firstAxis;
        }

        Direction.Axis otherAxis = preferredAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(findAnyShape(level, pos, otherAxis, frameState, requireEmptyInterior))
                .filter(frame -> frame.width() >= MIN_WIDTH && frame.height() >= MIN_HEIGHT);
    }

    private PortalFrame findAnyShape(
            BlockGetter level,
            BlockPos pos,
            Direction.Axis axis,
            BlockState frameState,
            boolean requireEmptyInterior
    ) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = calculateBottomLeft(level, rightDir, pos, frameState);
        if (bottomLeft == null) {
            return emptyFrame(axis, pos);
        }

        int width = calculateWidth(level, bottomLeft, rightDir, frameState);
        if (width == 0) {
            return emptyFrame(axis, bottomLeft);
        }

        MutablePortalCount portalCount = new MutablePortalCount();
        int height = calculateHeight(level, bottomLeft, rightDir, width, frameState, requireEmptyInterior, portalCount);
        if (height < MIN_HEIGHT || height > PortalShape.MAX_WIDTH) {
            return emptyFrame(axis, bottomLeft);
        }

        List<BlockPos> interiorBlocks = buildInteriorBlocks(bottomLeft, rightDir, width, height);
        List<BlockPos> frameBlocks = buildFrameBlocks(bottomLeft, rightDir, width, height);
        BlockPos anchorPos = frameBlocks.stream().min(PortalFrame.POSITION_COMPARATOR).orElse(bottomLeft);
        return new PortalFrame(axis, anchorPos, bottomLeft, width, height, List.copyOf(frameBlocks), List.copyOf(interiorBlocks));
    }

    private PortalFrame emptyFrame(Direction.Axis axis, BlockPos origin) {
        return new PortalFrame(axis, origin, origin, 0, 0, List.of(), List.of());
    }

    private @Nullable BlockPos calculateBottomLeft(BlockGetter level, Direction rightDir, BlockPos pos, BlockState frameState) {
        int minY = Math.max(level.getMinY(), pos.getY() - PortalShape.MAX_WIDTH);
        while (pos.getY() > minY && isInteriorEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }

        Direction leftDir = rightDir.getOpposite();
        int edge = getDistanceUntilEdgeAboveFrame(level, pos, leftDir, frameState) - 1;
        return edge < 0 ? null : pos.relative(leftDir, edge);
    }

    private int calculateWidth(BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockState frameState) {
        int width = getDistanceUntilEdgeAboveFrame(level, bottomLeft, rightDir, frameState);
        return width >= MIN_WIDTH && width <= PortalShape.MAX_WIDTH ? width : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction, BlockState frameState) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int width = 0; width <= PortalShape.MAX_WIDTH; width++) {
            cursor.set(pos).move(direction, width);
            BlockState state = level.getBlockState(cursor);
            if (!isInteriorEmpty(state)) {
                if (state.is(frameState.getBlock())) {
                    return width;
                }
                break;
            }

            BlockState belowState = level.getBlockState(cursor.move(Direction.DOWN));
            if (!belowState.is(frameState.getBlock())) {
                break;
            }
        }
        return 0;
    }

    private int calculateHeight(
            BlockGetter level,
            BlockPos bottomLeft,
            Direction rightDir,
            int width,
            BlockState frameState,
            boolean requireEmptyInterior,
            MutablePortalCount portalCount
    ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int height = getDistanceUntilTop(level, bottomLeft, rightDir, cursor, width, frameState, requireEmptyInterior, portalCount);
        return height >= MIN_HEIGHT && height <= PortalShape.MAX_WIDTH && hasTopFrame(level, bottomLeft, rightDir, cursor, width, height, frameState)
                ? height
                : 0;
    }

    private boolean hasTopFrame(BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos cursor, int width, int height, BlockState frameState) {
        for (int i = 0; i < width; i++) {
            BlockPos.MutableBlockPos framePos = cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
            if (!level.getBlockState(framePos).is(frameState.getBlock())) {
                return false;
            }
        }
        return true;
    }

    private int getDistanceUntilTop(
            BlockGetter level,
            BlockPos bottomLeft,
            Direction rightDir,
            BlockPos.MutableBlockPos cursor,
            int width,
            BlockState frameState,
            boolean requireEmptyInterior,
            MutablePortalCount portalCount
    ) {
        for (int height = 0; height < PortalShape.MAX_WIDTH; height++) {
            cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, -1);
            if (!level.getBlockState(cursor).is(frameState.getBlock())) {
                return height;
            }

            cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, width);
            if (!level.getBlockState(cursor).is(frameState.getBlock())) {
                return height;
            }

            for (int i = 0; i < width; i++) {
                cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
                BlockState state = level.getBlockState(cursor);
                if (!isInteriorEmpty(state)) {
                    return height;
                }
                if (state.is(Blocks.NETHER_PORTAL)) {
                    portalCount.count++;
                    if (requireEmptyInterior) {
                        return height;
                    }
                }
            }
        }
        return PortalShape.MAX_WIDTH;
    }

    private List<BlockPos> buildInteriorBlocks(BlockPos bottomLeft, Direction rightDir, int width, int height) {
        List<BlockPos> blocks = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                blocks.add(bottomLeft.relative(Direction.UP, y).relative(rightDir, x).immutable());
            }
        }
        return blocks.stream().sorted(PortalFrame.POSITION_COMPARATOR).toList();
    }

    private List<BlockPos> buildFrameBlocks(BlockPos bottomLeft, Direction rightDir, int width, int height) {
        List<BlockPos> blocks = new ArrayList<>((width + 2) * 2 + height * 2);
        Direction leftDir = rightDir.getOpposite();
        BlockPos lowerLeftCorner = bottomLeft.relative(leftDir).below();
        BlockPos lowerRightCorner = bottomLeft.relative(rightDir, width).below();
        for (int x = 0; x < width + 2; x++) {
            blocks.add(lowerLeftCorner.relative(rightDir, x).immutable());
            blocks.add(lowerLeftCorner.relative(Direction.UP, height + 1).relative(rightDir, x).immutable());
        }
        for (int y = 1; y <= height; y++) {
            blocks.add(lowerLeftCorner.relative(Direction.UP, y).immutable());
            blocks.add(lowerRightCorner.relative(Direction.UP, y).immutable());
        }
        return blocks.stream().sorted(PortalFrame.POSITION_COMPARATOR).distinct().toList();
    }

    private boolean isInteriorEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(Blocks.NETHER_PORTAL);
    }

    private static final class MutablePortalCount {
        private int count;
    }
}
