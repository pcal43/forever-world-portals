package net.pcal.fwportals.portal.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ForeverWorldPortalRegistryData extends SavedData {

    public static final int CURRENT_DATA_VERSION = 1;
    public static final SavedDataType<ForeverWorldPortalRegistryData> TYPE = new SavedDataType<>(
            Identifier.parse("fwportals_portal_registry"),
            ForeverWorldPortalRegistryData::new,
            CompoundTag.CODEC.xmap(ForeverWorldPortalRegistryData::fromTag, ForeverWorldPortalRegistryData::toTag),
            null
    );

    private static final Logger LOGGER = LogManager.getLogger("fwportals");

    private final Map<UUID, OriginPortalRecord> originPortalsById = new LinkedHashMap<>();
    private final Map<PortalLookupKey, UUID> originPortalIdsByAnchor = new LinkedHashMap<>();
    private final Map<UUID, ReservedDestinationRecord> reservedDestinationsById = new LinkedHashMap<>();

    public static ForeverWorldPortalRegistryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private static ForeverWorldPortalRegistryData fromTag(CompoundTag root) {
        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        CompoundTag migrated = data.migrateRootTag(root);

        int dataVersion = migrated.read("dataVersion", com.mojang.serialization.Codec.INT).orElse(CURRENT_DATA_VERSION);
        ListTag origins = migrated.get("origins") instanceof ListTag list ? list : new ListTag();
        for (int i = 0; i < origins.size(); i++) {
            if (!(origins.get(i) instanceof CompoundTag originTag)) {
                LOGGER.warn("[fwportals] Skipping malformed origin portal entry at index {}: not a compound tag", i);
                continue;
            }
            OriginPortalRecord.fromTag(originTag, message -> LOGGER.warn("[fwportals] {}", message)).ifPresent(record -> data.addOriginInternal(record));
        }

        ListTag reservedDestinations = migrated.get("reservedDestinations") instanceof ListTag list ? list : new ListTag();
        for (int i = 0; i < reservedDestinations.size(); i++) {
            if (!(reservedDestinations.get(i) instanceof CompoundTag destinationTag)) {
                LOGGER.warn("[fwportals] Skipping malformed reserved destination entry at index {}: not a compound tag", i);
                continue;
            }
            ReservedDestinationRecord.fromTag(destinationTag, message -> LOGGER.warn("[fwportals] {}", message))
                    .ifPresent(record -> data.reservedDestinationsById.put(record.id(), record));
        }

        LOGGER.info(
                "[fwportals] Loaded Forever World portal registry: dataVersion={} origins={} reservedDestinations={}",
                dataVersion,
                data.originPortalsById.size(),
                data.reservedDestinationsById.size()
        );
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        root.store("dataVersion", com.mojang.serialization.Codec.INT, CURRENT_DATA_VERSION);

        ListTag origins = new ListTag();
        for (OriginPortalRecord record : originPortalsById.values()) {
            origins.add(record.toTag());
        }
        root.put("origins", origins);

        ListTag reservedDestinations = new ListTag();
        for (ReservedDestinationRecord record : reservedDestinationsById.values()) {
            reservedDestinations.add(record.toTag());
        }
        root.put("reservedDestinations", reservedDestinations);
        return root;
    }

    public Optional<OriginPortalRecord> findOrigin(PortalLookupKey key) {
        UUID id = originPortalIdsByAnchor.get(key);
        return id == null ? Optional.empty() : Optional.ofNullable(originPortalsById.get(id));
    }

    public Optional<ReservedDestinationRecord> findReservedDestination(UUID id) {
        return Optional.ofNullable(reservedDestinationsById.get(id));
    }

    public void registerLinkedPair(OriginPortalRecord origin, ReservedDestinationRecord destination) {
        addOriginInternal(origin);
        reservedDestinationsById.put(destination.id(), destination);
        setDirty();
    }

    public boolean isSeparated(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, net.minecraft.core.BlockPos position, int minimumPortalSeparationBlocks) {
        long minimumDistanceSquared = (long)minimumPortalSeparationBlocks * (long)minimumPortalSeparationBlocks;

        for (OriginPortalRecord origin : originPortalsById.values()) {
            if (origin.dimension().equals(dimension) && horizontalDistanceSquared(origin.representativePortalPosition(), position) < minimumDistanceSquared) {
                return false;
            }
        }

        for (ReservedDestinationRecord destination : reservedDestinationsById.values()) {
            if (destination.dimension().equals(dimension) && horizontalDistanceSquared(destination.reservedDestinationPosition(), position) < minimumDistanceSquared) {
                return false;
            }
        }

        return true;
    }

    public Collection<OriginPortalRecord> originPortals() {
        return originPortalsById.values();
    }

    public Collection<ReservedDestinationRecord> reservedDestinations() {
        return reservedDestinationsById.values();
    }

    private void addOriginInternal(OriginPortalRecord origin) {
        originPortalsById.put(origin.id(), origin);
        originPortalIdsByAnchor.put(new PortalLookupKey(origin.dimension(), origin.canonicalFrameAnchor()), origin.id());
    }

    private CompoundTag migrateRootTag(CompoundTag root) {
        int version = root.read("dataVersion", com.mojang.serialization.Codec.INT).orElse(CURRENT_DATA_VERSION);
        return switch (version) {
            case CURRENT_DATA_VERSION -> root;
            default -> {
                LOGGER.warn("[fwportals] Loading portal registry with unexpected dataVersion={}; attempting best-effort read", version);
                yield root;
            }
        };
    }

    private static long horizontalDistanceSquared(net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        long dx = (long)a.getX() - b.getX();
        long dz = (long)a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
