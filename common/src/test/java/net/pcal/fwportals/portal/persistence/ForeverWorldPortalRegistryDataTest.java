package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.pcal.fwportals.TestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeverWorldPortalRegistryDataTest {

    @Test
    void serializesAndDeserializesPortalRegistry() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        OriginPortalRecord origin = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                UUID.randomUUID(),
                Level.OVERWORLD,
                new BlockPos(10, 64, 10),
                Direction.Axis.X,
                2,
                3,
                new BlockPos(10, 65, 11),
                UUID.randomUUID(),
                1234L
        );
        ReservedDestinationRecord destination = new ReservedDestinationRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                origin.linkedPortalId(),
                Level.OVERWORLD,
                new BlockPos(25010, 80, -400),
                origin.id(),
                false,
                null,
                null,
                0,
                0,
                null,
                1234L
        );
        data.registerLinkedPair(origin, destination);

        ForeverWorldPortalRegistryData reloaded = roundTrip(data);
        Optional<OriginPortalRecord> loadedOrigin = reloaded.findOrigin(new PortalLookupKey(Level.OVERWORLD, origin.canonicalFrameAnchor()));
        Optional<ReservedDestinationRecord> loadedDestination = reloaded.findReservedDestination(origin.linkedPortalId());

        assertTrue(loadedOrigin.isPresent());
        assertTrue(loadedDestination.isPresent());
        assertEquals(origin.canonicalFrameAnchor(), loadedOrigin.get().canonicalFrameAnchor());
        assertEquals(destination.reservedDestinationPosition(), loadedDestination.get().reservedDestinationPosition());
    }

    @Test
    void malformedPortalRecordsAreSkipped() {
        TestBootstrap.ensureBootstrapped();

        CompoundTag root = new CompoundTag();
        root.store("dataVersion", com.mojang.serialization.Codec.INT, ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION);
        ListTag origins = new ListTag();
        CompoundTag malformedOrigin = new CompoundTag();
        malformedOrigin.store("dimension", net.minecraft.resources.ResourceKey.codec(net.minecraft.core.registries.Registries.DIMENSION), Level.OVERWORLD);
        origins.add(malformedOrigin);
        root.put("origins", origins);
        root.put("reservedDestinations", new ListTag());

        DataResult<ForeverWorldPortalRegistryData> parsed = ForeverWorldPortalRegistryData.TYPE.codec().parse(NbtOps.INSTANCE, root);
        ForeverWorldPortalRegistryData data = parsed.getOrThrow();

        assertFalse(data.findOrigin(new PortalLookupKey(Level.OVERWORLD, new BlockPos(0, 0, 0))).isPresent());
        assertEquals(0, data.originPortals().size());
    }

    @Test
    void separationCheckUsesExistingOriginsAndReservedDestinations() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        OriginPortalRecord origin = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                UUID.randomUUID(),
                Level.OVERWORLD,
                new BlockPos(0, 64, 0),
                Direction.Axis.Z,
                2,
                3,
                new BlockPos(0, 65, 1),
                UUID.randomUUID(),
                1L
        );
        ReservedDestinationRecord destination = new ReservedDestinationRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                origin.linkedPortalId(),
                Level.OVERWORLD,
                new BlockPos(30000, 80, 0),
                origin.id(),
                false,
                null,
                null,
                0,
                0,
                null,
                1L
        );
        data.registerLinkedPair(origin, destination);

        assertFalse(data.isSeparated(Level.OVERWORLD, new BlockPos(100, 80, 0), 25000));
        assertFalse(data.isSeparated(Level.OVERWORLD, new BlockPos(30100, 80, 0), 25000));
        assertTrue(data.isSeparated(Level.OVERWORLD, new BlockPos(60000, 80, 0), 25000));
    }

    @Test
    void materializedDestinationCanBeFoundByAnchor() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        OriginPortalRecord origin = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                UUID.randomUUID(),
                Level.OVERWORLD,
                new BlockPos(5, 70, 5),
                Direction.Axis.X,
                2,
                3,
                new BlockPos(6, 71, 5),
                UUID.randomUUID(),
                10L
        );
        ReservedDestinationRecord destination = new ReservedDestinationRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                origin.linkedPortalId(),
                Level.OVERWORLD,
                new BlockPos(25000, 80, 25000),
                origin.id(),
                false,
                null,
                null,
                0,
                0,
                null,
                10L
        );
        data.registerLinkedPair(origin, destination);

        ReservedDestinationRecord materialized = destination.withPhysicalPortal(
                new BlockPos(25010, 80, 25010),
                Direction.Axis.Z,
                2,
                3,
                new BlockPos(25010, 81, 25011)
        );
        data.materializeReservedDestination(destination.id(), materialized);

        Optional<ReservedDestinationRecord> loadedDestination = data.findReservedDestinationByAnchor(
                new PortalLookupKey(Level.OVERWORLD, materialized.canonicalFrameAnchor())
        );
        assertTrue(loadedDestination.isPresent());
        assertEquals(materialized.canonicalFrameAnchor(), loadedDestination.get().canonicalFrameAnchor());
        assertTrue(loadedDestination.get().physicalPortalExists());
    }

    @Test
    void duplicatePortalAnchorsAreRejected() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        OriginPortalRecord origin = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                UUID.randomUUID(),
                Level.OVERWORLD,
                new BlockPos(0, 64, 0),
                Direction.Axis.Z,
                2,
                3,
                new BlockPos(0, 65, 1),
                UUID.randomUUID(),
                1L
        );
        ReservedDestinationRecord destination = new ReservedDestinationRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                origin.linkedPortalId(),
                Level.OVERWORLD,
                new BlockPos(30000, 80, 0),
                origin.id(),
                true,
                new BlockPos(30000, 79, 0),
                Direction.Axis.Z,
                2,
                3,
                new BlockPos(30000, 80, 1),
                1L
        );
        data.registerLinkedPair(origin, destination);

        OriginPortalRecord conflictingOrigin = new OriginPortalRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                UUID.randomUUID(),
                Level.OVERWORLD,
                new BlockPos(30000, 79, 0),
                Direction.Axis.Z,
                2,
                3,
                new BlockPos(30000, 80, 1),
                UUID.randomUUID(),
                2L
        );
        ReservedDestinationRecord conflictingDestination = new ReservedDestinationRecord(
                ForeverWorldPortalRegistryData.CURRENT_DATA_VERSION,
                conflictingOrigin.linkedPortalId(),
                Level.OVERWORLD,
                new BlockPos(60000, 80, 0),
                conflictingOrigin.id(),
                false,
                null,
                null,
                0,
                0,
                null,
                2L
        );

        assertThrows(IllegalStateException.class, () -> data.registerLinkedPair(conflictingOrigin, conflictingDestination));
    }

    private static ForeverWorldPortalRegistryData roundTrip(ForeverWorldPortalRegistryData data) {
        CompoundTag encoded = (CompoundTag) ForeverWorldPortalRegistryData.TYPE.codec()
                .encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow();
        return ForeverWorldPortalRegistryData.TYPE.codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
    }
}
