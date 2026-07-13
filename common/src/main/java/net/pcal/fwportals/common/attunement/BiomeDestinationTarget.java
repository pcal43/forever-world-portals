package net.pcal.fwportals.common.attunement;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;
import java.util.function.Predicate;

public record BiomeDestinationTarget(
        ResourceKey<Level> dimension,
        Set<ResourceKey<Biome>> biomes
) implements DestinationTarget {

    public BiomeDestinationTarget {
        biomes = Set.copyOf(biomes);
    }

    public Predicate<Holder<Biome>> asBiomePredicate() {
        return holder -> biomes.stream().anyMatch(holder::is);
    }
}
