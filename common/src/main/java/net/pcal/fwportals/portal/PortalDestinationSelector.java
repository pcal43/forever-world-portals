package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public final class PortalDestinationSelector {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;

    public PortalDestinationSelector(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public SearchContext beginSearch(
            ServerLevel level,
            BlockPos portalAnchor
    ) {
        return new SearchContext(
                level,
                portalAnchor.immutable(),
                new SpiralAnchorIterator(config.minimumGeneratedTerrainDistanceBlocks()),
                List.of(
                        GeneratedTerrainDistanceConstraint.snapshot(
                                level,
                                config.minimumGeneratedTerrainDistanceBlocks(),
                                logger
                        )
                )
        );
    }

    public Optional<DestinationPortalCandidate> findCandidateAnchor(
            SearchContext searchContext
    ) {
        BlockPos transientAnchor = searchContext.spiralAnchors().next();
        for (DestinationConstraint constraint : searchContext.constraints()) {
            String rejectionReason = constraint.rejectionReason(transientAnchor);
            if (rejectionReason != null) {
                logger.info(
                        "[fwportals] Rejecting candidate anchor {} in {} because {}",
                        transientAnchor,
                        searchContext.level().dimension().identifier(),
                        rejectionReason
                );
                return Optional.empty();
            }
        }

        BlockPos candidate = candidateAnchorAt(searchContext.level(), transientAnchor.getX(), transientAnchor.getZ());
        if (candidate == null) {
            return Optional.empty();
        }
        return Optional.of(new DestinationPortalCandidate(candidate));
    }

    private BlockPos candidateAnchorAt(ServerLevel level, int x, int z) {
        BlockPos column = new BlockPos(x, 0, z);
        ensureChunkAvailable(level, column);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (!level.getWorldBorder().isWithinBounds(surface) || surface.getY() <= level.getMinY() + 1) {
            return null;
        }
        return new BlockPos(x, surface.getY(), z);
    }

    private void ensureChunkAvailable(ServerLevel level, BlockPos probe) {
        ChunkPos chunkPos = ChunkPos.containing(probe);
        if (level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) == null) {
            level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, true);
        }
    }

    public record SearchContext(
            ServerLevel level,
            BlockPos portalAnchor,
            SpiralAnchorIterator spiralAnchors,
            List<DestinationConstraint> constraints
    ) {
        public SearchContext {
            portalAnchor = portalAnchor.immutable();
            constraints = List.copyOf(constraints);
        }
    }

    static final class SpiralAnchorIterator {
        private final int spacingBlocks;
        private int orbit = 0;
        private int segment = 0;
        private int stepInSegment = 0;
        private boolean yieldedOrigin = false;

        SpiralAnchorIterator(int spacingBlocks) {
            this.spacingBlocks = spacingBlocks;
        }

        BlockPos next() {
            if (!yieldedOrigin) {
                yieldedOrigin = true;
                return new BlockPos(0, 0, 0);
            }

            int radius = orbit + 1;
            int step = stepInSegment;
            BlockPos result = switch (segment) {
                case 0 -> grid(-radius, step);
                case 1 -> grid(-radius + step + 1, radius);
                case 2 -> grid(radius, radius - step - 1);
                case 3 -> grid(radius - step - 1, -radius);
                default -> throw new IllegalStateException("Unexpected spiral segment " + segment);
            };

            stepInSegment++;
            int segmentLength = segment == 0 ? radius + 1 : 2 * radius;
            if (stepInSegment >= segmentLength) {
                stepInSegment = 0;
                segment++;
                if (segment >= 4) {
                    segment = 0;
                    orbit++;
                }
            }
            return result;
        }

        private BlockPos grid(int gridX, int gridZ) {
            return new BlockPos(gridX * spacingBlocks, 0, gridZ * spacingBlocks);
        }
    }
}
