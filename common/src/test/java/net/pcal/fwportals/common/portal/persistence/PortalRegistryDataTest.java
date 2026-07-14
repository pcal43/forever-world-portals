package net.pcal.fwportals.common.portal.persistence;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.common.TestBootstrap;
import net.pcal.fwportals.common.persistence.PortalRecord;
import net.pcal.fwportals.common.persistence.PortalRegistryData;
import net.pcal.fwportals.common.portal.PortalFrame;
import net.pcal.fwportals.common.portal.PortalFrameDetector;
import net.pcal.fwportals.common.portal.PortalIdentity;
import net.pcal.fwportals.common.portal.PortalFrameDetectorTestBlockGetter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalRegistryDataTest {

    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity identity = new PortalIdentity();

    @Test
    void serializesAndDeserializesResolvedPortals() {
        TestBootstrap.ensureBootstrapped();

        PortalRegistryData data = new PortalRegistryData();
        PortalRecord portal = PortalRecord.resolved(
                Level.OVERWORLD,
                new BlockPos(0, 2, 1),
                Level.OVERWORLD,
                new BlockPos(25010, 80, -400)
        );
        data.createPortal(portal);

        PortalRegistryData reloaded = roundTrip(data);
        PortalRecord reloadedPortal = reloaded.portals().stream().findFirst().orElseThrow();

        assertTrue(reloadedPortal.isResolved());
        assertTrue(reloadedPortal.attunementItemId().isEmpty());
        assertEquals(portal.anchor(), reloadedPortal.anchor());
        assertEquals(portal.destinationAnchor(), reloadedPortal.destinationAnchor());
    }

    @Test
    void serializesAndDeserializesPendingPortals() {
        TestBootstrap.ensureBootstrapped();

        PortalRegistryData data = new PortalRegistryData();
        PortalRecord portal = PortalRecord.pending(
                Level.OVERWORLD,
                new BlockPos(0, 2, 1),
                Identifier.parse("minecraft:sunflower")
        );
        data.createPortal(portal);

        PortalRegistryData reloaded = roundTrip(data);
        PortalRecord reloadedPortal = reloaded.portals().stream().findFirst().orElseThrow();

        assertTrue(!reloadedPortal.isResolved());
        assertEquals(Optional.of(Identifier.parse("minecraft:sunflower")), reloadedPortal.attunementItemId());
        assertTrue(reloadedPortal.destinationAnchor().isEmpty());
    }

    @Test
    void malformedPortalRecordsAreSkipped() {
        TestBootstrap.ensureBootstrapped();

        CompoundTag root = new CompoundTag();
        ListTag sourcePortals = new ListTag();
        CompoundTag malformedPortal = new CompoundTag();
        malformedPortal.store("sourceDimension", net.minecraft.resources.ResourceKey.codec(net.minecraft.core.registries.Registries.DIMENSION), Level.OVERWORLD);
        sourcePortals.add(malformedPortal);
        root.put("sourcePortals", sourcePortals);

        DataResult<PortalRegistryData> parsed = PortalRegistryData.TYPE.codec().parse(NbtOps.INSTANCE, root);
        PortalRegistryData data = parsed.getOrThrow();

        assertEquals(0, data.portals().size());
    }

    @Test
    void portalInsideFrameMatchesThatFrame() {
        TestBootstrap.ensureBootstrapped();

        PortalFrame frame = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        BlockPos portalAnchor = identity.computeAnchorBlock(frame);

        PortalRegistryData data = new PortalRegistryData();
        data.createPortal(PortalRecord.resolved(
                Level.OVERWORLD,
                portalAnchor,
                Level.OVERWORLD,
                new BlockPos(100, 80, 100)
        ));

        List<PortalRecord> matches = data.findPortalsContainedBy(Level.OVERWORLD, frame);
        assertEquals(1, matches.size());
        assertEquals(portalAnchor, matches.get(0).anchor());
    }

    @Test
    void portalOutsideFrameDoesNotMatch() {
        TestBootstrap.ensureBootstrapped();

        PortalFrame frame = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);

        PortalRegistryData data = new PortalRegistryData();
        data.createPortal(PortalRecord.resolved(
                Level.OVERWORLD,
                new BlockPos(100, 100, 100),
                Level.OVERWORLD,
                new BlockPos(200, 80, 200)
        ));

        assertTrue(data.findPortalsContainedBy(Level.OVERWORLD, frame).isEmpty());
    }

    @Test
    void multiplePhysicalFramesCanMatchSameStoredPortalAnchor() {
        TestBootstrap.ensureBootstrapped();

        PortalFrame smaller = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        PortalFrame larger = buildAxisZFrame(new BlockPos(0, 0, 0), 3, 4);
        BlockPos portalAnchor = identity.computeAnchorBlock(smaller);

        PortalRegistryData data = new PortalRegistryData();
        data.createPortal(PortalRecord.resolved(
                Level.OVERWORLD,
                portalAnchor,
                Level.OVERWORLD,
                new BlockPos(500, 80, 500)
        ));

        assertEquals(1, data.findPortalsContainedBy(Level.OVERWORLD, smaller).size());
        assertEquals(1, data.findPortalsContainedBy(Level.OVERWORLD, larger).size());
    }

    @Test
    void repairedBrokenPortalMatchesExistingRegisteredAnchor() {
        TestBootstrap.ensureBootstrapped();

        PortalFrame repairedDiamondFrame = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        BlockPos registeredAnchor = identity.computeAnchorBlock(repairedDiamondFrame);

        PortalRegistryData data = new PortalRegistryData();
        data.createPortal(PortalRecord.resolved(
                Level.OVERWORLD,
                registeredAnchor,
                Level.OVERWORLD,
                new BlockPos(500, 80, 500)
        ));

        List<PortalRecord> matches = data.findPortalsContainedBy(Level.OVERWORLD, repairedDiamondFrame);
        assertEquals(1, matches.size());
        assertEquals(registeredAnchor, matches.get(0).anchor());
    }

    private static PortalRegistryData roundTrip(PortalRegistryData data) {
        CompoundTag encoded = (CompoundTag) PortalRegistryData.TYPE.codec()
                .encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow();
        return PortalRegistryData.TYPE.codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
    }

    private PortalFrame buildAxisZFrame(BlockPos anchor, int width, int height) {
        PortalFrameDetectorTestBlockGetter level = new PortalFrameDetectorTestBlockGetter();
        for (int z = 0; z < width + 2; z++) {
            level.setBlock(anchor.offset(0, 0, z), Blocks.DIAMOND_BLOCK.defaultBlockState());
            level.setBlock(anchor.offset(0, height + 1, z), Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        for (int y = 1; y <= height; y++) {
            level.setBlock(anchor.offset(0, y, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
            level.setBlock(anchor.offset(0, y, width + 1), Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        Optional<PortalFrame> frame = detector.findEmptyFrame(
                level,
                anchor.offset(0, 1, 1),
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        return frame.orElseThrow();
    }
}
