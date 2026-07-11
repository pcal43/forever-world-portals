package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SafeLandingFinder {

    public Optional<Vec3> findSafeLanding(ServerLevel level, ServerPlayer player, int centerX, int centerZ) {
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    BlockPos probe = new BlockPos(centerX + dx, 0, centerZ + dz);
                    Optional<Vec3> candidate = findSafeLandingAt(level, player, probe);
                    if (candidate.isPresent()) {
                        return candidate;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Vec3> findSafeLandingAt(ServerLevel level, ServerPlayer player, BlockPos xzProbe) {
        BlockPos airPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xzProbe);
        if (!level.getWorldBorder().isWithinBounds(airPos) || airPos.getY() <= level.getMinY() + 1) {
            return Optional.empty();
        }

        BlockState standingState = level.getBlockState(airPos);
        BlockState belowState = level.getBlockState(airPos.below());
        if (!standingState.getFluidState().isEmpty() || !belowState.getFluidState().isEmpty() || belowState.is(Blocks.POWDER_SNOW)) {
            return Optional.empty();
        }

        Vec3 dismountLocation = DismountHelper.findSafeDismountLocation(player.getType(), level, airPos, true);
        if (dismountLocation == null) {
            return Optional.empty();
        }

        Vec3 collisionFreePosition = PortalShape.findCollisionFreePosition(dismountLocation, level, player, player.getDimensions(player.getPose()));
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(collisionFreePosition))) {
            return Optional.empty();
        }

        return Optional.of(collisionFreePosition);
    }
}
