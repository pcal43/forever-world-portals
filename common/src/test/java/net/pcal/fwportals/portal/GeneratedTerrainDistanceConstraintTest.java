package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeneratedTerrainDistanceConstraintTest {

    @Test
    void rejectsCandidateRegionWithinMinimumChebyshevDistance() {
        GeneratedTerrainDistanceConstraint constraint = GeneratedTerrainDistanceConstraint.ofRegions(
                List.of(GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(0, 0)),
                10000
        );

        String rejectionReason = constraint.rejectionReason(new BlockPos(10000, 70, 0));

        assertEquals("it is too close to previously generated terrain", rejectionReason);
    }

    @Test
    void acceptsCandidateRegionAtMinimumChebyshevDistance() {
        GeneratedTerrainDistanceConstraint constraint = GeneratedTerrainDistanceConstraint.ofRegions(
                List.of(GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(0, 0)),
                10000
        );

        String rejectionReason = constraint.rejectionReason(new BlockPos(10752, 70, 0));

        assertNull(rejectionReason);
    }

    @Test
    void computesRectangleChebyshevDistanceAcrossNegativeCoordinates() {
        GeneratedTerrainDistanceConstraint.RegionRectangle first = GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(-2, -1);
        GeneratedTerrainDistanceConstraint.RegionRectangle second = GeneratedTerrainDistanceConstraint.RegionRectangle.forRegion(-1, 2);

        assertEquals(1025, first.chebyshevDistanceTo(second));
    }
}
