package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.pcal.fwportals.common.TestBootstrap;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalPlacementServiceTest {

    private final PortalPlacementService service = new PortalPlacementService(LogManager.getLogger("test"), new SafeLandingFinder());
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity identity = new PortalIdentity();

    @Test
    void generatedPortalsAlwaysUseStandardDimensions() {
        TestBootstrap.ensureBootstrapped();
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, new BlockPos(100, 70, -50));

        assertEquals(PortalLayout.STANDARD_INTERIOR_WIDTH, layout.width());
        assertEquals(PortalLayout.STANDARD_INTERIOR_HEIGHT, layout.height());
        assertEquals(14, layout.frameBlocks().size());
        assertEquals(6, layout.interiorBlocks().size());
        assertEquals(12, layout.foundationBlocks().size());
        assertEquals(60, layout.clearanceBlocks().size());
    }

    @Test
    void validCandidateCausesNoWorldMutationDuringValidation() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        BlockPos expectedAnchor = new BlockPos(100, 70, -50);
        primeSurface(level, requestedAnchor, 69);
        PortalLayout expectedLayout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, expectedAnchor);

        PortalPlacementService.LayoutSearchResult result = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                withinLayoutBounds(expectedLayout),
                constantSurfaceAirY(70),
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(result.layout().isPresent());
        assertEquals(expectedAnchor, result.layout().orElseThrow().anchorBlock());
        assertEquals(0, level.mutationCount());
    }

    @Test
    void invalidCandidateCausesNoWorldMutation() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);

        PortalPlacementService.LayoutSearchResult layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> true,
                constantSurfaceAirY(70),
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(layout.layout().isEmpty());
        assertEquals(0, level.mutationCount());
    }

    @Test
    void nearbyCandidatesRecomputeSurfaceY() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 0, -50);
        BlockPos requestedColumn = new BlockPos(100, 0, -50);
        BlockPos fallbackColumn = new BlockPos(99, 0, -51);

        primeSurface(level, new BlockPos(requestedColumn.getX(), 0, requestedColumn.getZ()), 69);
        primeSurface(level, new BlockPos(fallbackColumn.getX(), 0, fallbackColumn.getZ()), 75);
        level.setInitialBlock(new BlockPos(requestedColumn.getX(), 71, requestedColumn.getZ()), Blocks.STONE.defaultBlockState());

        List<BlockPos> queriedColumns = new ArrayList<>();
        IntBinaryOperator surfaceYAt = (x, z) -> {
            queriedColumns.add(new BlockPos(x, 0, z));
            if (x == requestedColumn.getX() && z == requestedColumn.getZ()) {
                return 70;
            }
            if (x == fallbackColumn.getX() && z == fallbackColumn.getZ()) {
                return 76;
            }
            return 70;
        };

        PortalLayout layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> true,
                surfaceYAt,
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        ).layout().orElseThrow();

        assertTrue(queriedColumns.contains(requestedColumn));
        assertTrue(queriedColumns.contains(fallbackColumn));
        assertEquals(new BlockPos(fallbackColumn.getX(), 76, fallbackColumn.getZ()), layout.anchorBlock());
    }

    @Test
    void slopedTerrainCanSucceedAtNearbyCandidate() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(0, 0, 0);
        BlockPos higherColumn = new BlockPos(-1, 0, -1);

        primeSurface(level, higherColumn, 83);

        PortalLayout layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> true,
                (x, z) -> (x == higherColumn.getX() && z == higherColumn.getZ()) ? 84 : level.getMinY(),
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        ).layout().orElseThrow();

        assertEquals(new BlockPos(-1, 84, -1), layout.anchorBlock());
    }

    @Test
    void unsafeFoundationBlockRejectsCandidateWithoutMutation() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        BlockPos expectedAnchor = new BlockPos(100, 70, -50);
        primeSurface(level, requestedAnchor, 69);
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, expectedAnchor);
        level.setInitialBlock(layout.foundationBlocks().get(0), Blocks.LAVA.defaultBlockState());

        PortalPlacementService.LayoutSearchResult maybeLayout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                withinLayoutBounds(layout),
                constantSurfaceAirY(70),
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(maybeLayout.layout().isEmpty());
        assertEquals(0, level.mutationCount());
    }

    @Test
    void nonReplaceableClearanceBlockRejectsCandidateWithoutMutation() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        BlockPos expectedAnchor = new BlockPos(100, 70, -50);
        primeSurface(level, requestedAnchor, 69);
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, expectedAnchor);
        level.setInitialBlock(layout.clearanceBlocks().get(0), Blocks.STONE.defaultBlockState());

        PortalPlacementService.LayoutSearchResult maybeLayout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                withinLayoutBounds(layout),
                constantSurfaceAirY(70),
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertTrue(maybeLayout.layout().isEmpty());
        assertEquals(0, level.mutationCount());
    }

    @Test
    void successfullyValidatedCandidateClearsVolumePlacesOneStandardPortalAndStillDetects() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);
        BlockPos expectedAnchor = new BlockPos(100, 70, -50);
        primeSurface(level, requestedAnchor, 69);
        PortalLayout layout = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                withinLayoutBounds(PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, expectedAnchor)),
                constantSurfaceAirY(70),
                requestedAnchor,
                1000,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        ).layout().orElseThrow();

        AtomicInteger writes = new AtomicInteger();
        PortalPlacementService.applyLayout(
                layout,
                Blocks.DIAMOND_BLOCK.defaultBlockState(),
                true,
                false,
                (pos, state) -> {
                    writes.incrementAndGet();
                    level.setBlock(pos, state);
                }
        );

        assertEquals(60 + layout.frameBlocks().size() + layout.interiorBlocks().size(), writes.get());
        assertEquals(expectedAnchor, layout.anchorBlock());
        assertEquals(PortalLayout.STANDARD_INTERIOR_WIDTH, layout.width());
        assertEquals(PortalLayout.STANDARD_INTERIOR_HEIGHT, layout.height());
        assertEquals(expectedAnchor, identity.computeAnchorBlock(layout.frame()));
        assertTrue(level.getBlockState(layout.foundationBlocks().get(0)).is(Blocks.STONE));
        assertTrue(level.getBlockState(layout.foundationBlocks().get(8)).is(Blocks.STONE));
        assertTrue(level.getBlockState(layout.frameBasePos()).is(Blocks.DIAMOND_BLOCK));

        Optional<PortalFrame> detected = detector.findPortalFrame(
                level,
                expectedAnchor,
                Direction.Axis.Z,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        assertTrue(detected.isPresent());
        assertEquals(expectedAnchor, identity.computeAnchorBlock(detected.orElseThrow()));
        assertEquals(layout.frame().width(), detected.orElseThrow().width());
        assertEquals(layout.frame().height(), detected.orElseThrow().height());
    }

    @Test
    void testingBothOrientationsAtOneCandidateConsumesTwoPlacementAttempts() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        BlockPos requestedAnchor = new BlockPos(100, 70, -50);

        PortalPlacementService.LayoutSearchResult result = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> false,
                constantSurfaceAirY(70),
                requestedAnchor,
                2,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertEquals(2, result.placementAttemptsUsed());
        assertEquals(
                PortalPlacementService.LayoutSearchResult.FailureReason.PLACEMENT_ATTEMPTS_EXHAUSTED,
                result.failureReason()
        );
    }

    @Test
    void placementAttemptBudgetResetsForNextBiomeResult() {
        TestBootstrap.ensureBootstrapped();
        MutableTestBlockGetter level = new MutableTestBlockGetter();

        PortalPlacementService.LayoutSearchResult first = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> false,
                constantSurfaceAirY(70),
                new BlockPos(100, 70, -50),
                2,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );
        PortalPlacementService.LayoutSearchResult second = service.findValidLayoutNearAnchor(
                level,
                level.getMinY(),
                pos -> false,
                constantSurfaceAirY(70),
                new BlockPos(200, 70, -50),
                2,
                Blocks.DIAMOND_BLOCK.defaultBlockState()
        );

        assertEquals(2, first.placementAttemptsUsed());
        assertEquals(2, second.placementAttemptsUsed());
        assertEquals(
                PortalPlacementService.LayoutSearchResult.FailureReason.PLACEMENT_ATTEMPTS_EXHAUSTED,
                second.failureReason()
        );
    }

    @Test
    void brokenDestinationPortalUsesWeatheredDeepslateMixNoPortalBlocksAndSubfoundation() {
        TestBootstrap.ensureBootstrapped();
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(Direction.Axis.Z, new BlockPos(100, 70, -50));
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        RandomSource random = RandomSource.create(12345L);

        PortalPlacementService.applyLayout(
                layout,
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                false,
                true,
                level::setBlock
        );
        for (BlockPos framePos : layout.frameBlocks()) {
            level.setBlock(framePos, PortalPlacementService.brokenFrameState(random));
        }

        for (BlockPos framePos : layout.frameBlocks()) {
            BlockState state = level.getBlockState(framePos);
            assertTrue(state.is(Blocks.DEEPSLATE_TILES) || state.is(Blocks.CRACKED_DEEPSLATE_TILES));
        }
        for (BlockPos interiorPos : layout.interiorBlocks()) {
            assertTrue(level.getBlockState(interiorPos).isAir());
        }
        assertEquals(4, layout.subfoundationBlocks().size());
        for (BlockPos subfoundationPos : layout.subfoundationBlocks()) {
            assertTrue(level.getBlockState(subfoundationPos).is(Blocks.CRYING_OBSIDIAN));
        }
    }

    @Test
    void completeDestinationPortalGeneratesDiamondFrameActivePortalAndSubfoundationForBothAxes() {
        TestBootstrap.ensureBootstrapped();
        assertCompletePortalForAxis(Direction.Axis.X);
        assertCompletePortalForAxis(Direction.Axis.Z);
    }

    private static IntBinaryOperator constantSurfaceAirY(int surfaceAirY) {
        return (x, z) -> surfaceAirY;
    }

    private static void assertCompletePortalForAxis(Direction.Axis axis) {
        MutableTestBlockGetter level = new MutableTestBlockGetter();
        PortalLayout layout = PortalLayout.createStandardForAnchorBlock(axis, new BlockPos(0, 70, 0));
        PortalPlacementService.applyLayout(
                layout,
                Blocks.DIAMOND_BLOCK.defaultBlockState(),
                true,
                true,
                level::setBlock
        );

        for (BlockPos framePos : layout.frameBlocks()) {
            assertTrue(level.getBlockState(framePos).is(Blocks.DIAMOND_BLOCK));
        }
        for (BlockPos interiorPos : layout.interiorBlocks()) {
            assertTrue(level.getBlockState(interiorPos).is(Blocks.NETHER_PORTAL));
        }
        assertEquals(4, layout.subfoundationBlocks().size());
        for (BlockPos subfoundationPos : layout.subfoundationBlocks()) {
            assertTrue(level.getBlockState(subfoundationPos).is(Blocks.CRYING_OBSIDIAN));
        }
        BlockPos first = layout.subfoundationBlocks().get(0);
        BlockPos last = layout.subfoundationBlocks().get(3);
        if (axis == Direction.Axis.X) {
            assertEquals(first.getX() + 3, last.getX());
            assertEquals(first.getZ(), last.getZ());
        } else {
            assertEquals(first.getZ() + 3, last.getZ());
            assertEquals(first.getX(), last.getX());
        }
    }

    private static void primeSurface(MutableTestBlockGetter level, BlockPos anchor, int surfaceBlockY) {
        primeSurface(level, anchor, surfaceBlockY, 6);
    }

    private static void primeSurface(MutableTestBlockGetter level, BlockPos anchor, int surfaceBlockY, int radius) {
        for (int x = anchor.getX() - radius; x <= anchor.getX() + radius; x++) {
            for (int z = anchor.getZ() - radius; z <= anchor.getZ() + radius; z++) {
                level.setInitialBlock(new BlockPos(x, surfaceBlockY, z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static Predicate<BlockPos> withinLayoutBounds(PortalLayout layout) {
        return pos -> layout.foundationBlocks().contains(pos) || layout.clearanceBlocks().contains(pos);
    }

    private static final class MutableTestBlockGetter implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private int mutationCount;

        void setInitialBlock(BlockPos pos, BlockState state) {
            blocks.put(pos.immutable(), state);
        }

        void setBlock(BlockPos pos, BlockState state) {
            mutationCount++;
            blocks.put(pos.immutable(), state);
        }

        int mutationCount() {
            return mutationCount;
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
            return getBlockState(pos).getFluidState();
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
