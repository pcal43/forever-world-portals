package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.TestBootstrap;
import net.pcal.fwportals.portal.ForeverWorldPortalFrame;
import net.pcal.fwportals.portal.PortalFrameDetector;
import net.pcal.fwportals.portal.PortalFrameDetectorTestBlockGetter;
import net.pcal.fwportals.portal.PortalIdentity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeverWorldPortalRegistryDataTest {

    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity identity = new PortalIdentity();

    @Test
    void serializesAndDeserializesPortals() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        PortalRecord portal = new PortalRecord(
                Level.OVERWORLD,
                new BlockPos(0, 2, 1),
                Level.OVERWORLD,
                new BlockPos(25010, 80, -400)
        );
        data.createPortal(portal);

        ForeverWorldPortalRegistryData reloaded = roundTrip(data);
        List<PortalRecord> portals = reloaded.portals().stream().toList();

        assertEquals(1, portals.size());
        assertEquals(portal.anchor(), portals.get(0).anchor());
        assertEquals(portal.destinationAnchor(), portals.get(0).destinationAnchor());
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

        DataResult<ForeverWorldPortalRegistryData> parsed = ForeverWorldPortalRegistryData.TYPE.codec().parse(NbtOps.INSTANCE, root);
        ForeverWorldPortalRegistryData data = parsed.getOrThrow();

        assertEquals(0, data.portals().size());
    }

    @Test
    void portalInsideFrameMatchesThatFrame() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalFrame frame = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        BlockPos portalAnchor = identity.computeAnchorBlock(frame);

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        data.createPortal(new PortalRecord(
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

        ForeverWorldPortalFrame frame = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        data.createPortal(new PortalRecord(
                Level.OVERWORLD,
                new BlockPos(100, 100, 100),
                Level.OVERWORLD,
                new BlockPos(200, 80, 200)
        ));

        assertTrue(data.findPortalsContainedBy(Level.OVERWORLD, frame).isEmpty());
    }

    @Test
    void multiplePhysicalFramesCanMatchSameStoredOrigin() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalFrame smaller = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        ForeverWorldPortalFrame larger = buildAxisZFrame(new BlockPos(0, 0, 0), 3, 4);
        BlockPos portalAnchor = identity.computeAnchorBlock(smaller);

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        data.createPortal(new PortalRecord(
                Level.OVERWORLD,
                portalAnchor,
                Level.OVERWORLD,
                new BlockPos(500, 80, 500)
        ));

        assertEquals(1, data.findPortalsContainedBy(Level.OVERWORLD, smaller).size());
        assertEquals(1, data.findPortalsContainedBy(Level.OVERWORLD, larger).size());
    }

    @Test
    void separationUsesOriginsAndDestinations() {
        TestBootstrap.ensureBootstrapped();

        ForeverWorldPortalRegistryData data = new ForeverWorldPortalRegistryData();
        data.createPortal(new PortalRecord(
                Level.OVERWORLD,
                new BlockPos(0, 64, 0),
                Level.OVERWORLD,
                new BlockPos(30000, 80, 0)
        ));

        assertFalse(data.isSeparated(Level.OVERWORLD, new BlockPos(100, 80, 0), 25000));
        assertTrue(data.isSeparated(Level.OVERWORLD, new BlockPos(60000, 80, 0), 25000));
    }

    private static ForeverWorldPortalRegistryData roundTrip(ForeverWorldPortalRegistryData data) {
        CompoundTag encoded = (CompoundTag) ForeverWorldPortalRegistryData.TYPE.codec()
                .encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow();
        return ForeverWorldPortalRegistryData.TYPE.codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
    }

    private ForeverWorldPortalFrame buildAxisZFrame(BlockPos anchor, int width, int height) {
        PortalFrameDetectorTestBlockGetter level = new PortalFrameDetectorTestBlockGetter();
        for (int z = 0; z < width + 2; z++) {
            level.setBlock(anchor.offset(0, 0, z), Blocks.DIAMOND_BLOCK.defaultBlockState());
            level.setBlock(anchor.offset(0, height + 1, z), Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        for (int y = 1; y <= height; y++) {
            level.setBlock(anchor.offset(0, y, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
            level.setBlock(anchor.offset(0, y, width + 1), Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        Optional<ForeverWorldPortalFrame> frame = detector.findEmptyFrame(
                level,
                anchor.offset(0, 1, 1),
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        return frame.orElseThrow();
    }
}
