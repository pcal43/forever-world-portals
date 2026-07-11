package net.pcal.fwportals.portal.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.pcal.fwportals.portal.ForeverWorldPortalFrame;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            .comparing((SourcePortalRecord portal) -> portal.dimension().identifier().toString())
            .thenComparingInt(portal -> portal.originBlock().getX())
            .thenComparingInt(portal -> portal.originBlock().getY())
            .thenComparingInt(portal -> portal.originBlock().getZ())
            .thenComparing(SourcePortalRecord::portalId);

    private final Map<UUID, SourcePortalRecord> sourcePortalsById = new LinkedHashMap<>();

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
                    .ifPresent(record -> data.sourcePortalsById.put(record.portalId(), record));
        }

        LOGGER.info(
                "[fwportals] Loaded Forever World portal registry: sourcePortals={}",
                data.sourcePortalsById.size()
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

    public List<SourcePortalRecord> findSourcePortalsContainedBy(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, ForeverWorldPortalFrame frame) {
        return orderedSourcePortals().stream()
                .filter(portal -> portal.dimension().equals(dimension))
                .filter(portal -> frame.containsInterior(portal.originBlock()))
                .toList();
    }

    public void createSourcePortal(SourcePortalRecord portal) {
        if (sourcePortalsById.containsKey(portal.portalId())) {
            throw new IllegalStateException("Source portal already exists with id " + portal.portalId());
        }
        sourcePortalsById.put(portal.portalId(), portal);
        setDirty();
    }

    public Optional<SourcePortalRecord> getSourcePortal(UUID portalId) {
        return Optional.ofNullable(sourcePortalsById.get(portalId));
    }

    public void removeSourcePortal(UUID portalId) {
        if (sourcePortalsById.remove(portalId) != null) {
            setDirty();
        }
    }

    public boolean isSeparated(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, net.minecraft.core.BlockPos position, int minimumPortalSeparationBlocks) {
        long minimumDistanceSquared = (long) minimumPortalSeparationBlocks * (long) minimumPortalSeparationBlocks;

        for (SourcePortalRecord portal : sourcePortalsById.values()) {
            if (portal.dimension().equals(dimension) && horizontalDistanceSquared(portal.originBlock(), position) < minimumDistanceSquared) {
                return false;
            }
            if (portal.destinationDimension().equals(dimension)
                    && horizontalDistanceSquared(portal.destinationPosition(), position) < minimumDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    public Collection<SourcePortalRecord> sourcePortals() {
        return orderedSourcePortals();
    }

    private List<SourcePortalRecord> orderedSourcePortals() {
        return sourcePortalsById.values().stream().sorted(PORTAL_ORDER).toList();
    }

    private static long horizontalDistanceSquared(net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
