package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.pcal.fwportals.TestBootstrap;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalPlacementServiceTest {

    private final PortalPlacementService service = new PortalPlacementService(LogManager.getLogger("test"), new SafeLandingFinder());
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity identity = new PortalIdentity();

    @Test
    void validCandidateCausesNoWorldMutationDuringValidation() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();

        Optional<PortalLayout> layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> true,
                new BlockPos(100, 70, -50),
                2,
                3,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(layout.isPresent());
        assertEquals(0, level.blockCount());
    }

    @Test
    void invalidCandidateCausesNoWorldMutation() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        level.setBlock(requestedAnchor, Blocks.LAVA.defaultBlockState());

        Optional<PortalLayout> layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> false,
                requestedAnchor,
                2,
                3,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(layout.isEmpty());
        assertEquals(1, level.blockCount());
        assertTrue(level.getBlockState(requestedAnchor).is(Blocks.LAVA));
    }

    @Test
    void landingPositionIsValidatedBeforePlacement() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();

        Optional<PortalLayout> layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> true,
                new BlockPos(0, level.getMinY() + 1, 0),
                2,
                3,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(layout.isEmpty());
        assertEquals(0, level.blockCount());
    }

    @Test
    void successfullyValidatedCandidateIsPlacedOnceAndStillDetected() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        PortalLayout layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> true,
                requestedAnchor,
                2,
                3,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        ).orElseThrow();

        AtomicInteger writes = new AtomicInteger();
        PortalPlacementService.applyLayout(
                layout,
                Blocks.DIAMOND_BLOCK.defaultBlockState(),
                (pos, state) -> {
                    writes.incrementAndGet();
                    level.setBlock(pos, state);
                }
        );

        assertEquals(layout.frameBlocks().size() + layout.interiorBlocks().size(), writes.get());
        assertEquals(requestedAnchor, layout.anchorBlock());

        Optional<ForeverWorldPortalFrame> detected = detector.findPortalFrame(
                level,
                requestedAnchor,
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        assertTrue(detected.isPresent());
        assertEquals(requestedAnchor, identity.computeAnchorBlock(detected.orElseThrow()));
    }

    private static final class MutableTestBlockGetter implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        void setBlock(BlockPos pos, BlockState state) {
            blocks.put(pos.immutable(), state);
        }

        int blockCount() {
            return blocks.size();
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
