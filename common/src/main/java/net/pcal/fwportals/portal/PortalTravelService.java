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
import net.pcal.fwportals.ReturnPortalMode;
import net.pcal.fwportals.portal.persistence.ForeverWorldPortalRegistryData;
import net.pcal.fwportals.portal.persistence.OriginPortalRecord;
import net.pcal.fwportals.portal.persistence.PortalLookupKey;
import net.pcal.fwportals.portal.persistence.PortalReservation;
import net.pcal.fwportals.portal.persistence.ReservedDestinationRecord;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public final class PortalTravelService {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalDestinationSelector destinationSelector;
    private final SafeLandingFinder safeLandingFinder;
    private final PortalPlacementService portalPlacementService;
    private final Set<PortalLookupKey> inProgressPortals = new HashSet<>();

    public PortalTravelService(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.destinationSelector = new PortalDestinationSelector(config, logger);
        this.safeLandingFinder = new SafeLandingFinder(logger);
        this.portalPlacementService = new PortalPlacementService(logger, safeLandingFinder);
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
            return handleOriginPortalTravel(level, player, frame, existingOrigin.get(), registry, lookupKey);
        }

        Optional<ReservedDestinationRecord> generatedDestination = registry.findReservedDestinationByAnchor(lookupKey);
        if (generatedDestination.isPresent()) {
            return handleGeneratedDestinationTravel(level, player, generatedDestination.get(), registry);
        }

        if (config.returnPortalMode() != ReturnPortalMode.GENERATE) {
            return failForUnimplementedReturnMode(player, lookupKey);
        }

        if (!inProgressPortals.add(lookupKey)) {
            player.sendSystemMessage(Component.literal("Forever World portal linking is already in progress."));
            logger.warn("[fwportals] Portal founding already in progress for {}", lookupKey.canonicalFrameAnchor());
            return null;
        }

        try {
            return handleFoundingTravel(level, player, portalEntryPos, frame, registry, lookupKey);
        } finally {
            inProgressPortals.remove(lookupKey);
        }
    }

    public void clearRuntimeState() {
        inProgressPortals.clear();
    }

    private @Nullable TeleportTransition handleOriginPortalTravel(
            ServerLevel level,
            ServerPlayer player,
            ForeverWorldPortalFrame sourceFrame,
            OriginPortalRecord originRecord,
            ForeverWorldPortalRegistryData registry,
            PortalLookupKey lookupKey
    ) {
        Optional<ReservedDestinationRecord> reservedDestination = registry.findReservedDestination(originRecord.linkedPortalId());
        if (reservedDestination.isEmpty()) {
            logger.warn(
                    "[fwportals] Existing origin portal {} is missing reserved destination {}",
                    originRecord.id(),
                    originRecord.linkedPortalId()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        ReservedDestinationRecord destination = reservedDestination.get();
        if (!destination.physicalPortalExists()) {
            if (config.returnPortalMode() != ReturnPortalMode.GENERATE) {
                return failForUnimplementedReturnMode(player, lookupKey);
            }
            if (!inProgressPortals.add(lookupKey)) {
                player.sendSystemMessage(Component.literal("Forever World portal linking is already in progress."));
                logger.warn("[fwportals] Destination materialization already in progress for {}", lookupKey.canonicalFrameAnchor());
                return null;
            }
            try {
                return materializeReturnPortalAndTeleport(level, player, sourceFrame, destination, registry, lookupKey);
            } finally {
                inProgressPortals.remove(lookupKey);
            }
        }

        Optional<ForeverWorldPortalFrame> destinationFrame = resolveDestinationFrame(level, destination);
        if (destinationFrame.isEmpty()) {
            logger.warn(
                    "[fwportals] Linked destination portal {} for origin {} is missing or invalid",
                    destination.reservedDestinationPosition(),
                    originRecord.canonicalFrameAnchor()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        Optional<Vec3> arrival = portalPlacementService.findArrivalPosition(level, player, destinationFrame.get());
        if (arrival.isEmpty()) {
            logger.warn(
                    "[fwportals] Linked destination portal {} for origin {} has no safe arrival",
                    destination.canonicalFrameAnchor(),
                    originRecord.canonicalFrameAnchor()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        logger.info(
                "[fwportals] Reusing linked destination portal {} for origin {}",
                destination.canonicalFrameAnchor(),
                originRecord.canonicalFrameAnchor()
        );
        return buildTransition(level, arrival.get(), player);
    }

    private @Nullable TeleportTransition handleGeneratedDestinationTravel(
            ServerLevel level,
            ServerPlayer player,
            ReservedDestinationRecord destinationRecord,
            ForeverWorldPortalRegistryData registry
    ) {
        Optional<OriginPortalRecord> originRecord = registry.findOriginById(destinationRecord.linkedOriginId());
        if (originRecord.isEmpty()) {
            logger.warn(
                    "[fwportals] Generated destination portal {} is missing linked origin {}",
                    destinationRecord.canonicalFrameAnchor(),
                    destinationRecord.linkedOriginId()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        Optional<ForeverWorldPortalFrame> originFrame = resolveOriginFrame(level, originRecord.get());
        if (originFrame.isEmpty()) {
            logger.warn(
                    "[fwportals] Linked origin portal {} for destination {} is missing or invalid",
                    originRecord.get().canonicalFrameAnchor(),
                    destinationRecord.canonicalFrameAnchor()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        Optional<Vec3> arrival = portalPlacementService.findArrivalPosition(level, player, originFrame.get());
        if (arrival.isEmpty()) {
            logger.warn(
                    "[fwportals] Linked origin portal {} for destination {} has no safe arrival",
                    originRecord.get().canonicalFrameAnchor(),
                    destinationRecord.canonicalFrameAnchor()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        logger.info(
                "[fwportals] Reusing linked origin portal {} for generated destination {}",
                originRecord.get().canonicalFrameAnchor(),
                destinationRecord.canonicalFrameAnchor()
        );
        return buildTransition(level, arrival.get(), player);
    }

    private @Nullable TeleportTransition handleFoundingTravel(
            ServerLevel level,
            ServerPlayer player,
            BlockPos portalEntryPos,
            ForeverWorldPortalFrame sourceFrame,
            ForeverWorldPortalRegistryData registry,
            PortalLookupKey lookupKey
    ) {
        Optional<PortalReservation> reservation = destinationSelector.selectDestination(level, player, sourceFrame.anchorPos(), registry);
        if (reservation.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not find a safe destination yet."));
            logger.warn(
                    "[fwportals] Failed to reserve destination for origin {} in {}",
                    sourceFrame.anchorPos(),
                    level.dimension().identifier()
            );
            return null;
        }

        Optional<GeneratedPortalPlacement> generatedPortal = portalPlacementService.generateReturnPortal(
                level,
                player,
                reservation.get().reservedDestinationPosition(),
                sourceFrame,
                config.frameBlock().defaultBlockState()
        );
        if (generatedPortal.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not create a return portal yet."));
            logger.warn(
                    "[fwportals] Failed to generate return portal near reserved destination {} for origin {}",
                    reservation.get().reservedDestinationPosition(),
                    lookupKey.canonicalFrameAnchor()
            );
            return null;
        }

        UUID originId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        OriginPortalRecord originRecord = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                originId,
                level.dimension(),
                sourceFrame.anchorPos(),
                sourceFrame.axis(),
                sourceFrame.width(),
                sourceFrame.height(),
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
                true,
                generatedPortal.get().frame().anchorPos(),
                generatedPortal.get().frame().axis(),
                generatedPortal.get().frame().width(),
                generatedPortal.get().frame().height(),
                generatedPortal.get().frame().representativePortalPosition(),
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
        logger.info(
                "[fwportals] Generated linked return portal {} for origin {}",
                reservedDestinationRecord.canonicalFrameAnchor(),
                originRecord.canonicalFrameAnchor()
        );

        return buildTransition(level, generatedPortal.get().arrivalPosition(), player);
    }

    private @Nullable TeleportTransition materializeReturnPortalAndTeleport(
            ServerLevel level,
            ServerPlayer player,
            ForeverWorldPortalFrame sourceFrame,
            ReservedDestinationRecord destinationRecord,
            ForeverWorldPortalRegistryData registry,
            PortalLookupKey lookupKey
    ) {
        Optional<GeneratedPortalPlacement> generatedPortal = portalPlacementService.generateReturnPortal(
                level,
                player,
                destinationRecord.reservedDestinationPosition(),
                sourceFrame,
                config.frameBlock().defaultBlockState()
        );
        if (generatedPortal.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not create a return portal yet."));
            logger.warn(
                    "[fwportals] Failed to materialize return portal near {} for origin {}",
                    destinationRecord.reservedDestinationPosition(),
                    lookupKey.canonicalFrameAnchor()
            );
            return null;
        }

        ReservedDestinationRecord materialized = destinationRecord.withPhysicalPortal(
                generatedPortal.get().frame().anchorPos(),
                generatedPortal.get().frame().axis(),
                generatedPortal.get().frame().width(),
                generatedPortal.get().frame().height(),
                generatedPortal.get().frame().representativePortalPosition()
        );
        registry.materializeReservedDestination(destinationRecord.id(), materialized);

        logger.info(
                "[fwportals] Materialized linked return portal {} for origin {}",
                materialized.canonicalFrameAnchor(),
                lookupKey.canonicalFrameAnchor()
        );
        return buildTransition(level, generatedPortal.get().arrivalPosition(), player);
    }

    private Optional<ForeverWorldPortalFrame> resolveOriginFrame(ServerLevel level, OriginPortalRecord originRecord) {
        return detector.findPortalFrame(
                level,
                originRecord.representativePortalPosition(),
                originRecord.axis(),
                config.frameBlock().defaultBlockState()
        );
    }

    private Optional<ForeverWorldPortalFrame> resolveDestinationFrame(ServerLevel level, ReservedDestinationRecord destinationRecord) {
        if (!destinationRecord.physicalPortalExists()
                || destinationRecord.axis() == null
                || destinationRecord.representativePortalPosition() == null) {
            return Optional.empty();
        }
        return detector.findPortalFrame(
                level,
                destinationRecord.representativePortalPosition(),
                destinationRecord.axis(),
                config.frameBlock().defaultBlockState()
        );
    }

    private @Nullable TeleportTransition failForUnimplementedReturnMode(ServerPlayer player, PortalLookupKey lookupKey) {
        player.sendSystemMessage(Component.literal(
                "Forever World return portal mode " + config.returnPortalMode() + " is not implemented yet."
        ));
        logger.warn(
                "[fwportals] Return portal mode {} is not implemented for portal {}",
                config.returnPortalMode(),
                lookupKey.canonicalFrameAnchor()
        );
        return null;
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
