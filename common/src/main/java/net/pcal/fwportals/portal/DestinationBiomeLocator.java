package net.pcal.fwportals.portal;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.pcal.fwportals.attunement.BiomeDestinationTarget;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;
import java.util.function.Predicate;

final class DestinationBiomeLocator {

    private static final int HORIZONTAL_SAMPLE_RESOLUTION_BLOCKS = 32;
    private static final int VERTICAL_SAMPLE_RESOLUTION_BLOCKS = 64;

    Optional<BlockPos> findNearest(
            ServerLevel level,
            BlockPos searchAnchor,
            int searchRadiusBlocks,
            BiomeDestinationTarget target
    ) {
        Predicate<Holder<Biome>> targetBiomePredicate = target.asBiomePredicate();
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
