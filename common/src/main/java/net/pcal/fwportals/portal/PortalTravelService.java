package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.pcal.fwportals.DestinationPortalMode;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import net.pcal.fwportals.attunement.AttunementDefinition;
import net.pcal.fwportals.attunement.AttunementLookup;
import net.pcal.fwportals.attunement.AttunementRegistry;
import net.pcal.fwportals.attunement.BiomeDestinationTarget;
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
    private final AttunementRegistry attunementRegistry;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalDestinationSelector destinationSelector;
    private final SafeLandingFinder safeLandingFinder;
    private final PortalPlacementService portalPlacementService;
    private final PortalIdentity portalIdentity = new PortalIdentity();
    private final Set<PortalKey> inProgressPortals = new HashSet<>();

    public PortalTravelService(ForeverWorldPortalsConfig config, Logger logger, AttunementRegistry attunementRegistry) {
        this.config = config;
        this.logger = logger;
        this.attunementRegistry = attunementRegistry;
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
        if (config.requireEmptyInventory() && !PortalInventoryAccess.isEmpty(player.getInventory())) {
            return null;
        }

        ForeverWorldPortalFrame frame = maybeFrame.get();
        ForeverWorldPortalRegistryData registry = ForeverWorldPortalRegistryData.get(level);
        List<PortalRecord> matches = registry.findPortalsContainedBy(level.dimension(), frame);
        PortalRecord matchedPortal = selectDeterministicMatch(level, matches);
        if (matchedPortal != null && matchedPortal.isResolved()) {
            return handleRegisteredPortalTravel(level, player, matchedPortal);
        }

        BlockPos portalAnchor = matchedPortal != null ? matchedPortal.anchor() : portalIdentity.computeAnchorBlock(frame);
        PortalKey inProgressKey = new PortalKey(level.dimension(), portalAnchor);
        if (!inProgressPortals.add(inProgressKey)) {
            player.sendSystemMessage(PortalFeedbackText.foundingInProgressMessage());
            logger.warn("[fwportals] Portal founding already in progress for portal anchor {}", portalAnchor);
            return null;
        }

        try {
            return handleFoundingTravel(level, player, registry, matchedPortal, portalAnchor);
        } finally {
            inProgressPortals.remove(inProgressKey);
        }
    }

    public void clearRuntimeState() {
        inProgressPortals.clear();
    }

    private @Nullable TeleportTransition handleRegisteredPortalTravel(ServerLevel level, ServerPlayer player, PortalRecord selected) {
        ServerLevel destinationLevel = level.getServer().getLevel(selected.destinationDimension().orElseThrow());
        if (destinationLevel == null) {
            logger.warn(
                    "[fwportals] Destination dimension {} is unavailable for portal anchor {}",
                    selected.destinationDimension().orElseThrow().identifier(),
                    selected.anchor()
            );
            player.sendSystemMessage(PortalFeedbackText.destinationUnavailableMessage());
            return null;
        }

        return buildTransition(destinationLevel, Vec3.atBottomCenterOf(selected.destinationAnchor().orElseThrow()), player);
    }

    private @Nullable TeleportTransition handleFoundingTravel(
            ServerLevel level,
            ServerPlayer player,
            ForeverWorldPortalRegistryData registry,
            @Nullable PortalRecord existingPortal,
            BlockPos portalAnchor
    ) {
        AttunementLookup attunementLookup = attunementRegistry.currentLookup();
        AttunementDefinition selectedAttunement = foundingAttunement(attunementLookup, existingPortal, logger);
        BiomeDestinationTarget destinationTarget = foundingDestinationTarget(selectedAttunement);
        String targetLabel = foundingTargetLabel(selectedAttunement);
        ServerLevel destinationLevel = level.getServer().getLevel(destinationTarget.dimension());
        if (destinationLevel == null) {
            player.sendSystemMessage(PortalFeedbackText.destinationUnavailableMessage());
            logger.warn(
                    "[fwportals] Destination dimension {} is unavailable for portal anchor {}",
                    destinationTarget.dimension().identifier(),
                    portalAnchor
            );
            return null;
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(
                PortalFeedbackText.seekingFrontierMessage(selectedAttunement)
        ));
        logger.info(
                "[fwportals] Selected destination portal mode {} for portal anchor {} in {}",
                config.destinationPortalMode(),
                portalAnchor,
                level.dimension().identifier()
        );

        PortalDestinationSelector.SearchContext searchContext = destinationSelector.beginSearch(
                destinationLevel,
                portalAnchor,
                destinationTarget,
                targetLabel
        );

        while (!searchContext.searchBudget().isExhausted()) {
            Optional<DestinationPortalCandidate> maybeCandidate = destinationSelector.findCandidateAnchor(searchContext);
            if (maybeCandidate.isEmpty()) {
                break;
            }

            DestinationPortalCandidate candidate = maybeCandidate.get();
            BlockState destinationFrameState = config.destinationPortalMode() == DestinationPortalMode.BROKEN
                    ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                    : config.frameBlock().defaultBlockState();
            PortalPlacementService.LayoutSearchResult layoutSearchResult = portalPlacementService.findValidLayoutNearAnchor(
                    destinationLevel,
                    candidate.requestedAnchor(),
                    config.maximumPortalPlacementAttemptsPerBiome(),
                    destinationFrameState
            );
            if (layoutSearchResult.layout().isEmpty()) {
                if (layoutSearchResult.failureReason()
                        == PortalPlacementService.LayoutSearchResult.FailureReason.PLACEMENT_ATTEMPTS_EXHAUSTED) {
                    logger.debug(
                            "[fwportals] Exhausted {} portal placement attempts for biome result {} in {}. Continuing outer search with {} spiral positions and {} biome searches consumed.",
                            config.maximumPortalPlacementAttemptsPerBiome(),
                            candidate.requestedAnchor(),
                            destinationLevel.dimension().identifier(),
                            searchContext.searchBudget().spiralPositionsExamined(),
                            searchContext.searchBudget().biomeSearchesPerformed()
                    );
                }
                continue;
            }

            PortalLayout layout = layoutSearchResult.layout().orElseThrow();
            BlockPos generatedPortalAnchor = layout.anchorBlock();

            boolean rejectedByConstraint = false;
            for (DestinationConstraint constraint : searchContext.constraints()) {
                String rejectionReason = constraint.rejectionReason(generatedPortalAnchor);
                if (rejectionReason != null) {
                    logger.warn(
                            "[fwportals] Rejecting generated destination portal at anchor {} because {}",
                            generatedPortalAnchor,
                            rejectionReason
                    );
                    rejectedByConstraint = true;
                    break;
                }
            }
            if (rejectedByConstraint) {
                continue;
            }

            List<PortalRecord> destinationMatches = registry.findPortalsContainedBy(destinationLevel.dimension(), layout.frame());
            if (!destinationMatches.isEmpty()) {
                player.sendSystemMessage(PortalFeedbackText.returnPortalClaimedMessage());
                logger.warn(
                        "[fwportals] Generated destination portal at anchor {} from requested anchor {} also enclosed existing portal anchors {}",
                        generatedPortalAnchor,
                        candidate.requestedAnchor(),
                        destinationMatches.stream().map(PortalRecord::anchor).toList()
                );
                continue;
            }

            if (config.destinationPortalMode() == DestinationPortalMode.NONE) {
                FoundingRegistration registration = buildFoundingRegistration(
                        config.destinationPortalMode(),
                        level.dimension(),
                        portalAnchor.immutable(),
                        destinationLevel.dimension(),
                        generatedPortalAnchor
                );
                PortalRecord outboundPortal = registration.outboundPortal();
                registry.putPortal(outboundPortal);
                logger.info(
                        "[fwportals] Registered outbound-only portal route {} in {} -> {} in {} with destination portal mode none",
                        outboundPortal.anchor(),
                        outboundPortal.dimension().identifier(),
                        outboundPortal.destinationAnchor().orElseThrow(),
                        outboundPortal.destinationDimension().orElseThrow().identifier()
                );
                logger.info(
                        "[fwportals] No destination portal generated or registered at validated anchor {} in {}",
                        generatedPortalAnchor,
                        destinationLevel.dimension().identifier()
                );
                return buildTransition(destinationLevel, Vec3.atBottomCenterOf(generatedPortalAnchor), player);
            }

            PortalKey generatedKey = new PortalKey(destinationLevel.dimension(), generatedPortalAnchor);
            if (!inProgressPortals.add(generatedKey)) {
                player.sendSystemMessage(PortalFeedbackText.returnPortalFoundingInProgressMessage());
                logger.warn("[fwportals] Reverse route founding already in progress for portal anchor {}", generatedPortalAnchor);
                continue;
            }

            try {
                GeneratedPortal generatedPortal = portalPlacementService.placeDestinationPortal(
                        destinationLevel,
                        layout,
                        config.destinationPortalMode(),
                        config.frameBlock().defaultBlockState()
                );
                FoundingRegistration registration = buildFoundingRegistration(
                        config.destinationPortalMode(),
                        level.dimension(),
                        portalAnchor.immutable(),
                        destinationLevel.dimension(),
                        generatedPortalAnchor
                );
                PortalRecord outboundPortal = registration.outboundPortal();
                PortalRecord reversePortal = registration.reversePortal().orElseThrow();
                PortalRecord previousOutboundPortal = existingPortal;
                registry.putPortal(outboundPortal);
                try {
                    registry.createPortal(reversePortal);
                } catch (RuntimeException ex) {
                    if (previousOutboundPortal != null) {
                        registry.putPortal(previousOutboundPortal);
                    } else {
                        registry.removePortal(outboundPortal.dimension(), outboundPortal.anchor());
                    }
                    throw ex;
                }

                logger.info(
                        "[fwportals] Registered portal route {} in {} -> {} in {}",
                        outboundPortal.anchor(),
                        outboundPortal.dimension().identifier(),
                        outboundPortal.destinationAnchor().orElseThrow(),
                        outboundPortal.destinationDimension().orElseThrow().identifier()
                );
                logger.info(
                        "[fwportals] Registered reverse portal route {} in {} -> {} in {}",
                    reversePortal.anchor(),
                    reversePortal.dimension().identifier(),
                    reversePortal.destinationAnchor().orElseThrow(),
                    reversePortal.destinationDimension().orElseThrow().identifier()
                );
                logger.info(
                        "[fwportals] Destination portal {} generated at anchor {} with axis {} and registered for return travel",
                        config.destinationPortalMode().name().toLowerCase(java.util.Locale.ROOT),
                        layout.anchorBlock(),
                        layout.axis()
                );

                return buildTransition(destinationLevel, Vec3.atBottomCenterOf(generatedPortal.anchorBlock()), player);
            } catch (RuntimeException ex) {
                logger.warn(
                        "[fwportals] Portal founding failed for portal anchor {} and requested destination anchor {}: {}",
                        portalAnchor,
                        candidate.requestedAnchor(),
                        ex.getMessage()
                );
                player.sendSystemMessage(PortalFeedbackText.foundingFailedMessage());
                return null;
            } finally {
                inProgressPortals.remove(generatedKey);
            }
        }

        player.sendSystemMessage(PortalFeedbackText.noSafeDestinationMessage());
        logger.warn(
                "[fwportals] Failed to create a destination portal for portal anchor {} in {} using {} after {} spiral positions and {} biome searches. Termination: {}",
                portalAnchor,
                level.dimension().identifier(),
                targetLabel,
                searchContext.searchBudget().spiralPositionsExamined(),
                searchContext.searchBudget().biomeSearchesPerformed(),
                searchContext.searchBudget().exhaustionReason().orElse(PortalDestinationSelector.ExhaustionReason.UNKNOWN)
        );
        return null;
    }

    static BiomeDestinationTarget defaultFoundingDestinationTarget(AttunementLookup attunementLookup) {
        return attunementLookup.defaultTarget();
    }

    static AttunementDefinition foundingAttunement(
            AttunementLookup attunementLookup,
            @Nullable PortalRecord portal,
            Logger logger
    ) {
        if (portal != null && portal.attunementItemId().isPresent()) {
            Identifier itemId = portal.attunementItemId().orElseThrow();
            Optional<AttunementDefinition> attunement = attunementLookup.resolve(itemId);
            if (attunement.isPresent()) {
                return attunement.get();
            }

            logger.warn(
                    "[fwportals] Pending portal {} in {} has unresolved attunement item {}. Falling back to default target.",
                    portal.anchor(),
                    portal.dimension().identifier(),
                    itemId
            );
        }
        return attunementLookup.defaultAttunement();
    }

    static BiomeDestinationTarget foundingDestinationTarget(AttunementDefinition attunementDefinition) {
        if (!(attunementDefinition.target() instanceof BiomeDestinationTarget biomeTarget)) {
            throw new IllegalStateException("Unsupported destination target type " + attunementDefinition.target().getClass().getName());
        }
        return biomeTarget;
    }

    static String foundingTargetLabel(AttunementDefinition attunementDefinition) {
        return "attunement " + attunementDefinition.id();
    }

    static FoundingRegistration buildFoundingRegistration(
            DestinationPortalMode destinationPortalMode,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> sourceDimension,
            BlockPos sourceAnchor,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destinationDimension,
            BlockPos destinationAnchor
    ) {
        PortalRecord outboundPortal = PortalRecord.resolved(
                sourceDimension,
                sourceAnchor,
                destinationDimension,
                destinationAnchor
        );
        if (destinationPortalMode == DestinationPortalMode.NONE) {
            return new FoundingRegistration(outboundPortal, Optional.empty());
        }

        PortalRecord reversePortal = PortalRecord.resolved(
                destinationDimension,
                destinationAnchor,
                sourceDimension,
                sourceAnchor
        );
        return new FoundingRegistration(outboundPortal, Optional.of(reversePortal));
    }

    private PortalRecord selectDeterministicMatch(ServerLevel level, List<PortalRecord> matches) {
        if (matches.isEmpty()) {
            return null;
        }
        PortalRecord selected = matches.get(0);
        if (matches.size() > 1) {
            logger.warn(
                    "[fwportals] Multiple portals matched one physical portal in {}: {}. Using first deterministic match {}",
                    level.dimension().identifier(),
                    matches.stream().map(PortalRecord::anchor).toList(),
                    selected.anchor()
            );
        }
        return selected;
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

    record FoundingRegistration(PortalRecord outboundPortal, Optional<PortalRecord> reversePortal) {
    }
}
