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
import net.pcal.fwportals.portal.persistence.PortalRecord;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PortalTravelService {

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalDestinationSelector destinationSelector;
    private final SafeLandingFinder safeLandingFinder;
    private final PortalPlacementService portalPlacementService;
    private final PortalIdentity portalIdentity = new PortalIdentity();
    private final Set<PortalKey> inProgressPortals = new HashSet<>();

    public PortalTravelService(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.destinationSelector = new PortalDestinationSelector(config, logger);
        this.safeLandingFinder = new SafeLandingFinder();
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
        List<PortalRecord> matches = registry.findPortalsContainedBy(level.dimension(), frame);
        if (!matches.isEmpty()) {
            return handleRegisteredPortalTravel(level, player, matches);
        }

        if (config.returnPortalMode() != ReturnPortalMode.GENERATE) {
            return failForUnimplementedReturnMode(player, frame);
        }

        BlockPos portalAnchor = portalIdentity.computeAnchorBlock(frame);
        PortalKey inProgressKey = new PortalKey(level.dimension(), portalAnchor);
        if (!inProgressPortals.add(inProgressKey)) {
            player.sendSystemMessage(Component.literal("Forever World portal founding is already in progress."));
            logger.warn("[fwportals] Portal founding already in progress for portal anchor {}", portalAnchor);
            return null;
        }

        try {
            return handleFoundingTravel(level, player, frame, registry, portalAnchor);
        } finally {
            inProgressPortals.remove(inProgressKey);
        }
    }

    public void clearRuntimeState() {
        inProgressPortals.clear();
    }

    private @Nullable TeleportTransition handleRegisteredPortalTravel(ServerLevel level, ServerPlayer player, List<PortalRecord> matches) {
        PortalRecord selected = matches.get(0);
        if (matches.size() > 1) {
            logger.warn(
                    "[fwportals] Multiple portals matched one physical portal in {}: {}. Using first deterministic match {}",
                    level.dimension().identifier(),
                    matches.stream().map(PortalRecord::anchor).toList(),
                    selected.anchor()
            );
        }

        ServerLevel destinationLevel = level.getServer().getLevel(selected.destinationDimension());
        if (destinationLevel == null) {
            logger.warn(
                    "[fwportals] Destination dimension {} is unavailable for portal anchor {}",
                    selected.destinationDimension().identifier(),
                    selected.anchor()
            );
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            return null;
        }

        return buildTransition(destinationLevel, Vec3.atBottomCenterOf(selected.destinationAnchor()), player);
    }

    private @Nullable TeleportTransition handleFoundingTravel(
            ServerLevel level,
            ServerPlayer player,
            ForeverWorldPortalFrame sourceFrame,
            ForeverWorldPortalRegistryData registry,
            BlockPos portalAnchor
    ) {
        ServerLevel destinationLevel = level.getServer().getLevel(level.dimension());
        if (destinationLevel == null) {
            player.sendSystemMessage(Component.literal("Forever World destination is unavailable. Try again later."));
            logger.warn(
                    "[fwportals] Destination dimension {} is unavailable for portal anchor {}",
                    level.dimension().identifier(),
                    portalAnchor
            );
            return null;
        }

        for (int attempt = 0; attempt < config.destinationSearchAttempts(); attempt++) {
            Optional<DestinationPortalCandidate> maybeCandidate = destinationSelector.findCandidateAnchor(
                    level,
                    portalAnchor,
                    registry,
                    attempt
            );
            if (maybeCandidate.isEmpty()) {
                continue;
            }

            DestinationPortalCandidate candidate = maybeCandidate.get();
            Optional<GeneratedPortal> generatedPortal = portalPlacementService.tryGeneratePortalAtAnchor(
                    destinationLevel,
                    player,
                    candidate.requestedAnchor(),
                    sourceFrame.width(),
                    sourceFrame.height(),
                    config.frameBlock().defaultBlockState()
            );
            if (generatedPortal.isEmpty()) {
                continue;
            }

            PortalPlacementRollback rollback = generatedPortal.get().rollback();
            BlockPos generatedPortalAnchor = generatedPortal.get().anchorBlock();

            List<PortalRecord> destinationMatches = registry.findPortalsContainedBy(destinationLevel.dimension(), generatedPortal.get().frame());
            if (!destinationMatches.isEmpty()) {
                rollback.rollback();
                player.sendSystemMessage(Component.literal("Forever World return portal location is already claimed."));
                logger.warn(
                        "[fwportals] Generated destination portal at anchor {} from requested anchor {} also enclosed existing portal anchors {}",
                        generatedPortalAnchor,
                        candidate.requestedAnchor(),
                        destinationMatches.stream().map(PortalRecord::anchor).toList()
                );
                continue;
            }

            PortalKey generatedKey = new PortalKey(destinationLevel.dimension(), generatedPortalAnchor);
            if (!inProgressPortals.add(generatedKey)) {
                rollback.rollback();
                player.sendSystemMessage(Component.literal("Forever World return portal founding is already in progress."));
                logger.warn("[fwportals] Reverse route founding already in progress for portal anchor {}", generatedPortalAnchor);
                continue;
            }

            boolean success = false;
            boolean rolledBack = false;
            try {
                PortalRecord outboundPortal = new PortalRecord(
                        level.dimension(),
                        portalAnchor.immutable(),
                        destinationLevel.dimension(),
                        generatedPortalAnchor
                );
                PortalRecord reversePortal = new PortalRecord(
                        destinationLevel.dimension(),
                        generatedPortalAnchor,
                        level.dimension(),
                        portalAnchor.immutable()
                );

                registry.createPortal(outboundPortal);
                try {
                    registry.createPortal(reversePortal);
                } catch (RuntimeException ex) {
                    registry.removePortal(outboundPortal.dimension(), outboundPortal.anchor());
                    throw ex;
                }

                logger.info(
                        "[fwportals] Registered portal route {} in {} -> {} in {}",
                        outboundPortal.anchor(),
                        outboundPortal.dimension().identifier(),
                        outboundPortal.destinationAnchor(),
                        outboundPortal.destinationDimension().identifier()
                );
                logger.info(
                    "[fwportals] Registered reverse portal route {} in {} -> {} in {}",
                    reversePortal.anchor(),
                    reversePortal.dimension().identifier(),
                    reversePortal.destinationAnchor(),
                    reversePortal.destinationDimension().identifier()
                );

                success = true;
                return buildTransition(destinationLevel, Vec3.atBottomCenterOf(generatedPortalAnchor), player);
            } catch (RuntimeException ex) {
                logger.warn(
                        "[fwportals] Rolling back failed portal founding for portal anchor {} and requested destination anchor {}: {}",
                        portalAnchor,
                        candidate.requestedAnchor(),
                        ex.getMessage()
                );
                try {
                    rollback.rollback();
                    rolledBack = true;
                } catch (RuntimeException rollbackEx) {
                    logger.error(
                        "[fwportals] Rollback failed after portal founding error for portal anchor {}: {}",
                            portalAnchor,
                            rollbackEx.getMessage()
                    );
                }
                player.sendSystemMessage(Component.literal("Forever World portal founding failed. Try again later."));
                return null;
            } finally {
                inProgressPortals.remove(generatedKey);
                if (!success && !rolledBack) {
                    try {
                        rollback.rollback();
                    } catch (RuntimeException rollbackEx) {
                        logger.error(
                                "[fwportals] Rollback failed while cleaning up portal founding for portal anchor {}: {}",
                                portalAnchor,
                                rollbackEx.getMessage()
                        );
                    }
                }
            }
        }

        player.sendSystemMessage(Component.literal("Forever World portal could not find a safe destination yet."));
        logger.warn(
                "[fwportals] Failed to create a destination portal after {} attempts for portal anchor {} in {}",
                config.destinationSearchAttempts(),
                portalAnchor,
                level.dimension().identifier()
        );
        return null;
    }

    private @Nullable TeleportTransition failForUnimplementedReturnMode(ServerPlayer player, ForeverWorldPortalFrame frame) {
        player.sendSystemMessage(Component.literal(
                "Forever World return portal mode " + config.returnPortalMode() + " is not implemented yet."
        ));
        logger.warn(
                "[fwportals] Return portal mode {} is not implemented for frame base {}",
                config.returnPortalMode(),
                frame.frameBasePos()
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

    private record PortalKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos anchor) {
    }
}
