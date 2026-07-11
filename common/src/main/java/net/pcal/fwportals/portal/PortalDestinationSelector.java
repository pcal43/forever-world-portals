package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import net.pcal.fwportals.portal.persistence.ForeverWorldPortalRegistryData;
import net.pcal.fwportals.portal.persistence.PortalReservation;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public final class PortalDestinationSelector {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final SafeLandingFinder safeLandingFinder;

    public PortalDestinationSelector(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.safeLandingFinder = new SafeLandingFinder(logger);
    }

    public Optional<PortalReservation> selectDestination(
            ServerLevel level,
            ServerPlayer player,
            BlockPos originAnchor,
            ForeverWorldPortalRegistryData registry
    ) {
        RandomSource random = RandomSource.create(level.getSeed() ^ originAnchor.asLong() ^ level.getGameTime());
        int minimumSeparation = config.minimumPortalSeparationBlocks();

        for (int attempt = 0; attempt < config.destinationSearchAttempts(); attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0);
            int extraDistance = random.nextInt(Math.max(1024, minimumSeparation));
            int distance = minimumSeparation + extraDistance;
            int x = originAnchor.getX() + (int)Math.round(Math.cos(angle) * distance);
            int z = originAnchor.getZ() + (int)Math.round(Math.sin(angle) * distance);
            BlockPos candidate = new BlockPos(x, originAnchor.getY(), z);

            if (!registry.isSeparated(level.dimension(), candidate, minimumSeparation)) {
                logger.info(
                        "[fwportals] Rejecting destination attempt {} at {} in {} because it is too close to an existing portal reservation",
                        attempt + 1,
                        candidate,
                        level.dimension().identifier()
                );
                continue;
            }

            Optional<Vec3> safeLanding = safeLandingFinder.findSafeLanding(level, player, x, z);
            if (safeLanding.isEmpty()) {
                continue;
            }

            BlockPos reservedPosition = BlockPos.containing(safeLanding.get());
            if (!registry.isSeparated(level.dimension(), reservedPosition, minimumSeparation)) {
                continue;
            }

            return Optional.of(new PortalReservation(level.dimension(), reservedPosition, safeLanding.get()));
        }

        logger.warn(
                "[fwportals] Failed to select destination after {} attempts for origin {} in {}",
                config.destinationSearchAttempts(),
                originAnchor,
                level.dimension().identifier()
        );
        return Optional.empty();
    }

    static long horizontalDistanceSquared(BlockPos a, BlockPos b) {
        long dx = (long)a.getX() - b.getX();
        long dz = (long)a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
