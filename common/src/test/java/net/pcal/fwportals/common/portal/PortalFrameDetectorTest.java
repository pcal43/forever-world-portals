package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.pcal.fwportals.common.TestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalFrameDetectorTest {

    private final PortalFrameDetector detector = new PortalFrameDetector();

    @Test
    void detectsValidDiamondFrame() {
        TestBootstrap.ensureBootstrapped();
        TestBlockGetter level = new TestBlockGetter();
        buildAxisZFrame(level, new BlockPos(0, 0, 0), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2, 3);

        Optional<PortalFrame> frame = detector.findEmptyFrame(
                level,
                new BlockPos(0, 1, 1),
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(frame.isPresent());
        assertEquals(Direction.Axis.Z, frame.get().axis());
        assertEquals(2, frame.get().width());
        assertEquals(3, frame.get().height());
        assertEquals(new BlockPos(0, 0, 0), frame.get().frameBasePos());
        assertEquals(14, frame.get().frameBlocks().size());
        assertEquals(6, frame.get().interiorBlocks().size());
    }

    @Test
    void rejectsInvalidDiamondFrame() {
        TestBootstrap.ensureBootstrapped();
        TestBlockGetter level = new TestBlockGetter();
        buildAxisZFrame(level, new BlockPos(0, 0, 0), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2, 3);
        level.setBlock(new BlockPos(0, 4, 1), Blocks.AIR.defaultBlockState());

        Optional<PortalFrame> frame = detector.findEmptyFrame(
                level,
                new BlockPos(0, 1, 1),
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertFalse(frame.isPresent());
    }

    @Test
    void calculatesCanonicalAnchorFromFrameGeometry() {
        TestBootstrap.ensureBootstrapped();
        TestBlockGetter level = new TestBlockGetter();
        buildAxisXFrame(level, new BlockPos(10, 5, 20), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2, 3);

        Optional<PortalFrame> frame = detector.findEmptyFrame(
                level,
                new BlockPos(11, 6, 20),
                Direction.Axis.X,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(frame.isPresent());
        assertEquals(new BlockPos(10, 5, 20), frame.get().frameBasePos());
    }

    @Test
    void ignoresOrdinaryObsidianPortals() {
        TestBootstrap.ensureBootstrapped();
        TestBlockGetter level = new TestBlockGetter();
        buildAxisZFrame(level, new BlockPos(0, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 2, 3);

        Optional<PortalFrame> frame = detector.findEmptyFrame(
                level,
                new BlockPos(0, 1, 1),
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertFalse(frame.isPresent());
    }

    private static void buildAxisZFrame(TestBlockGetter level, BlockPos anchor, BlockState frameState, int width, int height) {
        for (int z = 0; z < width + 2; z++) {
            level.setBlock(anchor.offset(0, 0, z), frameState);
            level.setBlock(anchor.offset(0, height + 1, z), frameState);
        }
        for (int y = 1; y <= height; y++) {
            level.setBlock(anchor.offset(0, y, 0), frameState);
            level.setBlock(anchor.offset(0, y, width + 1), frameState);
        }
    }

    private static void buildAxisXFrame(TestBlockGetter level, BlockPos anchor, BlockState frameState, int width, int height) {
        for (int x = 0; x < width + 2; x++) {
            level.setBlock(anchor.offset(x, 0, 0), frameState);
            level.setBlock(anchor.offset(x, height + 1, 0), frameState);
        }
        for (int y = 1; y <= height; y++) {
            level.setBlock(anchor.offset(0, y, 0), frameState);
            level.setBlock(anchor.offset(width + 1, y, 0), frameState);
        }
    }

    private static final class TestBlockGetter implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        void setBlock(BlockPos pos, BlockState state) {
            blocks.put(pos.immutable(), state);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}
