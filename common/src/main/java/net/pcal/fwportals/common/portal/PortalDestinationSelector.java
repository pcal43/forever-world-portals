package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.pcal.fwportals.common.config.Config;
import net.pcal.fwportals.common.attunement.BiomeDestinationTarget;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PortalDestinationSelector {

    private final Config config;
    private final Logger logger;
    private final DestinationBiomeLocator destinationBiomeLocator;

    public PortalDestinationSelector(Config config, Logger logger) {
        this(config, logger, new DestinationBiomeLocator());
    }

    PortalDestinationSelector(
            Config config,
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
                new SpiralAnchorIterator(config.destinationSpiralSpacingBlocks()),
                deriveBiomeSearchRadiusBlocks(config.destinationSpiralSpacingBlocks()),
                new SearchBudget(config.maximumSpiralSearchPositions(), config.maximumBiomeSearches()),
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
        while (searchContext.searchBudget().canContinue()) {
            Optional<BlockPos> maybeBiomeAnchor = findNextBiomeSearchAnchor(
                    searchContext,
                    transientAnchor -> destinationBiomeLocator.findNearest(
                            searchContext.level(),
                            transientAnchor,
                            searchContext.biomeSearchRadiusBlocks(),
                            searchContext.destinationTarget()
                    ),
                    reason -> logger.debug("[fwportals] {}", reason),
                    searchContext.level().dimension().identifier().toString(),
                    searchContext.targetLabel()
            );
            if (maybeBiomeAnchor.isEmpty()) {
                continue;
            }

            BlockPos biomeAnchor = maybeBiomeAnchor.get();
            BlockPos candidate = candidateAnchorAt(searchContext.level(), biomeAnchor.getX(), biomeAnchor.getZ());
            if (candidate == null) {
                continue;
            }
            return Optional.of(new DestinationPortalCandidate(candidate));
        }
        logger.debug(
                "[fwportals] Destination search exhausted for {} in {} after {} spiral positions and {} biome searches: {}",
                searchContext.targetLabel(),
                searchContext.level().dimension().identifier(),
                searchContext.searchBudget().spiralPositionsExamined(),
                searchContext.searchBudget().biomeSearchesPerformed(),
                searchContext.searchBudget().exhaustionReason().orElse(ExhaustionReason.UNKNOWN)
        );
        return Optional.empty();
    }

    static Optional<BlockPos> findNextBiomeSearchAnchor(
            SearchContext searchContext,
            Function<BlockPos, Optional<BlockPos>> biomeLocator,
            java.util.function.Consumer<String> logSink,
            String dimensionId,
            String targetLabel
    ) {
        if (!searchContext.searchBudget().canContinue()) {
            return Optional.empty();
        }
        if (!searchContext.searchBudget().tryConsumeSpiralPosition()) {
            return Optional.empty();
        }

        BlockPos transientAnchor = searchContext.spiralAnchors().next();
        String transientRejection = firstRejectionReason(searchContext.constraints(), transientAnchor);
        if (transientRejection != null) {
            logSink.accept("Rejecting spiral anchor " + transientAnchor + " in " + dimensionId + " for " + targetLabel + " because " + transientRejection);
            return Optional.empty();
        }

        if (!searchContext.searchBudget().tryConsumeBiomeSearch()) {
            return Optional.empty();
        }

        Optional<BlockPos> maybeBiomeAnchor = biomeLocator.apply(transientAnchor);
        if (maybeBiomeAnchor.isEmpty()) {
            logSink.accept("No target biome found near spiral anchor " + transientAnchor + " in " + dimensionId + " for " + targetLabel);
            return Optional.empty();
        }

        BlockPos biomeAnchor = maybeBiomeAnchor.get();
        String biomeRejection = firstRejectionReason(searchContext.constraints(), biomeAnchor);
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

    static int deriveBiomeSearchRadiusBlocks(int spiralSpacingBlocks) {
        int radius = (int) Math.ceil(spiralSpacingBlocks / Math.sqrt(2.0));
        if (radius <= 0) {
            throw new IllegalArgumentException("Derived biome search radius must be positive for spacing " + spiralSpacingBlocks);
        }
        return radius;
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
            int biomeSearchRadiusBlocks,
            SearchBudget searchBudget,
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

    enum ExhaustionReason {
        MAXIMUM_SPIRAL_SEARCH_POSITIONS,
        MAXIMUM_BIOME_SEARCHES,
        UNKNOWN
    }

    static final class SearchBudget {
        private final int maximumSpiralSearchPositions;
        private final int maximumBiomeSearches;
        private int spiralPositionsExamined;
        private int biomeSearchesPerformed;
        private ExhaustionReason exhaustionReason;

        SearchBudget(int maximumSpiralSearchPositions, int maximumBiomeSearches) {
            this.maximumSpiralSearchPositions = maximumSpiralSearchPositions;
            this.maximumBiomeSearches = maximumBiomeSearches;
        }

        boolean tryConsumeSpiralPosition() {
            if (spiralPositionsExamined >= maximumSpiralSearchPositions) {
                exhaustionReason = ExhaustionReason.MAXIMUM_SPIRAL_SEARCH_POSITIONS;
                return false;
            }
            spiralPositionsExamined++;
            return true;
        }

        boolean tryConsumeBiomeSearch() {
            if (biomeSearchesPerformed >= maximumBiomeSearches) {
                exhaustionReason = ExhaustionReason.MAXIMUM_BIOME_SEARCHES;
                return false;
            }
            biomeSearchesPerformed++;
            return true;
        }

        boolean isExhausted() {
            return exhaustionReason != null;
        }

        boolean canContinue() {
            if (exhaustionReason != null) {
                return false;
            }
            if (spiralPositionsExamined >= maximumSpiralSearchPositions) {
                exhaustionReason = ExhaustionReason.MAXIMUM_SPIRAL_SEARCH_POSITIONS;
                return false;
            }
            if (biomeSearchesPerformed >= maximumBiomeSearches) {
                exhaustionReason = ExhaustionReason.MAXIMUM_BIOME_SEARCHES;
                return false;
            }
            return true;
        }

        int spiralPositionsExamined() {
            return spiralPositionsExamined;
        }

        int biomeSearchesPerformed() {
            return biomeSearchesPerformed;
        }

        Optional<ExhaustionReason> exhaustionReason() {
            return Optional.ofNullable(exhaustionReason);
        }
    }
}
