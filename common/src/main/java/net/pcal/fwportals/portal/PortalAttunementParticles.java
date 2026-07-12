package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class PortalAttunementParticles {

    static final int ANIMATION_TICKS = 6;
    static final int END_ROD_COUNT = 30;
    static final int REVERSE_PORTAL_COUNT = 36;
    static final int DUST_COUNT = 18;
    static final float DUST_SCALE = 0.9F;

    private PortalAttunementParticles() {
    }

    public static void emitAcceptedOfferingTick(ServerLevel level, PortalInteriorBounds bounds, int colorRgb, int tickIndex) {
        int emittedEndRod = countForTick(END_ROD_COUNT, ANIMATION_TICKS, tickIndex);
        int emittedReversePortal = countForTick(REVERSE_PORTAL_COUNT, ANIMATION_TICKS, tickIndex);
        int emittedDust = countForTick(DUST_COUNT, ANIMATION_TICKS, tickIndex);

        spawnAcrossInterior(level, bounds, ParticleTypes.END_ROD, emittedEndRod, 0.01);
        spawnAcrossInterior(level, bounds, ParticleTypes.REVERSE_PORTAL, emittedReversePortal, 0.015);
        spawnAcrossInterior(level, bounds, dustFromRgb(colorRgb), emittedDust, 0.004);
    }

    static DustParticleOptions dustFromRgb(int colorRgb) {
        return new DustParticleOptions(colorRgb, DUST_SCALE);
    }

    static PortalInteriorBounds boundsOf(ForeverWorldPortalFrame frame) {
        return PortalInteriorBounds.of(frame);
    }

    private static void spawnAcrossInterior(
            ServerLevel level,
            PortalInteriorBounds bounds,
            ParticleOptions particle,
            int count,
            double speed
    ) {
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double x = randomBetween(level, bounds.minX(), bounds.maxX());
            double y = randomBetween(level, bounds.minY(), bounds.maxY());
            double z = randomBetween(level, bounds.minZ(), bounds.maxZ());
            level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, speed);
        }
    }

    static int countForTick(int totalCount, int totalTicks, int tickIndex) {
        int emittedBefore = totalCount * tickIndex / totalTicks;
        int emittedAfter = totalCount * (tickIndex + 1) / totalTicks;
        return emittedAfter - emittedBefore;
    }

    private static double randomBetween(ServerLevel level, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + level.getRandom().nextDouble() * (max - min);
    }

    record PortalInteriorCenter(double x, double y, double z) {
    }

    record PortalInteriorBounds(
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ,
            PortalInteriorCenter center
    ) {
        static PortalInteriorBounds of(ForeverWorldPortalFrame frame) {
            List<BlockPos> interiorBlocks = frame.interiorBlocks();
            int minBlockX = interiorBlocks.stream().mapToInt(BlockPos::getX).min().orElseThrow();
            int maxBlockX = interiorBlocks.stream().mapToInt(BlockPos::getX).max().orElseThrow();
            int minBlockY = interiorBlocks.stream().mapToInt(BlockPos::getY).min().orElseThrow();
            int maxBlockY = interiorBlocks.stream().mapToInt(BlockPos::getY).max().orElseThrow();
            int minBlockZ = interiorBlocks.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
            int maxBlockZ = interiorBlocks.stream().mapToInt(BlockPos::getZ).max().orElseThrow();

            double minX = minBlockX + (frame.axis() == Direction.Axis.X ? 0.0 : 0.15);
            double maxX = maxBlockX + (frame.axis() == Direction.Axis.X ? 1.0 : 0.85);
            double minZ = minBlockZ + (frame.axis() == Direction.Axis.Z ? 0.0 : 0.15);
            double maxZ = maxBlockZ + (frame.axis() == Direction.Axis.Z ? 1.0 : 0.85);
            double minY = minBlockY + 0.05;
            double maxY = maxBlockY + 0.95;

            return new PortalInteriorBounds(
                    minX,
                    maxX,
                    minY,
                    maxY,
                    minZ,
                    maxZ,
                    new PortalInteriorCenter(
                            (minX + maxX) * 0.5,
                            (minY + maxY) * 0.5,
                            (minZ + maxZ) * 0.5
                    )
            );
        }
    }
}
