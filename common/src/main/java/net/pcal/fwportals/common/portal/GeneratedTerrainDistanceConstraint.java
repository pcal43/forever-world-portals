package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeneratedTerrainDistanceConstraint implements DestinationConstraint {

    private static final int REGION_SIZE_BLOCKS = 512;
    private static final Pattern REGION_FILE_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

    private final List<RegionRectangle> occupiedRegions;
    private final int minimumGeneratedTerrainDistanceBlocks;

    private GeneratedTerrainDistanceConstraint(List<RegionRectangle> occupiedRegions, int minimumGeneratedTerrainDistanceBlocks) {
        this.occupiedRegions = List.copyOf(occupiedRegions);
        this.minimumGeneratedTerrainDistanceBlocks = minimumGeneratedTerrainDistanceBlocks;
    }

    public static GeneratedTerrainDistanceConstraint snapshot(
            ServerLevel level,
            int minimumGeneratedTerrainDistanceBlocks,
            Logger logger
    ) {
        List<RegionRectangle> occupiedRegions = new ArrayList<>();
        Path regionDirectory = DimensionType.getStorageFolder(
                level.dimension(),
                level.getServer().getWorldPath(LevelResource.ROOT)
        ).resolve("region");

        if (!Files.isDirectory(regionDirectory)) {
            return new GeneratedTerrainDistanceConstraint(occupiedRegions, minimumGeneratedTerrainDistanceBlocks);
        }

        try (var paths = Files.list(regionDirectory)) {
            paths.forEach(path -> parseRegionFile(path).ifPresent(occupiedRegions::add));
        } catch (IOException ex) {
            logger.warn(
                    "[fwportals] Failed to snapshot generated terrain in {}: {}",
                    regionDirectory,
                    ex.getMessage()
            );
        }

        return new GeneratedTerrainDistanceConstraint(occupiedRegions, minimumGeneratedTerrainDistanceBlocks);
    }

    @Override
    public @Nullable String rejectionReason(BlockPos candidateAnchor) {
        RegionRectangle candidateRegion = RegionRectangle.forBlock(candidateAnchor);
        for (RegionRectangle occupiedRegion : occupiedRegions) {
            if (candidateRegion.chebyshevDistanceTo(occupiedRegion) < minimumGeneratedTerrainDistanceBlocks) {
                return "it is too close to previously generated terrain";
            }
        }
        return null;
    }

    private static java.util.Optional<RegionRectangle> parseRegionFile(Path path) {
        Matcher matcher = REGION_FILE_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return java.util.Optional.empty();
        }

        int regionX = Integer.parseInt(matcher.group(1));
        int regionZ = Integer.parseInt(matcher.group(2));
        return java.util.Optional.of(RegionRectangle.forRegion(regionX, regionZ));
    }

    record RegionRectangle(int minX, int maxX, int minZ, int maxZ) {

        static RegionRectangle forBlock(BlockPos pos) {
            return forRegion(Math.floorDiv(pos.getX(), REGION_SIZE_BLOCKS), Math.floorDiv(pos.getZ(), REGION_SIZE_BLOCKS));
        }

        static RegionRectangle forRegion(int regionX, int regionZ) {
            int minX = regionX * REGION_SIZE_BLOCKS;
            int minZ = regionZ * REGION_SIZE_BLOCKS;
            return new RegionRectangle(
                    minX,
                    minX + REGION_SIZE_BLOCKS - 1,
                    minZ,
                    minZ + REGION_SIZE_BLOCKS - 1
            );
        }

        int chebyshevDistanceTo(RegionRectangle other) {
            return Math.max(
                    axisDistance(minX, maxX, other.minX, other.maxX),
                    axisDistance(minZ, maxZ, other.minZ, other.maxZ)
            );
        }

        private static int axisDistance(int firstMin, int firstMax, int secondMin, int secondMax) {
            if (firstMax < secondMin) {
                return secondMin - firstMax;
            }
            if (secondMax < firstMin) {
                return firstMin - secondMax;
            }
            return 0;
        }
    }

    /**
     * TEST ONLY.  Creates a constraint from an explicit region snapshot for deterministic tests.
     */
    static GeneratedTerrainDistanceConstraint ofRegions(
            List<RegionRectangle> occupiedRegions,
            int minimumGeneratedTerrainDistanceBlocks
    ) {
        return new GeneratedTerrainDistanceConstraint(occupiedRegions, minimumGeneratedTerrainDistanceBlocks);
    }
}
