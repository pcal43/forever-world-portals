package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.BiConsumer;
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
            int width,
            int height,
            BlockState frameState
    ) {
        return findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                level.getWorldBorder()::isWithinBounds,
                requestedAnchor,
                width,
                height,
                frameState
        );
    }

    Optional<PortalLayout> findValidLayoutNearAnchor(
            BlockGetter level,
            int minY,
            Predicate<BlockPos> withinBounds,
            BlockPos requestedAnchor,
            int width,
            int height,
            BlockState frameState
    ) {
        for (BlockPos candidateAnchor : candidateAnchors(requestedAnchor)) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (axis == Direction.Axis.Y) {
                    continue;
                }

                PortalLayout layout = PortalLayout.createForAnchorBlock(axis, candidateAnchor, width, height);
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
        GeneratedPortal generatedPortal = new GeneratedPortal(layout.frame(), layout.anchorBlock());
        logger.info(
                "[fwportals] Generated portal at anchor {} with axis {} in {}",
                generatedPortal.anchorBlock(),
                generatedPortal.frame().axis(),
                level.dimension().identifier()
        );
        return generatedPortal;
    }

    static void applyLayout(PortalLayout layout, BlockState frameState, BiConsumer<BlockPos, BlockState> blockSetter) {
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

    private Iterable<BlockPos> candidateAnchors(BlockPos requestedAnchor) {
        java.util.List<BlockPos> anchors = new java.util.ArrayList<>();
        anchors.add(requestedAnchor);
        for (int radius = 1; radius <= ANCHOR_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    anchors.add(requestedAnchor.offset(dx, 0, dz));
                }
            }
        }
        return anchors;
    }

    private boolean canPlaceLayout(
            BlockGetter level,
            int minY,
            Predicate<BlockPos> withinBounds,
            PortalLayout layout,
            BlockState frameState
    ) {
        int frameBaseY = layout.frameBasePos().getY();

        for (BlockPos pos : layout.interiorBlocks()) {
            if (!withinBounds.test(pos) || !isReplaceableForPortal(level.getBlockState(pos))) {
                return false;
            }
        }
        for (BlockPos pos : layout.frameBlocks()) {
            if (!withinBounds.test(pos)) {
                return false;
            }

            BlockState state = level.getBlockState(pos);
            if (state.is(frameState.getBlock())) {
                continue;
            }

            if (pos.getY() == frameBaseY) {
                if (!canUseAsFoundation(level, pos, state)) {
                    return false;
                }
                continue;
            }

            if (!isReplaceableForPortal(state) || isHazardous(state)) {
                return false;
            }
        }

        return safeLandingFinder.validateGeneratedLandingSpot(level, minY, withinBounds, layout, frameState) == null;
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

    private boolean canUseAsFoundation(BlockGetter level, BlockPos pos, BlockState state) {
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
}
