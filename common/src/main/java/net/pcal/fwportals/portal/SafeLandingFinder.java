package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class SafeLandingFinder {

    private final Logger logger;

    public SafeLandingFinder(Logger logger) {
        this.logger = logger;
    }

    public Optional<Vec3> findSafeLanding(ServerLevel level, ServerPlayer player, int centerX, int centerZ) {
        int probeCount = 0;
        int outOfBoundsCount = 0;
        int hazardCount = 0;
        int blockedCount = 0;
        int collisionCount = 0;
        boolean generatedAnyChunks = false;

        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    BlockPos probe = new BlockPos(centerX + dx, 0, centerZ + dz);
                    if (ensureChunkAvailable(level, probe)) {
                        generatedAnyChunks = true;
                    }
                    ProbeResult result = findSafeLandingAt(level, player, probe);
                    probeCount++;
                    if (result.landingPosition() != null) {
                        logger.info(
                                "[fwportals] Found safe landing {} near target ({}, {}) in {} after {} probes",
                                BlockPos.containing(result.landingPosition()),
                                centerX,
                                centerZ,
                                level.dimension().identifier(),
                                probeCount
                        );
                        return Optional.of(result.landingPosition());
                    }

                    switch (result.failureReason()) {
                        case OUT_OF_BOUNDS -> outOfBoundsCount++;
                        case HAZARD -> hazardCount++;
                        case BLOCKED -> blockedCount++;
                        case COLLISION -> collisionCount++;
                    }
                }
            }
        }

        logger.info(
                "[fwportals] No safe landing found near target ({}, {}) in {} after {} probes: generatedChunks={} outOfBounds={} hazard={} blocked={} collision={}",
                centerX,
                centerZ,
                level.dimension().identifier(),
                probeCount,
                generatedAnyChunks,
                outOfBoundsCount,
                hazardCount,
                blockedCount,
                collisionCount
        );
        return Optional.empty();
    }

    public Optional<Vec3> findSafeStandingAt(ServerLevel level, ServerPlayer player, BlockPos feetPos) {
        FailureReason validation = validateManualLandingSpot(level, feetPos);
        if (validation != null) {
            return Optional.empty();
        }

        Vec3 standingPosition = Vec3.atBottomCenterOf(feetPos);
        Vec3 collisionFreePosition = PortalShape.findCollisionFreePosition(
                standingPosition,
                level,
                player,
                player.getDimensions(player.getPose())
        );
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(collisionFreePosition))) {
            return Optional.empty();
        }
        return Optional.of(collisionFreePosition);
    }

    private boolean ensureChunkAvailable(ServerLevel level, BlockPos probe) {
        ChunkPos chunkPos = ChunkPos.containing(probe);
        if (level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) != null) {
            return false;
        }

        logger.info(
                "[fwportals] Generating chunk {} for destination search in {}",
                chunkPos,
                level.dimension().identifier()
        );
        level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, true);
        return true;
    }

    private ProbeResult findSafeLandingAt(ServerLevel level, ServerPlayer player, BlockPos xzProbe) {
        BlockPos airPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xzProbe);
        if (!level.getWorldBorder().isWithinBounds(airPos) || airPos.getY() <= level.getMinY() + 1) {
            return ProbeResult.failure(FailureReason.OUT_OF_BOUNDS);
        }

        BlockState standingState = level.getBlockState(airPos);
        BlockState belowState = level.getBlockState(airPos.below());
        if (!standingState.getFluidState().isEmpty() || !belowState.getFluidState().isEmpty() || belowState.is(Blocks.POWDER_SNOW)) {
            return ProbeResult.failure(FailureReason.HAZARD);
        }

        Vec3 dismountLocation = DismountHelper.findSafeDismountLocation(player.getType(), level, airPos, true);
        if (dismountLocation != null) {
            Vec3 collisionFreePosition = PortalShape.findCollisionFreePosition(dismountLocation, level, player, player.getDimensions(player.getPose()));
            if (level.getWorldBorder().isWithinBounds(BlockPos.containing(collisionFreePosition))) {
                return ProbeResult.success(collisionFreePosition);
            }
        }

        for (int y = airPos.getY() - 2; y <= airPos.getY() + 4; y++) {
            BlockPos feetPos = new BlockPos(xzProbe.getX(), y, xzProbe.getZ());
            FailureReason manualCheck = validateManualLandingSpot(level, feetPos);
            if (manualCheck != null) {
                continue;
            }

            Optional<Vec3> standingPosition = findSafeStandingAt(level, player, feetPos);
            if (standingPosition.isPresent()) {
                return ProbeResult.success(standingPosition.get());
            }
        }

        if (!belowState.blocksMotion()) {
            return ProbeResult.failure(FailureReason.BLOCKED);
        }
        return ProbeResult.failure(FailureReason.COLLISION);
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
        BLOCKED,
        COLLISION
    }

    private record ProbeResult(@Nullable Vec3 landingPosition, FailureReason failureReason) {

        private static ProbeResult success(Vec3 landingPosition) {
            return new ProbeResult(landingPosition, FailureReason.COLLISION);
        }

        private static ProbeResult failure(FailureReason reason) {
            return new ProbeResult(null, reason);
        }
    }
}
