package net.pcal.fwportals.portal;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

final class DestinationBiomeLocator {

    private static final int HORIZONTAL_SAMPLE_RESOLUTION_BLOCKS = 32;
    private static final int VERTICAL_SAMPLE_RESOLUTION_BLOCKS = 64;
    private static final Set<ResourceKey<Biome>> TARGET_BIOMES = Set.of(
            Biomes.SUNFLOWER_PLAINS,
            Biomes.FLOWER_FOREST,
            Biomes.PALE_GARDEN
    );

    private final Predicate<Holder<Biome>> targetBiomePredicate = holder -> TARGET_BIOMES.stream().anyMatch(holder::is);

    Optional<BlockPos> findNearest(ServerLevel level, BlockPos searchAnchor, int searchRadiusBlocks) {
        Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(
                targetBiomePredicate,
                searchAnchor,
                searchRadiusBlocks,
                HORIZONTAL_SAMPLE_RESOLUTION_BLOCKS,
                VERTICAL_SAMPLE_RESOLUTION_BLOCKS
        );
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst().immutable());
    }
}
