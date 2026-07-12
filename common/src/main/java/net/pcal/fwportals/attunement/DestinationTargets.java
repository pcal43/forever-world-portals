package net.pcal.fwportals.attunement;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Set;

public final class DestinationTargets {

    private static final BiomeDestinationTarget DEFAULT_TARGET = new BiomeDestinationTarget(
            Level.OVERWORLD,
            Set.of(
                    Biomes.SUNFLOWER_PLAINS,
                    Biomes.FLOWER_FOREST,
                    Biomes.PALE_GARDEN
            )
    );

    public static BiomeDestinationTarget defaultBiomeTarget() {
        return DEFAULT_TARGET;
    }

    public static String defaultTargetLabel() {
        return "default destination target";
    }

    public static String targetLabel(AttunementDefinition attunementDefinition) {
        return "attunement " + attunementDefinition.id();
    }

    public static String targetLabel(BiomeDestinationTarget target) {
        if (DEFAULT_TARGET.equals(target)) {
            return defaultTargetLabel();
        }
        Set<ResourceKey<Biome>> biomes = target.biomes();
        if (biomes.isEmpty()) {
            return "custom biome target";
        }
        return biomes.iterator().next().identifier().toString();
    }

    private DestinationTargets() {
    }
}
