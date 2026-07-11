package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import net.pcal.fwportals.portal.persistence.ForeverWorldPortalRegistryData;
import net.pcal.fwportals.portal.persistence.OriginPortalRecord;
import net.pcal.fwportals.portal.persistence.PortalLookupKey;
import net.pcal.fwportals.portal.persistence.PortalReservation;
import net.pcal.fwportals.portal.persistence.ReservedDestinationRecord;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class PortalTravelService {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalDestinationSelector destinationSelector;
    private final SafeLandingFinder safeLandingFinder = new SafeLandingFinder();

    public PortalTravelService(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.destinationSelector = new PortalDestinationSelector(config, logger);
    }

    public @Nullable TeleportTransition getTeleportTransition(ServerLevel level, Entity entity, BlockPos portalEntryPos) {
        if (!level.getBlockState(portalEntryPos).is(Blocks.NETHER_PORTAL)) {
            return null;
        }

        Optional<ForeverWorldPortalFrame> maybeFrame = detector.findPortalFrame(level, portalEntryPos, config.frameBlock().defaultBlockState());
        if (maybeFrame.isEmpty()) {
            return null;
        }

        if (!(entity instanceof ServerPlayer player)) {
            logger.info("[fwportals] Suppressing Forever World portal travel for non-player entity {} at {}", entity.getName().getString(), portalEntryPos);
            return null;
        }

        ForeverWorldPortalFrame frame = maybeFrame.get();
        ForeverWorldPortalRegistryData registry = ForeverWorldPortalRegistryData.get(level);
        PortalLookupKey lookupKey = new PortalLookupKey(level.dimension(), frame.anchorPos());
        Optional<OriginPortalRecord> existingOrigin = registry.findOrigin(lookupKey);

        if (existingOrigin.isPresent()) {
            Optional<ReservedDestinationRecord> reservedDestination = registry.findReservedDestination(existingOrigin.get().linkedPortalId());
            if (reservedDestination.isEmpty()) {
                logger.warn(
                        "[fwportals] Existing origin portal {} is missing reserved destination {}",
                        existingOrigin.get().id(),
                        existingOrigin.get().linkedPortalId()
                );
                player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
                return null;
            }

            Optional<Vec3> safeLanding = safeLandingFinder.findSafeLanding(
                    level,
                    player,
                    reservedDestination.get().reservedDestinationPosition().getX(),
                    reservedDestination.get().reservedDestinationPosition().getZ()
            );
            if (safeLanding.isEmpty()) {
                logger.warn(
                        "[fwportals] Reserved destination {} for origin {} no longer has a safe landing spot",
                        reservedDestination.get().reservedDestinationPosition(),
                        existingOrigin.get().canonicalFrameAnchor()
                );
                player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
                return null;
            }

            logger.info(
                    "[fwportals] Reusing reserved destination {} for origin {}",
                    reservedDestination.get().reservedDestinationPosition(),
                    existingOrigin.get().canonicalFrameAnchor()
            );
            return buildTransition(level, safeLanding.get(), player);
        }

        Optional<PortalReservation> reservation = destinationSelector.selectDestination(level, player, frame.anchorPos(), registry);
        if (reservation.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not find a safe destination yet."));
            logger.warn(
                    "[fwportals] Failed to reserve destination for origin {} in {}",
                    frame.anchorPos(),
                    level.dimension().identifier()
            );
            return null;
        }

        UUID originId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        OriginPortalRecord originRecord = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                originId,
                level.dimension(),
                frame.anchorPos(),
                frame.axis(),
                frame.width(),
                frame.height(),
                portalEntryPos.immutable(),
                destinationId,
                level.getGameTime()
        );
        ReservedDestinationRecord reservedDestinationRecord = new ReservedDestinationRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                destinationId,
                reservation.get().dimension(),
                reservation.get().reservedDestinationPosition(),
                originId,
                false,
                level.getGameTime()
        );
        registry.registerLinkedPair(originRecord, reservedDestinationRecord);

        logger.info(
                "[fwportals] Registered new Forever World origin {} linked to reserved destination {}",
                originRecord.canonicalFrameAnchor(),
                reservedDestinationRecord.reservedDestinationPosition()
        );
        logger.info(
                "[fwportals] Reserved destination {} in {} for origin {}",
                reservedDestinationRecord.reservedDestinationPosition(),
                reservedDestinationRecord.dimension().identifier(),
                originRecord.canonicalFrameAnchor()
        );

        return buildTransition(level, reservation.get().landingPosition(), player);
    }

    public void clearRuntimeState() {
    }

    private TeleportTransition buildTransition(ServerLevel level, Vec3 targetPosition, ServerPlayer player) {
        logger.info(
                "[fwportals] Teleporting player {} to {} in {}",
                player.getName().getString(),
                BlockPos.containing(targetPosition),
                level.dimension().identifier()
        );
        return new TeleportTransition(
                level,
                targetPosition,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND
        );
    }
}
