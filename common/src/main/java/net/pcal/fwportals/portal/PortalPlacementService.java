package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PortalPlacementService {

    private static final int ANCHOR_SEARCH_RADIUS = 8;

    private final Logger logger;
    private final SafeLandingFinder safeLandingFinder;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity portalIdentity = new PortalIdentity();

    public PortalPlacementService(Logger logger, SafeLandingFinder safeLandingFinder) {
        this.logger = logger;
        this.safeLandingFinder = safeLandingFinder;
    }

    public Optional<GeneratedPortal> tryGeneratePortalAtAnchor(
            ServerLevel level,
            ServerPlayer player,
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
                if (!layout.interiorBlocks().contains(candidateAnchor)) {
                    continue;
                }
                if (!canPlaceLayout(level, layout, frameState)) {
                    continue;
                }

                PlacementTransaction transaction = new PlacementTransaction(level);
                boolean committed = false;
                try {
                    placeLayout(transaction, layout, frameState);

                    Optional<ForeverWorldPortalFrame> detectedFrame = detector.findPortalFrame(
                            level,
                            candidateAnchor,
                            axis,
                            frameState
                    );
                    if (detectedFrame.isEmpty()) {
                        transaction.rollback();
                        continue;
                    }

                    ForeverWorldPortalFrame frame = detectedFrame.get();
                    BlockPos generatedPortalAnchor = portalIdentity.computeAnchorBlock(frame);
                    if (!generatedPortalAnchor.equals(candidateAnchor)) {
                        logger.warn(
                                "[fwportals] Rejecting generated portal at frame base {} in {} because computed anchor {} did not match candidate anchor {}",
                                frame.frameBasePos(),
                                level.dimension().identifier(),
                                generatedPortalAnchor,
                                candidateAnchor
                        );
                        transaction.rollback();
                        continue;
                    }

                    if (!safeLandingFinder.canArriveAtAnchor(level, player, generatedPortalAnchor)) {
                        transaction.rollback();
                        continue;
                    }

                    committed = true;
                    logger.info(
                            "[fwportals] Generated portal near requested anchor {} using actual anchor {} with axis {} in {}",
                            requestedAnchor,
                            generatedPortalAnchor,
                            frame.axis(),
                            level.dimension().identifier()
                    );
                    return Optional.of(new GeneratedPortal(frame, generatedPortalAnchor, transaction));
                } catch (RuntimeException ex) {
                    if (!committed) {
                        transaction.rollback();
                    }
                    throw ex;
                }
            }
        }

        logger.warn(
                "[fwportals] Failed to generate portal near requested anchor {} in {}",
                requestedAnchor,
                level.dimension().identifier()
        );
        return Optional.empty();
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

    private boolean canPlaceLayout(ServerLevel level, PortalLayout layout, BlockState frameState) {
        int frameBaseY = layout.frameBasePos().getY();

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

    private static final class PlacementTransaction implements PortalPlacementRollback {
        private final ServerLevel level;
        private final Map<BlockPos, Snapshot> snapshots = new LinkedHashMap<>();

        private PlacementTransaction(ServerLevel level) {
            this.level = level;
        }

        private void setBlock(BlockPos pos, BlockState state, int flags) {
            snapshots.putIfAbsent(pos.immutable(), Snapshot.capture(level, pos));
            level.setBlock(pos, state, flags);
        }

        @Override
        public void rollback() {
            for (Map.Entry<BlockPos, Snapshot> entry : snapshots.entrySet()) {
                BlockPos pos = entry.getKey();
                Snapshot snapshot = entry.getValue();
                level.setBlock(pos, snapshot.state(), 18);
                if (snapshot.blockEntityTag() != null) {
                    BlockEntity blockEntity = BlockEntity.loadStatic(pos, snapshot.state(), snapshot.blockEntityTag(), level.registryAccess());
                    if (blockEntity != null) {
                        level.setBlockEntity(blockEntity);
                    }
                } else {
                    level.removeBlockEntity(pos);
                }
            }
        }
    }

    private record Snapshot(BlockState state, net.minecraft.nbt.CompoundTag blockEntityTag) {
        private static Snapshot capture(ServerLevel level, BlockPos pos) {
            LevelChunk chunk = level.getChunkAt(pos);
            return new Snapshot(
                    level.getBlockState(pos),
                    chunk.getBlockEntityNbtForSaving(pos, level.registryAccess())
            );
        }
    }
}
