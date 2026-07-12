package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SafeLandingFinder {

    public SafeLandingFinder() {
    }

    public boolean canArriveAtAnchor(ServerLevel level, ServerPlayer player, BlockPos anchorBlock) {
        return validateManualLandingSpot(level, anchorBlock) == null
                && level.getWorldBorder().isWithinBounds(anchorBlock);
    }

    private @Nullable FailureReason validateManualLandingSpot(ServerLevel level, BlockPos feetPos) {
        if (!level.getWorldBorder().isWithinBounds(feetPos) || feetPos.getY() <= level.getMinY() + 1) {
            return FailureReason.OUT_OF_BOUNDS;
        }

        BlockPos headPos = feetPos.above();
        BlockPos floorPos = feetPos.below();
        BlockState feetState = level.getBlockState(feetPos);
        BlockState headState = level.getBlockState(headPos);
        BlockState floorState = level.getBlockState(floorPos);

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
