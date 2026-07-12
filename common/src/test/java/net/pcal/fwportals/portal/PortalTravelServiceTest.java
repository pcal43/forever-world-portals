package net.pcal.fwportals.portal;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.attunement.AttunementDefinition;
import net.pcal.fwportals.attunement.AttunementLookup;
import net.pcal.fwportals.attunement.BiomeDestinationTarget;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalTravelServiceTest {

    @Test
    void foundingTravelUsesDefaultAttunementTargetFromLookup() {
        BiomeDestinationTarget expected = new BiomeDestinationTarget(
                Level.OVERWORLD,
                Set.of(Biomes.PLAINS, Biomes.FOREST)
        );
        AttunementLookup lookup = AttunementLookup.of(
                new AttunementDefinition("default", null, expected),
                Map.of()
        );

        assertEquals(expected, PortalTravelService.defaultFoundingDestinationTarget(lookup));
    }
}
