package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
            SearchContext searchContext,
            int attempt
    ) {
        RandomSource random = RandomSource.create(
                searchContext.level().getSeed()
                        ^ searchContext.portalAnchor().asLong()
                        ^ searchContext.level().getGameTime()
                        ^ ((long) attempt * 0x9E3779B97F4A7C15L)
        );
        int minimumGeneratedTerrainDistanceBlocks = config.minimumGeneratedTerrainDistanceBlocks();

        double angle = random.nextDouble() * (Math.PI * 2.0);
        int extraDistance = random.nextInt(Math.max(1024, minimumGeneratedTerrainDistanceBlocks));
        int distance = minimumGeneratedTerrainDistanceBlocks + extraDistance;
        int x = searchContext.portalAnchor().getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = searchContext.portalAnchor().getZ() + (int) Math.round(Math.sin(angle) * distance);
        BlockPos candidate = candidateAnchorAt(searchContext.level(), x, z);
        if (candidate == null) {
            return Optional.empty();
        }

        for (DestinationConstraint constraint : searchContext.constraints()) {
            String rejectionReason = constraint.rejectionReason(candidate);
            if (rejectionReason != null) {
                logger.info(
                        "[fwportals] Rejecting candidate anchor {} in {} because {}",
                        candidate,
                        searchContext.level().dimension().identifier(),
                        rejectionReason
                );
                return Optional.empty();
            }
        }
        return Optional.of(new DestinationPortalCandidate(candidate));
    }

    static long horizontalDistanceSquared(BlockPos a, BlockPos b) {
        long dx = (long)a.getX() - b.getX();
        long dz = (long)a.getZ() - b.getZ();
        return dx * dx + dz * dz;
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

    public record SearchContext(ServerLevel level, BlockPos portalAnchor, List<DestinationConstraint> constraints) {
        public SearchContext {
            portalAnchor = portalAnchor.immutable();
            constraints = List.copyOf(constraints);
        }
    }
}
