package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import net.pcal.fwportals.attunement.BiomeDestinationTarget;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PortalDestinationSelector {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final DestinationBiomeLocator destinationBiomeLocator;

    public PortalDestinationSelector(ForeverWorldPortalsConfig config, Logger logger) {
        this(config, logger, new DestinationBiomeLocator());
    }

    PortalDestinationSelector(
            ForeverWorldPortalsConfig config,
            Logger logger,
            DestinationBiomeLocator destinationBiomeLocator
    ) {
        this.config = config;
        this.logger = logger;
        this.destinationBiomeLocator = destinationBiomeLocator;
    }

    SearchContext beginSearch(
            ServerLevel level,
            BlockPos portalAnchor,
            BiomeDestinationTarget destinationTarget,
            String targetLabel
    ) {
        return new SearchContext(
                level,
                portalAnchor.immutable(),
                destinationTarget,
                targetLabel,
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
        Optional<BlockPos> maybeBiomeAnchor = findNextBiomeSearchAnchor(
                searchContext.spiralAnchors(),
                searchContext.constraints(),
                transientAnchor -> destinationBiomeLocator.findNearest(
                        searchContext.level(),
                        transientAnchor,
                        config.minimumGeneratedTerrainDistanceBlocks(),
                        searchContext.destinationTarget()
                ),
                reason -> logger.info("[fwportals] {}", reason),
                searchContext.level().dimension().identifier().toString(),
                searchContext.targetLabel()
        );
        if (maybeBiomeAnchor.isEmpty()) {
            return Optional.empty();
        }

        BlockPos biomeAnchor = maybeBiomeAnchor.get();
        BlockPos candidate = candidateAnchorAt(searchContext.level(), biomeAnchor.getX(), biomeAnchor.getZ());
        if (candidate == null) {
            return Optional.empty();
        }
        return Optional.of(new DestinationPortalCandidate(candidate));
    }

    static Optional<BlockPos> findNextBiomeSearchAnchor(
            SpiralAnchorIterator spiralAnchors,
            List<DestinationConstraint> constraints,
            Function<BlockPos, Optional<BlockPos>> biomeLocator,
            java.util.function.Consumer<String> logSink,
            String dimensionId,
            String targetLabel
    ) {
        BlockPos transientAnchor = spiralAnchors.next();
        String transientRejection = firstRejectionReason(constraints, transientAnchor);
        if (transientRejection != null) {
            logSink.accept("Rejecting spiral anchor " + transientAnchor + " in " + dimensionId + " for " + targetLabel + " because " + transientRejection);
            return Optional.empty();
        }

        Optional<BlockPos> maybeBiomeAnchor = biomeLocator.apply(transientAnchor);
        if (maybeBiomeAnchor.isEmpty()) {
            logSink.accept("No target biome found near spiral anchor " + transientAnchor + " in " + dimensionId + " for " + targetLabel);
            return Optional.empty();
        }

        BlockPos biomeAnchor = maybeBiomeAnchor.get();
        String biomeRejection = firstRejectionReason(constraints, biomeAnchor);
        if (biomeRejection != null) {
            logSink.accept("Rejecting biome-targeted anchor " + biomeAnchor + " from spiral anchor " + transientAnchor + " in " + dimensionId + " for " + targetLabel + " because " + biomeRejection);
            return Optional.empty();
        }

        return Optional.of(biomeAnchor.immutable());
    }

    private static String firstRejectionReason(List<DestinationConstraint> constraints, BlockPos anchor) {
        for (DestinationConstraint constraint : constraints) {
            String rejectionReason = constraint.rejectionReason(anchor);
            if (rejectionReason != null) {
                return rejectionReason;
            }
        }
        return null;
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
            BiomeDestinationTarget destinationTarget,
            String targetLabel,
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
