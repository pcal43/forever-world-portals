package net.pcal.fwportals.common.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.pcal.fwportals.common.portal.PortalFrame;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PortalRegistryData extends SavedData {

    public static final SavedDataType<PortalRegistryData> TYPE = new SavedDataType<>(
            Identifier.parse("fwportals_portal_registry"),
            PortalRegistryData::new,
            CompoundTag.CODEC.xmap(PortalRegistryData::fromTag, PortalRegistryData::toTag),
            null
    );

    private static final Logger LOGGER = LogManager.getLogger("fwportals");
    private static final Comparator<PortalRecord> PORTAL_ORDER = Comparator
            .comparing((PortalRecord portal) -> portal.dimension().identifier().toString())
            .thenComparingInt(portal -> portal.anchor().getX())
            .thenComparingInt(portal -> portal.anchor().getY())
            .thenComparingInt(portal -> portal.anchor().getZ())
            .thenComparing(portal -> portal.destinationDimension().map(value -> value.identifier().toString()).orElse(""))
            .thenComparingInt(portal -> portal.destinationAnchor().map(BlockPos::getX).orElse(Integer.MIN_VALUE))
            .thenComparingInt(portal -> portal.destinationAnchor().map(BlockPos::getY).orElse(Integer.MIN_VALUE))
            .thenComparingInt(portal -> portal.destinationAnchor().map(BlockPos::getZ).orElse(Integer.MIN_VALUE))
            .thenComparing(portal -> portal.attunementItemId().map(Object::toString).orElse(""));

    private final Map<PortalKey, PortalRecord> portalsByKey = new LinkedHashMap<>();

    public static PortalRegistryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private static PortalRegistryData fromTag(CompoundTag root) {
        PortalRegistryData data = new PortalRegistryData();

        ListTag portals = root.get("sourcePortals") instanceof ListTag list ? list : new ListTag();
        for (int i = 0; i < portals.size(); i++) {
            if (!(portals.get(i) instanceof CompoundTag portalTag)) {
                LOGGER.warn("[fwportals] Skipping malformed portal entry at index {}: not a compound tag", i);
                continue;
            }
            PortalRecord.fromTag(portalTag, message -> LOGGER.warn("[fwportals] {}", message))
                    .ifPresent(record -> data.portalsByKey.put(PortalKey.of(record), record));
        }

        LOGGER.info(
                "[fwportals] Loaded Forever World portal registry: portals={}",
                data.portalsByKey.size()
        );
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        ListTag portals = new ListTag();
        for (PortalRecord record : orderedPortals()) {
            portals.add(record.toTag());
        }
        root.put("sourcePortals", portals);
        return root;
    }

    public List<PortalRecord> findPortalsContainedBy(ResourceKey<Level> dimension, PortalFrame frame) {
        return orderedPortals().stream()
                .filter(portal -> portal.dimension().equals(dimension))
                .filter(portal -> frame.containsInterior(portal.anchor()))
                .toList();
    }

    public void createPortal(PortalRecord portal) {
        PortalKey key = PortalKey.of(portal);
        if (portalsByKey.containsKey(key)) {
            throw new IllegalStateException("Portal already exists for " + key.dimension.identifier() + " at " + key.anchor);
        }
        portalsByKey.put(key, portal);
        setDirty();
    }

    public void putPortal(PortalRecord portal) {
        portalsByKey.put(PortalKey.of(portal), portal);
        setDirty();
    }

    public Optional<PortalRecord> getPortal(ResourceKey<Level> dimension, BlockPos portalAnchor) {
        return Optional.ofNullable(portalsByKey.get(new PortalKey(dimension, portalAnchor.immutable())));
    }

    public void removePortal(ResourceKey<Level> dimension, BlockPos portalAnchor) {
        if (portalsByKey.remove(new PortalKey(dimension, portalAnchor.immutable())) != null) {
            setDirty();
        }
    }

    public Collection<PortalRecord> portals() {
        return orderedPortals();
    }

    private List<PortalRecord> orderedPortals() {
        return portalsByKey.values().stream().sorted(PORTAL_ORDER).toList();
    }

    private record PortalKey(ResourceKey<Level> dimension, BlockPos anchor) {
        private static PortalKey of(PortalRecord record) {
            return new PortalKey(record.dimension(), record.anchor().immutable());
        }
    }
}
