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
import net.pcal.fwportals.portal.persistence.PortalReservation;
import net.pcal.fwportals.portal.persistence.SourcePortalRecord;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PortalTravelService {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalDestinationSelector destinationSelector;
    private final SafeLandingFinder safeLandingFinder;
    private final PortalPlacementService portalPlacementService;
    private final PortalIdentity portalIdentity = new PortalIdentity();
    private final Set<SourcePortalKey> inProgressPortals = new HashSet<>();

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
        List<SourcePortalRecord> matches = registry.findSourcePortalsContainedBy(level.dimension(), frame);
        if (!matches.isEmpty()) {
            return handleRegisteredPortalTravel(level, player, matches);
        }

        if (config.returnPortalMode() != ReturnPortalMode.GENERATE) {
            return failForUnimplementedReturnMode(player, frame);
        }

        BlockPos originBlock = portalIdentity.computeOriginBlock(frame);
        SourcePortalKey inProgressKey = new SourcePortalKey(level.dimension(), originBlock);
        if (!inProgressPortals.add(inProgressKey)) {
            player.sendSystemMessage(Component.literal("Forever World portal founding is already in progress."));
            logger.warn("[fwportals] Portal founding already in progress for origin block {}", originBlock);
            return null;
        }

        try {
            return handleFoundingTravel(level, player, frame, registry, originBlock);
        } finally {
            inProgressPortals.remove(inProgressKey);
        }
    }

    public void clearRuntimeState() {
        inProgressPortals.clear();
    }

    private @Nullable TeleportTransition handleRegisteredPortalTravel(ServerLevel level, ServerPlayer player, List<SourcePortalRecord> matches) {
        SourcePortalRecord selected = matches.get(0);
        if (matches.size() > 1) {
            logger.warn(
                    "[fwportals] Multiple source portals matched one physical portal in {}: {}. Using first deterministic match {}",
                    level.dimension().identifier(),
                    matches.stream().map(SourcePortalRecord::portalId).toList(),
                    selected.portalId()
            );
        }

        ServerLevel destinationLevel = level.getServer().getLevel(selected.destinationDimension());
        if (destinationLevel == null) {
            logger.warn(
                    "[fwportals] Destination dimension {} is unavailable for source portal {}",
                    selected.destinationDimension().identifier(),
                    selected.portalId()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        return buildTransition(destinationLevel, Vec3.atBottomCenterOf(selected.destinationPosition()), player);
    }

    private @Nullable TeleportTransition handleFoundingTravel(
            ServerLevel level,
            ServerPlayer player,
            ForeverWorldPortalFrame sourceFrame,
            ForeverWorldPortalRegistryData registry,
            BlockPos sourceOriginBlock
    ) {
        Optional<PortalReservation> reservation = destinationSelector.selectDestination(level, player, sourceFrame.anchorPos(), registry);
        if (reservation.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not find a safe destination yet."));
            logger.warn(
                    "[fwportals] Failed to reserve destination for source origin {} in {}",
                    sourceOriginBlock,
                    level.dimension().identifier()
            );
            return null;
        }

        BlockPos destinationPosition = BlockPos.containing(reservation.get().landingPosition());
        SourcePortalRecord outboundPortal = new SourcePortalRecord(
                UUID.randomUUID(),
                level.dimension(),
                sourceOriginBlock.immutable(),
                reservation.get().dimension(),
                destinationPosition
        );

        ServerLevel destinationLevel = level.getServer().getLevel(reservation.get().dimension());
        if (destinationLevel == null) {
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            logger.warn(
                    "[fwportals] Destination dimension {} is unavailable for source origin {}",
                    reservation.get().dimension().identifier(),
                    sourceOriginBlock
            );
            return null;
        }

        Optional<GeneratedPortalPlacement> generatedPortal = portalPlacementService.generateReturnPortal(
                destinationLevel,
                player,
                reservation.get().reservedDestinationPosition(),
                sourceFrame,
                config.frameBlock().defaultBlockState()
        );
        if (generatedPortal.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not create a return portal yet."));
            logger.warn(
                    "[fwportals] Failed to generate return portal near {} for source origin {}",
                    reservation.get().reservedDestinationPosition(),
                    sourceOriginBlock
            );
            return null;
        }

        List<SourcePortalRecord> destinationMatches = registry.findSourcePortalsContainedBy(destinationLevel.dimension(), generatedPortal.get().frame());
        if (!destinationMatches.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World return portal location is already claimed."));
            logger.warn(
                    "[fwportals] Generated destination portal at {} encloses existing source portal ids {}",
                    generatedPortal.get().frame().anchorPos(),
                    destinationMatches.stream().map(SourcePortalRecord::portalId).toList()
            );
            return null;
        }

        Optional<Vec3> reverseArrival = portalPlacementService.findArrivalPosition(level, player, sourceFrame);
        if (reverseArrival.isEmpty()) {
            player.sendSystemMessage(Component.literal("Forever World portal could not determine a safe return destination."));
            logger.warn(
                    "[fwportals] Failed to determine safe reverse arrival near source origin {}",
                    sourceOriginBlock
            );
            return null;
        }

        BlockPos generatedOriginBlock = portalIdentity.computeOriginBlock(generatedPortal.get().frame());
        SourcePortalKey generatedKey = new SourcePortalKey(reservation.get().dimension(), generatedOriginBlock);
        if (!inProgressPortals.add(generatedKey)) {
            player.sendSystemMessage(Component.literal("Forever World return portal founding is already in progress."));
            logger.warn("[fwportals] Reverse route founding already in progress for origin block {}", generatedOriginBlock);
            return null;
        }

        try {
            SourcePortalRecord reversePortal = new SourcePortalRecord(
                    UUID.randomUUID(),
                    reservation.get().dimension(),
                    generatedOriginBlock.immutable(),
                    level.dimension(),
                    BlockPos.containing(reverseArrival.get())
            );

            registry.createSourcePortal(outboundPortal);
            registry.createSourcePortal(reversePortal);

            logger.info(
                    "[fwportals] Registered new source portal {} in {} -> {} in {}",
                    outboundPortal.originBlock(),
                    outboundPortal.dimension().identifier(),
                    outboundPortal.destinationPosition(),
                    outboundPortal.destinationDimension().identifier()
            );
            logger.info(
                    "[fwportals] Registered complementary source portal {} in {} -> {} in {}",
                    reversePortal.originBlock(),
                    reversePortal.dimension().identifier(),
                    reversePortal.destinationPosition(),
                    reversePortal.destinationDimension().identifier()
            );

            return buildTransition(destinationLevel, generatedPortal.get().arrivalPosition(), player);
        } finally {
            inProgressPortals.remove(generatedKey);
        }
    }

    private @Nullable TeleportTransition failForUnimplementedReturnMode(ServerPlayer player, ForeverWorldPortalFrame frame) {
        player.sendSystemMessage(Component.literal(
                "Forever World return portal mode " + config.returnPortalMode() + " is not implemented yet."
        ));
        logger.warn(
                "[fwportals] Return portal mode {} is not implemented for portal anchor {}",
                config.returnPortalMode(),
                frame.anchorPos()
        );
        return null;
    }

    private TeleportTransition buildTransition(ServerLevel destinationLevel, Vec3 targetPosition, ServerPlayer player) {
        logger.info(
                "[fwportals] Teleporting player {} to {} in {}",
                player.getName().getString(),
                BlockPos.containing(targetPosition),
                destinationLevel.dimension().identifier()
        );
        return new TeleportTransition(
                destinationLevel,
                targetPosition,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND
        );
    }

    private record SourcePortalKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos originBlock) {
    }
}
