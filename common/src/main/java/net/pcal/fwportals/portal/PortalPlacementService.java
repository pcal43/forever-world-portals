package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

public final class PortalPlacementService {

    private static final int ANCHOR_SEARCH_RADIUS = 8;

    private final Logger logger;
    private final SafeLandingFinder safeLandingFinder;

    public PortalPlacementService(Logger logger, SafeLandingFinder safeLandingFinder) {
        this.logger = logger;
        this.safeLandingFinder = safeLandingFinder;
    }

    public Optional<PortalLayout> findValidLayoutNearAnchor(
            ServerLevel level,
            BlockPos requestedAnchor,
            BlockState frameState
    ) {
        return findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                level.getWorldBorder()::isWithinBounds,
                (x, z) -> level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(x, 0, z)
                ).getY(),
                requestedAnchor,
                frameState
        );
    }

    Optional<PortalLayout> findValidLayoutNearAnchor(
            BlockGetter level,
            int minY,
            Predicate<BlockPos> withinBounds,
            IntBinaryOperator surfaceAirYAt,
            BlockPos requestedAnchor,
            BlockState frameState
    ) {
        for (BlockPos candidateColumn : candidateColumns(requestedAnchor)) {
            int surfaceAirY = surfaceAirYAt.applyAsInt(candidateColumn.getX(), candidateColumn.getZ());
            BlockPos candidateAnchor = new BlockPos(
                    candidateColumn.getX(),
                    surfaceAirY,
                    candidateColumn.getZ()
            );
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (axis == Direction.Axis.Y) {
                    continue;
                }

                PortalLayout layout = PortalLayout.createStandardForAnchorBlock(axis, candidateAnchor);
                if (!layout.anchorBlock().equals(candidateAnchor)) {
                    continue;
                }
                if (!canPlaceLayout(level, minY, withinBounds, layout, frameState)) {
                    continue;
                }
                return Optional.of(layout);
            }
        }
        return Optional.empty();
    }

    public GeneratedPortal placeValidatedLayout(ServerLevel level, PortalLayout layout, BlockState frameState) {
        applyLayout(layout, frameState, (pos, state) -> level.setBlock(pos, state, 18));
        GeneratedPortal generatedPortal = new GeneratedPortal(layout.anchorBlock());
        logger.info(
                "[fwportals] Generated portal at anchor {} with axis {} in {}",
                generatedPortal.anchorBlock(),
                layout.axis(),
                level.dimension().identifier()
        );
        return generatedPortal;
    }

    static void applyLayout(PortalLayout layout, BlockState frameState, BiConsumer<BlockPos, BlockState> blockSetter) {
        for (BlockPos clearancePos : layout.clearanceBlocks()) {
            blockSetter.accept(clearancePos, Blocks.AIR.defaultBlockState());
        }

        for (BlockPos framePos : layout.frameBlocks()) {
            blockSetter.accept(framePos, frameState);
        }

        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                net.minecraft.world.level.block.NetherPortalBlock.AXIS,
                layout.axis()
        );
        for (BlockPos interiorPos : layout.interiorBlocks()) {
            blockSetter.accept(interiorPos, portalState);
        }
    }

    private Iterable<BlockPos> candidateColumns(BlockPos requestedAnchor) {
        List<BlockPos> columns = new ArrayList<>();
        columns.add(new BlockPos(requestedAnchor.getX(), 0, requestedAnchor.getZ()));
        for (int radius = 1; radius <= ANCHOR_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    columns.add(new BlockPos(requestedAnchor.getX() + dx, 0, requestedAnchor.getZ() + dz));
                }
            }
        }
        return columns;
    }

    private boolean canPlaceLayout(
            BlockGetter level,
            int minY,
            Predicate<BlockPos> withinBounds,
            PortalLayout layout,
            BlockState frameState
    ) {
        for (BlockPos pos : layout.foundationBlocks()) {
            if (!withinBounds.test(pos) || !isSafeFoundationBlock(level, pos)) {
                return false;
            }
        }

        for (BlockPos pos : layout.clearanceBlocks()) {
            if (!withinBounds.test(pos) || !isClearableVolumeBlock(level, pos)) {
                return false;
            }
        }

        return safeLandingFinder.validateGeneratedLandingSpot(level, minY, withinBounds, layout, frameState) == null;
    }

    private boolean isClearableVolumeBlock(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.hasBlockEntity()
                && !isPortalState(state)
                && !isHazardous(state)
                && (state.isAir() || state.canBeReplaced());
    }

    private boolean isSafeFoundationBlock(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && !state.canBeReplaced()
                && !state.hasBlockEntity()
                && !isPortalState(state)
                && !isHazardous(state)
                && !(state.getBlock() instanceof FallingBlock)
                && state.blocksMotion()
                && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private boolean isHazardous(BlockState state) {
        return !state.getFluidState().isEmpty()
                || state.is(Blocks.LAVA)
                || state.is(Blocks.WATER)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private boolean isPortalState(BlockState state) {
        return state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY);
    }
}
