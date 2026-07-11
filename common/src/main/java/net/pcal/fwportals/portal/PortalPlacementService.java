package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PortalPlacementService {

    private static final int SEARCH_RADIUS = 12;

    private final Logger logger;
    private final SafeLandingFinder safeLandingFinder;
    private final PortalFrameDetector detector = new PortalFrameDetector();

    public PortalPlacementService(Logger logger, SafeLandingFinder safeLandingFinder) {
        this.logger = logger;
        this.safeLandingFinder = safeLandingFinder;
    }

    public Optional<GeneratedPortalPlacement> generateReturnPortal(
            ServerLevel level,
            ServerPlayer player,
            BlockPos targetPosition,
            ForeverWorldPortalFrame sourceFrame,
            BlockState frameState
    ) {
        for (Direction.Axis axis : axisOrder(sourceFrame.axis())) {
            for (BlockPos anchor : candidateAnchors(level, targetPosition)) {
                PortalLayout layout = PortalLayout.create(axis, anchor, sourceFrame.width(), sourceFrame.height());
                if (!canPlaceLayout(level, layout, frameState)) {
                    continue;
                }

                Optional<Vec3> arrivalPosition = findArrivalPosition(level, player, layout);
                if (arrivalPosition.isEmpty()) {
                    continue;
                }

                PlacementTransaction transaction = new PlacementTransaction(level);
                try {
                    placeLayout(transaction, layout, frameState);
                    Optional<ForeverWorldPortalFrame> generatedFrame = detector.findPortalFrame(
                            level,
                            layout.representativePortalPosition(),
                            axis,
                            frameState
                    );
                    if (generatedFrame.isEmpty()) {
                        transaction.rollback();
                        continue;
                    }

                    logger.info(
                            "[fwportals] Generated return portal at {} linked near {} in {}",
                            generatedFrame.get().anchorPos(),
                            targetPosition,
                            level.dimension().identifier()
                    );
                    return Optional.of(new GeneratedPortalPlacement(generatedFrame.get(), arrivalPosition.get()));
                } catch (RuntimeException ex) {
                    transaction.rollback();
                    throw ex;
                }
            }
        }

        logger.warn(
                "[fwportals] Failed to place return portal near {} in {}",
                targetPosition,
                level.dimension().identifier()
        );
        return Optional.empty();
    }

    public Optional<Vec3> findArrivalPosition(ServerLevel level, ServerPlayer player, ForeverWorldPortalFrame frame) {
        PortalLayout layout = PortalLayout.create(frame.axis(), frame.anchorPos(), frame.width(), frame.height());
        return findArrivalPosition(level, player, layout)
                .or(() -> safeLandingFinder.findSafeLanding(
                        level,
                        player,
                        frame.representativePortalPosition().getX(),
                        frame.representativePortalPosition().getZ()
                ));
    }

    private Iterable<BlockPos> candidateAnchors(ServerLevel level, BlockPos targetPosition) {
        java.util.List<BlockPos> anchors = new java.util.ArrayList<>();
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    BlockPos column = new BlockPos(targetPosition.getX() + dx, 0, targetPosition.getZ() + dz);
                    ensureChunkAvailable(level, column);
                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                    if (!level.getWorldBorder().isWithinBounds(surface) || surface.getY() <= level.getMinY() + 1) {
                        continue;
                    }

                    anchors.add(new BlockPos(column.getX(), surface.getY() - 1, column.getZ()));
                }
            }
        }
        return anchors;
    }

    private Direction.Axis[] axisOrder(Direction.Axis preferredAxis) {
        return preferredAxis == Direction.Axis.X
                ? new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}
                : new Direction.Axis[]{Direction.Axis.Z, Direction.Axis.X};
    }

    private boolean canPlaceLayout(ServerLevel level, PortalLayout layout, BlockState frameState) {
        int baseY = layout.anchorPos().getY();

        for (BlockPos pos : layout.interiorBlocks()) {
            if (!isReplaceableForPortal(level.getBlockState(pos))) {
                return false;
            }
        }
        for (BlockPos pos : layout.frameBlocks()) {
            BlockState state = level.getBlockState(pos);
            if (state.is(frameState.getBlock())) {
                continue;
            }

            if (pos.getY() == baseY) {
                if (!canUseAsFoundation(level, pos, state)) {
                    return false;
                }
                continue;
            }

            if (!isReplaceableForPortal(state) || isHazardous(state)) {
                return false;
            }
        }
        return true;
    }

    private boolean isReplaceableForPortal(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.FIRE);
    }

    private boolean canUseAsFoundation(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.canBeReplaced()) {
            return true;
        }
        return !isHazardous(state) && state.blocksMotion() && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private boolean isHazardous(BlockState state) {
        return !state.getFluidState().isEmpty()
                || state.is(Blocks.LAVA)
                || state.is(Blocks.WATER)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private void placeLayout(PlacementTransaction transaction, PortalLayout layout, BlockState frameState) {
        for (BlockPos framePos : layout.frameBlocks()) {
            transaction.setBlock(framePos, frameState, 18);
        }

        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                net.minecraft.world.level.block.NetherPortalBlock.AXIS,
                layout.axis()
        );
        for (BlockPos interiorPos : layout.interiorBlocks()) {
            transaction.setBlock(interiorPos, portalState, 18);
        }
    }

    private Optional<Vec3> findArrivalPosition(ServerLevel level, ServerPlayer player, PortalLayout layout) {
        for (Direction side : normalDirections(layout.axis())) {
            for (BlockPos interiorPos : orderedBottomInterior(layout)) {
                Optional<Vec3> standingPosition = safeLandingFinder.findSafeStandingAt(level, player, interiorPos.relative(side));
                if (standingPosition.isPresent()) {
                    return standingPosition;
                }
            }
        }
        return Optional.empty();
    }

    private java.util.List<BlockPos> orderedBottomInterior(PortalLayout layout) {
        return layout.interiorBlocks().stream()
                .filter(pos -> pos.getY() == layout.anchorPos().getY() + 1)
                .toList();
    }

    private Direction[] normalDirections(Direction.Axis axis) {
        return axis == Direction.Axis.X
                ? new Direction[]{Direction.NORTH, Direction.SOUTH}
                : new Direction[]{Direction.WEST, Direction.EAST};
    }

    private void ensureChunkAvailable(ServerLevel level, BlockPos probe) {
        ChunkPos chunkPos = ChunkPos.containing(probe);
        if (level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) == null) {
            level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, true);
        }
    }

    private static final class PlacementTransaction {
        private final ServerLevel level;
        private final Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();

        private PlacementTransaction(ServerLevel level) {
            this.level = level;
        }

        private void setBlock(BlockPos pos, BlockState state, int flags) {
            originalStates.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            level.setBlock(pos, state, flags);
        }

        private void rollback() {
            for (Map.Entry<BlockPos, BlockState> entry : originalStates.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue(), 18);
            }
        }
    }
}
