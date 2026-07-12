package net.pcal.fwportals.portal.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.pcal.fwportals.portal.ForeverWorldPortalFrame;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ForeverWorldPortalRegistryData extends SavedData {

    public static final SavedDataType<ForeverWorldPortalRegistryData> TYPE = new SavedDataType<>(
            Identifier.parse("fwportals_portal_registry"),
            ForeverWorldPortalRegistryData::new,
            CompoundTag.CODEC.xmap(ForeverWorldPortalRegistryData::fromTag, ForeverWorldPortalRegistryData::toTag),
            null
    );

    private static final Logger LOGGER = LogManager.getLogger("fwportals");
    private static final Comparator<SourcePortalRecord> PORTAL_ORDER = Comparator
            .comparing((SourcePortalRecord portal) -> portal.sourceDimension().identifier().toString())
            .thenComparingInt(portal -> portal.sourceAnchor().getX())
            .thenComparingInt(portal -> portal.sourceAnchor().getY())
            .thenComparingInt(portal -> portal.sourceAnchor().getZ())
            .thenComparing(portal -> portal.destinationDimension().identifier().toString())
            .thenComparingInt(portal -> portal.destinationAnchor().getX())
            .thenComparingInt(portal -> portal.destinationAnchor().getY())
            .thenComparingInt(portal -> portal.destinationAnchor().getZ());

    private final Map<SourcePortalKey, SourcePortalRecord> sourcePortalsByKey = new LinkedHashMap<>();

    public static ForeverWorldPortalRegistryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private static ForeverWorldPortalRegistryData fromTag(CompoundTag root) {
        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();

        ListTag sourcePortals = root.get("sourcePortals") instanceof ListTag list ? list : new ListTag();
        for (int i = 0; i < sourcePortals.size(); i++) {
            if (!(sourcePortals.get(i) instanceof CompoundTag portalTag)) {
                LOGGER.warn("[fwportals] Skipping malformed source portal entry at index {}: not a compound tag", i);
                continue;
            }
            SourcePortalRecord.fromTag(portalTag, message -> LOGGER.warn("[fwportals] {}", message))
                    .ifPresent(record -> data.sourcePortalsByKey.put(SourcePortalKey.of(record), record));
        }

        LOGGER.info(
                "[fwportals] Loaded Forever World portal registry: sourcePortals={}",
                data.sourcePortalsByKey.size()
        );
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        ListTag sourcePortals = new ListTag();
        for (SourcePortalRecord record : orderedSourcePortals()) {
            sourcePortals.add(record.toTag());
        }
        root.put("sourcePortals", sourcePortals);
        return root;
    }

    public List<SourcePortalRecord> findSourcePortalsContainedBy(ResourceKey<Level> dimension, ForeverWorldPortalFrame frame) {
        return orderedSourcePortals().stream()
                .filter(portal -> portal.sourceDimension().equals(dimension))
                .filter(portal -> frame.containsInterior(portal.sourceAnchor()))
                .toList();
    }

    public void createSourcePortal(SourcePortalRecord portal) {
        SourcePortalKey key = SourcePortalKey.of(portal);
        if (sourcePortalsByKey.containsKey(key)) {
            throw new IllegalStateException("Source portal already exists for " + key.dimension.identifier() + " at " + key.anchor);
        }
        sourcePortalsByKey.put(key, portal);
        setDirty();
    }

    public Optional<SourcePortalRecord> getSourcePortal(ResourceKey<Level> dimension, BlockPos sourceAnchor) {
        return Optional.ofNullable(sourcePortalsByKey.get(new SourcePortalKey(dimension, sourceAnchor.immutable())));
    }

    public void removeSourcePortal(ResourceKey<Level> dimension, BlockPos sourceAnchor) {
        if (sourcePortalsByKey.remove(new SourcePortalKey(dimension, sourceAnchor.immutable())) != null) {
            setDirty();
        }
    }

    public boolean isSeparated(ResourceKey<Level> dimension, BlockPos position, int minimumPortalSeparationBlocks) {
        long minimumDistanceSquared = (long) minimumPortalSeparationBlocks * (long) minimumPortalSeparationBlocks;

        for (SourcePortalRecord portal : sourcePortalsByKey.values()) {
            if (portal.sourceDimension().equals(dimension)
                    && horizontalDistanceSquared(portal.sourceAnchor(), position) < minimumDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    public Collection<SourcePortalRecord> sourcePortals() {
        return orderedSourcePortals();
    }

    private List<SourcePortalRecord> orderedSourcePortals() {
        return sourcePortalsByKey.values().stream().sorted(PORTAL_ORDER).toList();
    }

    private static long horizontalDistanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private record SourcePortalKey(ResourceKey<Level> dimension, BlockPos anchor) {
        private static SourcePortalKey of(SourcePortalRecord record) {
            return new SourcePortalKey(record.sourceDimension(), record.sourceAnchor().immutable());
        }
    }
}
