package net.pcal.fwportals.portal;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.attunement.AttunementDefinition;
import net.pcal.fwportals.attunement.AttunementLookup;
import net.pcal.fwportals.attunement.BiomeDestinationTarget;
import net.pcal.fwportals.portal.persistence.PortalRecord;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
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
                new AttunementDefinition("default", null, expected, 0x123456, null),
                Map.of()
        );

        assertEquals(expected, PortalTravelService.defaultFoundingDestinationTarget(lookup));
    }

    @Test
    void pendingPortalUsesStoredAttunementTargetWhenPresent() {
        BiomeDestinationTarget defaultTarget = new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS));
        BiomeDestinationTarget attunedTarget = new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PALE_GARDEN));
        AttunementLookup lookup = AttunementLookup.of(
                new AttunementDefinition("default", null, defaultTarget, 0x111111, null),
                Map.of(net.minecraft.world.item.Items.PALE_OAK_SAPLING, new AttunementDefinition(
                        "pale_garden",
                        net.minecraft.world.item.Items.PALE_OAK_SAPLING,
                        attunedTarget,
                        0x222222,
                        null
                ))
        );

        PortalRecord portal = PortalRecord.pending(
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                net.minecraft.resources.Identifier.parse("minecraft:pale_oak_sapling")
        );

        assertEquals(attunedTarget, PortalTravelService.foundingDestinationTarget(lookup, portal, LogManager.getLogger("test")));
    }

    @Test
    void pendingPortalFallsBackToDefaultWhenNoStoredAttunementExists() {
        BiomeDestinationTarget defaultTarget = new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS));
        AttunementLookup lookup = AttunementLookup.of(
                new AttunementDefinition("default", null, defaultTarget, 0x111111, null),
                Map.of()
        );
        PortalRecord portal = new PortalRecord(
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        assertEquals(defaultTarget, PortalTravelService.foundingDestinationTarget(lookup, portal, LogManager.getLogger("test")));
    }

    @Test
    void resolvingPortalClearsStoredAttunement() {
        PortalRecord pendingPortal = PortalRecord.pending(
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                net.minecraft.resources.Identifier.parse("minecraft:sunflower")
        );

        PortalRecord resolvedPortal = pendingPortal.resolvedTo(Level.OVERWORLD, new net.minecraft.core.BlockPos(100, 70, 100));

        assertEquals(Optional.empty(), resolvedPortal.attunementItemId());
        assertEquals(Optional.of(new net.minecraft.core.BlockPos(100, 70, 100)), resolvedPortal.destinationAnchor());
    }

    @Test
    void pendingPortalKeepsStoredAttunementUntilResolveSucceeds() {
        PortalRecord pendingPortal = PortalRecord.pending(
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                net.minecraft.resources.Identifier.parse("minecraft:sunflower")
        );

        assertEquals(Optional.of(net.minecraft.resources.Identifier.parse("minecraft:sunflower")), pendingPortal.attunementItemId());
        assertEquals(Optional.empty(), pendingPortal.destinationAnchor());
    }
}
