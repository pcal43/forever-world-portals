package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.TestBootstrap;
import net.pcal.fwportals.common.attunement.AttunementDefinition;
import net.pcal.fwportals.common.attunement.AttunementLookup;
import net.pcal.fwportals.common.attunement.BiomeDestinationTarget;
import net.pcal.fwportals.common.portal.PortalAttunementService;
import net.pcal.fwportals.common.persistence.PortalRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalAttunementServiceTest {

    @Test
    void replacingOneAttunementWithAnotherUsesLastAcceptedOffering() {
        TestBootstrap.ensureBootstrapped();
        AttunementLookup lookup = lookupWithTwoAttunements();
        PortalRecord existing = PortalRecord.pending(Level.OVERWORLD, new BlockPos(0, 64, 0), Identifier.parse("minecraft:sunflower"));

        PortalAttunementService.AcceptedOffering offering = PortalAttunementService.resolveAcceptedOffering(
                lookup,
                existing,
                Level.OVERWORLD,
                existing.anchor(),
                Items.ALLIUM
        ).orElseThrow();

        assertEquals(Optional.of(Identifier.parse("minecraft:allium")), offering.updatedPortal().attunementItemId());
    }

    @Test
    void rejectsUnrecognizedItems() {
        TestBootstrap.ensureBootstrapped();
        AttunementLookup lookup = lookupWithTwoAttunements();

        assertTrue(PortalAttunementService.resolveAcceptedOffering(
                lookup,
                null,
                Level.OVERWORLD,
                new BlockPos(0, 64, 0),
                Items.STICK
        ).isEmpty());
    }

    @Test
    void ignoresOfferingsOnResolvedPortals() {
        TestBootstrap.ensureBootstrapped();
        AttunementLookup lookup = lookupWithTwoAttunements();
        PortalRecord resolved = PortalRecord.resolved(
                Level.OVERWORLD,
                new BlockPos(0, 64, 0),
                Level.OVERWORLD,
                new BlockPos(100, 70, 100)
        );

        assertTrue(PortalAttunementService.resolveAcceptedOffering(
                lookup,
                resolved,
                Level.OVERWORLD,
                resolved.anchor(),
                Items.SUNFLOWER
        ).isEmpty());
    }

    @Test
    void consumesExactlyOneItemFromStack() {
        TestBootstrap.ensureBootstrapped();
        assertEquals(2, PortalAttunementService.remainingCountAfterAcceptedOffering(3));
    }

    private static AttunementLookup lookupWithTwoAttunements() {
        return AttunementLookup.of(
                new AttunementDefinition(
                        "default",
                        null,
                        new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS)),
                        0x111111,
                        null
                ),
                Map.of(
                        Items.SUNFLOWER,
                        new AttunementDefinition(
                                "sunflower_plains",
                                Items.SUNFLOWER,
                                new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.SUNFLOWER_PLAINS)),
                                0x222222,
                                null
                        ),
                        Items.ALLIUM,
                        new AttunementDefinition(
                                "flower_forest",
                                Items.ALLIUM,
                                new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.FLOWER_FOREST)),
                                0x333333,
                                null
                        )
                )
        );
    }
}
