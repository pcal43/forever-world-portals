package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.TestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalLayoutTest {

    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity identity = new PortalIdentity();

    @Test
    void buildsAxisXPortalGeometryFromAnchor() {
        TestBootstrap.ensureBootstrapped();

        PortalLayout layout = PortalLayout.create(Direction.Axis.X, new BlockPos(10, 5, 20), 2, 3);

        assertEquals(14, layout.frameBlocks().size());
        assertEquals(6, layout.interiorBlocks().size());
        assertEquals(new BlockPos(11, 6, 20), layout.representativePortalPosition());
    }

    @Test
    void buildsAxisZPortalGeometryFromAnchor() {
        TestBootstrap.ensureBootstrapped();

        PortalLayout layout = PortalLayout.create(Direction.Axis.Z, new BlockPos(0, 0, 0), 2, 3);

        assertEquals(14, layout.frameBlocks().size());
        assertEquals(6, layout.interiorBlocks().size());
        assertEquals(new BlockPos(0, 1, 1), layout.representativePortalPosition());
    }

    @Test
    void buildsPortalGeometryForRequestedAnchorBlock() {
        TestBootstrap.ensureBootstrapped();

        PortalLayout layout = PortalLayout.createForAnchorBlock(Direction.Axis.Z, new BlockPos(100, 70, -50), 2, 3);

        assertEquals(new BlockPos(100, 69, -51), layout.frameBasePos());
        assertEquals(new BlockPos(100, 70, -50), layout.interiorBlocks().get(0));
    }

    @Test
    void requestedAnchorRemainsCanonicalForAxisZLayout() {
        TestBootstrap.ensureBootstrapped();

        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        PortalLayout layout = PortalLayout.createForAnchorBlock(Direction.Axis.Z, requestedAnchor, 2, 3);
        ForeverWorldPortalFrame frame = detectFrame(layout, Direction.Axis.Z);

        assertEquals(requestedAnchor, identity.computeAnchorBlock(frame));
    }

    @Test
    void requestedAnchorRemainsCanonicalForAxisXLayout() {
        TestBootstrap.ensureBootstrapped();

        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        PortalLayout layout = PortalLayout.createForAnchorBlock(Direction.Axis.X, requestedAnchor, 2, 3);
        ForeverWorldPortalFrame frame = detectFrame(layout, Direction.Axis.X);

        assertEquals(requestedAnchor, identity.computeAnchorBlock(frame));
    }

    private ForeverWorldPortalFrame detectFrame(PortalLayout layout, Direction.Axis axis) {
        PortalFrameDetectorTestBlockGetter level = new PortalFrameDetectorTestBlockGetter();
        for (BlockPos framePos : layout.frameBlocks()) {
            level.setBlock(framePos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        Optional<ForeverWorldPortalFrame> frame = detector.findEmptyFrame(
                level,
                layout.representativePortalPosition(),
                axis,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        return frame.orElseThrow();
    }
}
