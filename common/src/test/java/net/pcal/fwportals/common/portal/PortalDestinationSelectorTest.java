package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.common.TestBootstrap;
import net.pcal.fwportals.common.attunement.BiomeDestinationTarget;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalDestinationSelectorTest {

    @Test
    void producesExactInitialSpiralSequence() {
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10);

        List<BlockPos> actual = java.util.stream.Stream.generate(iterator::next)
                .limit(17)
                .toList();

        assertEquals(List.of(
                new BlockPos(0, 0, 0),
                new BlockPos(-10, 0, 0),
                new BlockPos(-10, 0, 10),
                new BlockPos(0, 0, 10),
                new BlockPos(10, 0, 10),
                new BlockPos(10, 0, 0),
                new BlockPos(10, 0, -10),
                new BlockPos(0, 0, -10),
                new BlockPos(-10, 0, -10),
                new BlockPos(-20, 0, 0),
                new BlockPos(-20, 0, 10),
                new BlockPos(-20, 0, 20),
                new BlockPos(-10, 0, 20),
                new BlockPos(0, 0, 20),
                new BlockPos(10, 0, 20),
                new BlockPos(20, 0, 20),
                new BlockPos(20, 0, 10)
        ), actual);
    }

    @Test
    void producesNoDuplicateCoordinatesOnGrid() {
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10);
        Set<BlockPos> seen = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            BlockPos pos = iterator.next();
            assertTrue(seen.add(pos), "duplicate spiral coordinate: " + pos);
            assertEquals(0, Math.floorMod(pos.getX(), 10));
            assertEquals(0, Math.floorMod(pos.getZ(), 10));
        }
    }

    @Test
    void derivesBiomeSearchRadiusFromSpacing() {
        assertEquals(7072, PortalDestinationSelector.deriveBiomeSearchRadiusBlocks(10000));
    }

    @Test
    void derivedBiomeSearchRadiusReachesGridCellCorner() {
        int spacing = 10000;
        int radius = PortalDestinationSelector.deriveBiomeSearchRadiusBlocks(spacing);
        long radiusSquaredTimesTwo = 2L * radius * radius;
        long spacingSquared = 1L * spacing * spacing;
        long previousRadiusSquaredTimesTwo = 2L * (radius - 1L) * (radius - 1L);

        assertTrue(radiusSquaredTimesTwo >= spacingSquared);
        assertTrue(previousRadiusSquaredTimesTwo < spacingSquared);
    }

    @Test
    void spiralRejectionConsumesSpiralBudgetButNotBiomeBudget() {
        GeneratedTerrainDistanceConstraint constraint = GeneratedTerrainDistanceConstraint.ofRegions(
                List.of(GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(0, 0)),
                10000
        );
        PortalDestinationSelector.SearchContext context = searchContext(4, 4, 10000, List.of(constraint));
        AtomicInteger biomeLookups = new AtomicInteger();

        Optional<BlockPos> result = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> {
                    biomeLookups.incrementAndGet();
                    return Optional.of(anchor);
                },
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );

        assertTrue(result.isEmpty());
        assertEquals(1, context.searchBudget().spiralPositionsExamined());
        assertEquals(0, context.searchBudget().biomeSearchesPerformed());
        assertEquals(0, biomeLookups.get());
    }

    @Test
    void biomeLookupReturningNoResultStillConsumesOneBiomeSearchAttempt() {
        PortalDestinationSelector.SearchContext context = searchContext(4, 4, 10000, List.of());
        AtomicInteger biomeLookups = new AtomicInteger();

        Optional<BlockPos> result = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> {
                    biomeLookups.incrementAndGet();
                    return Optional.empty();
                },
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );

        assertTrue(result.isEmpty());
        assertEquals(1, context.searchBudget().spiralPositionsExamined());
        assertEquals(1, context.searchBudget().biomeSearchesPerformed());
        assertEquals(1, biomeLookups.get());
    }

    @Test
    void returnedBiomePositionIsCheckedIndependentlyAgainstConstraint() {
        GeneratedTerrainDistanceConstraint constraint = GeneratedTerrainDistanceConstraint.ofRegions(
                List.of(GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(21, 21)),
                10000
        );
        PortalDestinationSelector.SearchContext context = searchContext(4, 4, 10000, List.of(constraint));

        Optional<BlockPos> result = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> Optional.of(new BlockPos(21 * 512, 0, 21 * 512)),
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );

        assertTrue(result.isEmpty());
        assertEquals(1, context.searchBudget().spiralPositionsExamined());
        assertEquals(1, context.searchBudget().biomeSearchesPerformed());
    }

    @Test
    void searchStopsWhenSpiralBudgetIsExhausted() {
        PortalDestinationSelector.SearchContext context = searchContext(1, 4, 10000, List.of());

        Optional<BlockPos> first = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> Optional.empty(),
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );
        Optional<BlockPos> second = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> Optional.of(anchor),
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertEquals(1, context.searchBudget().spiralPositionsExamined());
        assertEquals(1, context.searchBudget().biomeSearchesPerformed());
        assertEquals(PortalDestinationSelector.ExhaustionReason.MAXIMUM_SPIRAL_SEARCH_POSITIONS,
                context.searchBudget().exhaustionReason().orElseThrow());
    }

    @Test
    void searchStopsWhenBiomeBudgetIsExhaustedWithoutConsumingAnotherSpiralPosition() {
        PortalDestinationSelector.SearchContext context = searchContext(4, 1, 10000, List.of());

        Optional<BlockPos> first = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> Optional.empty(),
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );
        Optional<BlockPos> second = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> Optional.of(anchor),
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertEquals(1, context.searchBudget().spiralPositionsExamined());
        assertEquals(1, context.searchBudget().biomeSearchesPerformed());
        assertEquals(PortalDestinationSelector.ExhaustionReason.MAXIMUM_BIOME_SEARCHES,
                context.searchBudget().exhaustionReason().orElseThrow());
    }

    @Test
    void successfulBiomeLookupIncrementsCountersExactlyOnce() {
        PortalDestinationSelector.SearchContext context = searchContext(4, 4, 10000, List.of());
        AtomicInteger biomeLookups = new AtomicInteger();

        Optional<BlockPos> result = PortalDestinationSelector.findNextBiomeSearchAnchor(
                context,
                anchor -> {
                    biomeLookups.incrementAndGet();
                    return Optional.of(anchor.offset(64, 0, 96));
                },
                ignored -> {
                },
                "minecraft:overworld",
                "default destination target"
        );

        assertEquals(Optional.of(new BlockPos(64, 0, 96)), result);
        assertEquals(1, context.searchBudget().spiralPositionsExamined());
        assertEquals(1, context.searchBudget().biomeSearchesPerformed());
        assertEquals(1, biomeLookups.get());
    }

    private static PortalDestinationSelector.SearchContext searchContext(
            int maximumSpiralSearchPositions,
            int maximumBiomeSearches,
            int spiralSpacingBlocks,
            List<DestinationConstraint> constraints
    ) {
        TestBootstrap.ensureBootstrapped();
        return new PortalDestinationSelector.SearchContext(
                null,
                BlockPos.ZERO,
                new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS)),
                "default destination target",
                new PortalDestinationSelector.SpiralAnchorIterator(spiralSpacingBlocks),
                PortalDestinationSelector.deriveBiomeSearchRadiusBlocks(spiralSpacingBlocks),
                new PortalDestinationSelector.SearchBudget(maximumSpiralSearchPositions, maximumBiomeSearches),
                constraints
        );
    }
}
