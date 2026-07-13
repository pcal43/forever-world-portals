package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.pcal.fwportals.common.attunement.AttunementDefinition;
import net.pcal.fwportals.common.attunement.AttunementLookup;
import net.pcal.fwportals.common.attunement.AttunementRegistry;
import net.pcal.fwportals.common.persistence.PortalRegistryData;
import net.pcal.fwportals.common.persistence.PortalRecord;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PortalAttunementService {

    private static final long OFFERING_COOLDOWN_TICKS = 100L;

    private final Logger logger;
    private final AttunementRegistry attunementRegistry;
    private final PortalIdentity portalIdentity = new PortalIdentity();
    private final Map<UUID, Long> recentlyAcceptedOfferingTicks = new HashMap<>();
    private final Map<ResourceKey<Level>, List<ActiveAnimation>> activeAnimationsByDimension = new HashMap<>();

    public PortalAttunementService(Logger logger, AttunementRegistry attunementRegistry) {
        this.logger = logger;
        this.attunementRegistry = attunementRegistry;
    }

    public void onItemInsidePortal(ServerLevel level, PortalFrame frame, ItemEntity itemEntity) {
        if (itemEntity.isRemoved() || itemEntity.isOnPortalCooldown()) {
            return;
        }

        long gameTime = level.getGameTime();
        Long lastAcceptedTick = recentlyAcceptedOfferingTicks.get(itemEntity.getUUID());
        if (lastAcceptedTick != null && gameTime - lastAcceptedTick <= OFFERING_COOLDOWN_TICKS) {
            return;
        }

        PortalRegistryData registry = PortalRegistryData.get(level);
        List<PortalRecord> matches = registry.findPortalsContainedBy(level.dimension(), frame);
        PortalRecord selectedPortal = selectDeterministicMatch(level.dimension(), matches);
        if (selectedPortal != null && selectedPortal.isResolved()) {
            return;
        }

        BlockPos portalAnchor = selectedPortal != null
                ? selectedPortal.anchor()
                : portalIdentity.computeAnchorBlock(frame);
        Optional<AcceptedOffering> maybeOffering = resolveAcceptedOffering(
                attunementRegistry.currentLookup(),
                selectedPortal,
                level.dimension(),
                portalAnchor,
                itemEntity.getItem()
        );
        if (maybeOffering.isEmpty()) {
            return;
        }

        AcceptedOffering offering = maybeOffering.get();
        registry.putPortal(offering.updatedPortal());
        consumeAcceptedItem(itemEntity);
        recentlyAcceptedOfferingTicks.put(itemEntity.getUUID(), gameTime);

        startAcceptedOfferingFeedback(level, portalAnchor, frame, offering.attunementDefinition());

        Entity owner = itemEntity.getOwner();
        if (owner instanceof ServerPlayer player && !player.isRemoved()) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    PortalFeedbackText.acceptedAttunementMessage(offering.attunementDefinition())
            ));
        }

        logger.info(
                "[fwportals] Portal {} in {} accepted attunement item {}",
                portalAnchor,
                level.dimension().identifier(),
                offering.attunementItemId()
        );
    }

    public void clearRuntimeState() {
        recentlyAcceptedOfferingTicks.clear();
        activeAnimationsByDimension.clear();
    }

    public void onServerTick(MinecraftServer server) {
        for (Map.Entry<ResourceKey<Level>, List<ActiveAnimation>> entry : activeAnimationsByDimension.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }

            Iterator<ActiveAnimation> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                ActiveAnimation animation = iterator.next();
                PortalAttunementParticles.emitAcceptedOfferingTick(
                        level,
                        animation.bounds(),
                        animation.colorRgb(),
                        animation.tickIndex()
                );
                if (animation.tickIndex() + 1 >= PortalAttunementParticles.ANIMATION_TICKS) {
                    iterator.remove();
                } else {
                    animation.advance();
                }
            }
        }
        activeAnimationsByDimension.values().removeIf(List::isEmpty);
    }

    static Optional<AcceptedOffering> resolveAcceptedOffering(
            AttunementLookup attunementLookup,
            @Nullable PortalRecord existingPortal,
            ResourceKey<Level> dimension,
            BlockPos portalAnchor,
            ItemStack stack
    ) {
        return resolveAcceptedOffering(attunementLookup, existingPortal, dimension, portalAnchor, stack.getItem());
    }

    static Optional<AcceptedOffering> resolveAcceptedOffering(
            AttunementLookup attunementLookup,
            @Nullable PortalRecord existingPortal,
            ResourceKey<Level> dimension,
            BlockPos portalAnchor,
            Item item
    ) {
        if (existingPortal != null && existingPortal.isResolved()) {
            return Optional.empty();
        }

        Optional<AttunementDefinition> definition = attunementLookup.resolve(item);
        if (definition.isEmpty() || definition.get().item() == null) {
            return Optional.empty();
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(definition.get().item());
        PortalRecord updatedPortal = existingPortal == null
                ? PortalRecord.pending(dimension, portalAnchor, itemId)
                : existingPortal.withAttunementItem(itemId);
        return Optional.of(new AcceptedOffering(updatedPortal, itemId, definition.get()));
    }

    static void consumeAcceptedItem(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        consumeAcceptedStack(stack);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
            itemEntity.setPortalCooldown();
        }
    }

    static void consumeAcceptedStack(ItemStack stack) {
        stack.setCount(remainingCountAfterAcceptedOffering(stack.getCount()));
    }

    static int remainingCountAfterAcceptedOffering(int count) {
        return Math.max(0, count - 1);
    }

    private void startAcceptedOfferingFeedback(
            ServerLevel level,
            BlockPos portalAnchor,
            PortalFrame frame,
            AttunementDefinition attunementDefinition
    ) {
        level.playSound(null, portalAnchor, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.75F, 1.1F);
        ActiveAnimation animation = new ActiveAnimation(
                PortalAttunementParticles.boundsOf(frame),
                attunementDefinition.colorRgb()
        );
        activeAnimationsByDimension.computeIfAbsent(level.dimension(), ignored -> new java.util.ArrayList<>()).add(animation);
        PortalAttunementParticles.emitAcceptedOfferingTick(level, animation.bounds(), animation.colorRgb(), animation.tickIndex());
        animation.advance();
    }

    private PortalRecord selectDeterministicMatch(ResourceKey<Level> dimension, List<PortalRecord> matches) {
        if (matches.isEmpty()) {
            return null;
        }

        PortalRecord selected = matches.get(0);
        if (matches.size() > 1) {
            logger.warn(
                    "[fwportals] Multiple portals matched one physical portal in {} while processing attunement offering: {}. Using first deterministic match {}",
                    dimension.identifier(),
                    matches.stream().map(PortalRecord::anchor).toList(),
                    selected.anchor()
            );
        }
        return selected;
    }

    record AcceptedOffering(PortalRecord updatedPortal, Identifier attunementItemId, AttunementDefinition attunementDefinition) {
    }

    private static final class ActiveAnimation {
        private final PortalAttunementParticles.PortalInteriorBounds bounds;
        private final int colorRgb;
        private int tickIndex;

        private ActiveAnimation(PortalAttunementParticles.PortalInteriorBounds bounds, int colorRgb) {
            this.bounds = bounds;
            this.colorRgb = colorRgb;
        }

        PortalAttunementParticles.PortalInteriorBounds bounds() {
            return bounds;
        }

        int colorRgb() {
            return colorRgb;
        }

        int tickIndex() {
            return tickIndex;
        }

        void advance() {
            tickIndex++;
        }
    }
}
