package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class SafeLandingFinder {

    public SafeLandingFinder() {
    }

    @Nullable FailureReason validateGeneratedLandingSpot(
            BlockGetter level,
            int minY,
            Predicate<BlockPos> withinBounds,
            PortalLayout layout,
            BlockState frameState
    ) {
        BlockPos feetPos = layout.anchorBlock();
        if (!withinBounds.test(feetPos) || feetPos.getY() <= minY + 1) {
            return FailureReason.OUT_OF_BOUNDS;
        }

        BlockPos headPos = feetPos.above();
        BlockPos floorPos = feetPos.below();
        if (!withinBounds.test(headPos) || !withinBounds.test(floorPos)) {
            return FailureReason.OUT_OF_BOUNDS;
        }

        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                net.minecraft.world.level.block.NetherPortalBlock.AXIS,
                layout.axis()
        );
        BlockState feetState = projectedState(level, layout, frameState, portalState, feetPos);
        BlockState headState = projectedState(level, layout, frameState, portalState, headPos);
        BlockState floorState = projectedState(level, layout, frameState, portalState, floorPos);

        if (!floorState.blocksMotion() || !floorState.isFaceSturdy(level, floorPos, Direction.UP)) {
            return FailureReason.BLOCKED;
        }

        if (isHazardous(feetState) || isHazardous(headState) || isHazardous(floorState)) {
            return FailureReason.HAZARD;
        }

        if (!feetState.getCollisionShape(level, feetPos).isEmpty() || !headState.getCollisionShape(level, headPos).isEmpty()) {
            return FailureReason.BLOCKED;
        }

        return null;
    }

    private BlockState projectedState(
            BlockGetter level,
            PortalLayout layout,
            BlockState frameState,
            BlockState portalState,
            BlockPos pos
    ) {
        if (layout.frameBlocks().contains(pos)) {
            return frameState;
        }
        if (layout.interiorBlocks().contains(pos)) {
            return portalState;
        }
        return level.getBlockState(pos);
    }

    private boolean isHazardous(BlockState state) {
        return !state.getFluidState().isEmpty()
                || state.is(Blocks.LAVA)
                || state.is(Blocks.WATER)
                || state.is(Blocks.POWDER_SNOW);
    }

    private enum FailureReason {
        OUT_OF_BOUNDS,
        HAZARD,
        BLOCKED
    }
}
