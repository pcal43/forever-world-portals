package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.pcal.fwportals.common.portal.PortalAttunementParticles;
import net.pcal.fwportals.common.portal.PortalFrame;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalAttunementParticlesTest {

    @Test
    void convertsAttunementRgbIntoDustParticleOptions() {
        DustParticleOptions dust = PortalAttunementParticles.dustFromRgb(0x3366CC);
        Vector3f color = dust.getColor();

        assertEquals(0x33 / 255.0F, color.x(), 0.0001F);
        assertEquals(0x66 / 255.0F, color.y(), 0.0001F);
        assertEquals(0xCC / 255.0F, color.z(), 0.0001F);
        assertEquals(PortalAttunementParticles.DUST_SCALE, dust.getScale(), 0.0001F);
    }

    @Test
    void spreadsParticleCountsAcrossAnimationTicks() {
        assertEquals(6, PortalAttunementParticles.ANIMATION_TICKS);
        assertEquals(30, sumCountsForAllTicks(PortalAttunementParticles.END_ROD_COUNT));
        assertEquals(36, sumCountsForAllTicks(PortalAttunementParticles.REVERSE_PORTAL_COUNT));
        assertEquals(18, sumCountsForAllTicks(PortalAttunementParticles.DUST_COUNT));
    }

    @Test
    void generatesPortalWideBoundsAcrossInterior() {
        PortalFrame frame = new PortalFrame(
                Direction.Axis.Z,
                new BlockPos(0, 0, 0),
                new BlockPos(0, 1, 1),
                2,
                3,
                List.of(),
                List.of(
                        new BlockPos(0, 1, 1),
                        new BlockPos(0, 1, 2),
                        new BlockPos(0, 2, 1),
                        new BlockPos(0, 2, 2),
                        new BlockPos(0, 3, 1),
                        new BlockPos(0, 3, 2)
                )
        );

        PortalAttunementParticles.PortalInteriorBounds bounds = PortalAttunementParticles.boundsOf(frame);

        assertEquals(0.15, bounds.minX(), 0.0001);
        assertEquals(0.85, bounds.maxX(), 0.0001);
        assertEquals(1.05, bounds.minY(), 0.0001);
        assertEquals(3.95, bounds.maxY(), 0.0001);
        assertEquals(1.0, bounds.minZ(), 0.0001);
        assertEquals(3.0, bounds.maxZ(), 0.0001);
        assertTrue(bounds.center().x() > bounds.minX() && bounds.center().x() < bounds.maxX());
        assertTrue(bounds.center().y() > bounds.minY() && bounds.center().y() < bounds.maxY());
        assertTrue(bounds.center().z() > bounds.minZ() && bounds.center().z() < bounds.maxZ());
    }

    private static int sumCountsForAllTicks(int totalCount) {
        int sum = 0;
        for (int tick = 0; tick < PortalAttunementParticles.ANIMATION_TICKS; tick++) {
            sum += PortalAttunementParticles.countForTick(totalCount, PortalAttunementParticles.ANIMATION_TICKS, tick);
        }
        return sum;
    }
}
