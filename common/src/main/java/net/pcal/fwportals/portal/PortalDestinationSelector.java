package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import net.pcal.fwportals.portal.persistence.ForeverWorldPortalRegistryData;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public final class PortalDestinationSelector {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;

    public PortalDestinationSelector(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public Optional<DestinationPortalCandidate> findCandidateAnchor(
            ServerLevel level,
            BlockPos portalAnchor,
            ForeverWorldPortalRegistryData registry,
            int attempt
    ) {
        RandomSource random = RandomSource.create(
                level.getSeed()
                        ^ portalAnchor.asLong()
                        ^ level.getGameTime()
                        ^ ((long) attempt * 0x9E3779B97F4A7C15L)
        );
        int minimumSeparation = config.minimumPortalSeparationBlocks();

        double angle = random.nextDouble() * (Math.PI * 2.0);
        int extraDistance = random.nextInt(Math.max(1024, minimumSeparation));
        int distance = minimumSeparation + extraDistance;
        int x = portalAnchor.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = portalAnchor.getZ() + (int) Math.round(Math.sin(angle) * distance);
        BlockPos candidate = candidateAnchorAt(level, x, z);
        if (candidate == null) {
            return Optional.empty();
        }
        if (!registry.isSeparated(level.dimension(), candidate, minimumSeparation)) {
            logger.info(
                    "[fwportals] Rejecting candidate anchor {} in {} because it is too close to an existing registered source anchor",
                    candidate,
                    level.dimension().identifier()
            );
            return Optional.empty();
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
}
