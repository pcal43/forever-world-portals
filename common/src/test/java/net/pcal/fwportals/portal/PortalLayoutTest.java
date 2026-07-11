package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.pcal.fwportals.TestBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalLayoutTest {

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
}
