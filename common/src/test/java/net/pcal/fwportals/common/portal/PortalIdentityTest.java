package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.common.TestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalIdentityTest {

    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity identity = new PortalIdentity();

    @Test
    void oddWidthInteriorSelectsSingleCenterBlockInLowestRow() {
        TestBootstrap.ensureBootstrapped();
        PortalFrame frame = buildAxisZFrame(new BlockPos(0, 0, 0), 3, 3);

        assertEquals(new BlockPos(0, 1, 2), identity.computeAnchorBlock(frame));
    }

    @Test
    void evenWidthAxisXTieBreaksTowardNegativeX() {
        TestBootstrap.ensureBootstrapped();
        PortalFrame frame = buildAxisXFrame(new BlockPos(10, 5, 20), 2, 4);

        assertEquals(new BlockPos(11, 6, 20), identity.computeAnchorBlock(frame));
    }

    @Test
    void evenWidthAxisZTieBreaksTowardNegativeZ() {
        TestBootstrap.ensureBootstrapped();
        PortalFrame frame = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);

        assertEquals(new BlockPos(0, 1, 1), identity.computeAnchorBlock(frame));
    }

    @Test
    void anchorIsAlwaysInLowestInteriorRow() {
        TestBootstrap.ensureBootstrapped();
        PortalFrame frame = buildAxisXFrame(new BlockPos(10, 5, 20), 3, 4);

        assertEquals(6, identity.computeAnchorBlock(frame).getY());
    }

    @Test
    void rebuiltPortalStillEnclosingAnchorKeepsIdentity() {
        TestBootstrap.ensureBootstrapped();
        PortalFrame original = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        BlockPos anchor = identity.computeAnchorBlock(original);
        PortalFrame rebuilt = buildAxisZFrame(new BlockPos(0, 0, 0), 3, 4);

        assertTrue(identity.portalEnclosesAnchor(rebuilt, anchor));
    }

    @Test
    void rebuiltPortalElsewhereBecomesDistinctIdentity() {
        TestBootstrap.ensureBootstrapped();
        PortalFrame original = buildAxisZFrame(new BlockPos(0, 0, 0), 2, 3);
        BlockPos anchor = identity.computeAnchorBlock(original);
        PortalFrame rebuiltElsewhere = buildAxisZFrame(new BlockPos(10, 0, 0), 2, 3);

        assertFalse(identity.portalEnclosesAnchor(rebuiltElsewhere, anchor));
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

    private PortalFrame buildAxisXFrame(BlockPos anchor, int width, int height) {
        PortalFrameDetectorTestBlockGetter level = new PortalFrameDetectorTestBlockGetter();
        for (int x = 0; x < width + 2; x++) {
            level.setBlock(anchor.offset(x, 0, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
            level.setBlock(anchor.offset(x, height + 1, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        for (int y = 1; y <= height; y++) {
            level.setBlock(anchor.offset(0, y, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
            level.setBlock(anchor.offset(width + 1, y, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        Optional<PortalFrame> frame = detector.findEmptyFrame(
                level,
                anchor.offset(1, 1, 0),
                Direction.Axis.X,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        return frame.orElseThrow();
    }
}
