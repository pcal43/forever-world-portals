package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void completesFirstAndSecondOrbits() {
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10);

        List<BlockPos> actual = java.util.stream.Stream.generate(iterator::next)
                .limit(25)
                .toList();

        assertEquals(new BlockPos(-10, 0, -10), actual.get(8));
        assertEquals(new BlockPos(-20, 0, -20), actual.get(23));
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
    void producesCoordinatesLazily() {
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10);

        assertEquals(new BlockPos(0, 0, 0), iterator.next());
        assertEquals(new BlockPos(-10, 0, 0), iterator.next());
        assertEquals(new BlockPos(-10, 0, 10), iterator.next());
    }

    @Test
    void usesBiomeSearchResultFromEligibleSpiralAnchor() {
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10000);

        Optional<BlockPos> result = PortalDestinationSelector.findNextBiomeSearchAnchor(
                iterator,
                List.of(),
                spiralAnchor -> Optional.of(new BlockPos(spiralAnchor.getX() + 64, 0, spiralAnchor.getZ() + 96)),
                ignored -> {
                },
                "minecraft:overworld"
        );

        assertEquals(Optional.of(new BlockPos(64, 0, 96)), result);
    }

    @Test
    void continuesWhenOneSpiralAnchorHasNoMatchingBiome() {
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10000);

        Optional<BlockPos> first = PortalDestinationSelector.findNextBiomeSearchAnchor(
                iterator,
                List.of(),
                spiralAnchor -> spiralAnchor.equals(new BlockPos(0, 0, 0))
                        ? Optional.empty()
                        : Optional.of(spiralAnchor.offset(128, 0, 128)),
                ignored -> {
                },
                "minecraft:overworld"
        );
        Optional<BlockPos> second = PortalDestinationSelector.findNextBiomeSearchAnchor(
                iterator,
                List.of(),
                spiralAnchor -> Optional.of(spiralAnchor.offset(128, 0, 128)),
                ignored -> {
                },
                "minecraft:overworld"
        );

        assertTrue(first.isEmpty());
        assertEquals(Optional.of(new BlockPos(-9872, 0, 128)), second);
    }

    @Test
    void rejectsBiomeResultTooCloseToFrozenSnapshotAndAcceptsNextEligibleAnchor() {
        GeneratedTerrainDistanceConstraint constraint = GeneratedTerrainDistanceConstraint.ofRegions(
                List.of(GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(0, 0)),
                10000
        );
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10000);

        Optional<BlockPos> accepted = Optional.empty();
        for (int i = 0; i < 12 && accepted.isEmpty(); i++) {
            accepted = PortalDestinationSelector.findNextBiomeSearchAnchor(
                    iterator,
                    List.of(constraint),
                    spiralAnchor -> spiralAnchor.equals(new BlockPos(-20000, 0, 0))
                            ? Optional.of(new BlockPos(10000, 0, 0))
                            : Optional.of(spiralAnchor.offset(1024, 0, 1024)),
                    ignored -> {
                    },
                    "minecraft:overworld"
            );
        }

        assertEquals(Optional.of(new BlockPos(-18976, 0, 11024)), accepted);
    }

    @Test
    void returnsNoBiomeAnchorWhenConstraintRejectsCurrentSpiralAnchor() {
        GeneratedTerrainDistanceConstraint constraint = GeneratedTerrainDistanceConstraint.ofRegions(
                List.of(GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(0, 0)),
                10000
        );
        PortalDestinationSelector.SpiralAnchorIterator iterator = new PortalDestinationSelector.SpiralAnchorIterator(10000);

        Optional<BlockPos> result = PortalDestinationSelector.findNextBiomeSearchAnchor(
                iterator,
                List.of(constraint),
                spiralAnchor -> Optional.of(spiralAnchor),
                ignored -> {
                },
                "minecraft:overworld"
        );

        assertTrue(result.isEmpty());
    }
}
