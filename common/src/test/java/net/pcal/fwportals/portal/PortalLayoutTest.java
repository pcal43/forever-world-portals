package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.pcal.fwportals.TestBootstrap;
import net.pcal.fwportals.common.portal.PortalFrame;
import net.pcal.fwportals.common.portal.PortalIdentity;
import net.pcal.fwportals.common.portal.PortalLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalLayoutTest {

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

        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, new BlockPos(100, 70, -50));

        assertEquals(new BlockPos(100, 69, -51), layout.frameBasePos());
        assertEquals(new BlockPos(100, 70, -50), layout.interiorBlocks().get(0));
        assertEquals(new BlockPos(100, 70, -50), layout.anchorBlock());
        assertEquals(12, layout.foundationBlocks().size());
        assertEquals(60, layout.clearanceBlocks().size());
    }

    @Test
    void requestedAnchorRemainsCanonicalForAxisZLayout() {
        TestBootstrap.ensureBootstrapped();

        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, requestedAnchor);
        PortalFrame frame = layout.frame();

        assertEquals(requestedAnchor, identity.computeAnchorBlock(frame));
    }

    @Test
    void requestedAnchorRemainsCanonicalForAxisXLayout() {
        TestBootstrap.ensureBootstrapped();

        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.X, requestedAnchor);
        PortalFrame frame = layout.frame();

        assertEquals(requestedAnchor, identity.computeAnchorBlock(frame));
    }
}
