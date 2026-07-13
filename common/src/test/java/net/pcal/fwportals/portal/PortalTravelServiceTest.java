package net.pcal.fwportals.portal;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.common.attunement.AttunementDefinition;
import net.pcal.fwportals.common.attunement.AttunementLookup;
import net.pcal.fwportals.common.attunement.BiomeDestinationTarget;
import net.pcal.fwportals.common.config.Config;
import net.pcal.fwportals.common.portal.PortalTravelService;
import net.pcal.fwportals.common.persistence.PortalRecord;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalTravelServiceTest {

    @Test
    void foundingTravelUsesDefaultAttunementTargetFromLookup() {
        BiomeDestinationTarget expected = new BiomeDestinationTarget(
                Level.OVERWORLD,
                Set.of(Biomes.PLAINS, Biomes.FOREST)
        );
        AttunementDefinition defaultAttunement = new AttunementDefinition("default", null, expected, 0x123456, null);
        AttunementLookup lookup = AttunementLookup.of(defaultAttunement, Map.of());

        assertEquals(expected, PortalTravelService.defaultFoundingDestinationTarget(lookup));
        assertEquals(expected, PortalTravelService.foundingDestinationTarget(defaultAttunement));
    }

    @Test
    void pendingPortalUsesStoredAttunementTargetWhenPresent() {
        BiomeDestinationTarget defaultTarget = new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS));
        BiomeDestinationTarget attunedTarget = new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PALE_GARDEN));
        AttunementDefinition paleGarden = new AttunementDefinition(
                "pale_garden",
                net.minecraft.world.item.Items.PALE_OAK_SAPLING,
                attunedTarget,
                0x222222,
                null
        );
        AttunementLookup lookup = AttunementLookup.of(
                new AttunementDefinition("default", null, defaultTarget, 0x111111, null),
                Map.of(net.minecraft.world.item.Items.PALE_OAK_SAPLING, paleGarden)
        );

        PortalRecord portal = PortalRecord.pending(
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                net.minecraft.resources.Identifier.parse("minecraft:pale_oak_sapling")
        );

        assertEquals(paleGarden, PortalTravelService.foundingAttunement(lookup, portal, LogManager.getLogger("test")));
        assertEquals(attunedTarget, PortalTravelService.foundingDestinationTarget(paleGarden));
        assertEquals("attunement pale_garden", PortalTravelService.foundingTargetLabel(paleGarden));
    }

    @Test
    void pendingPortalFallsBackToDefaultWhenNoStoredAttunementExists() {
        BiomeDestinationTarget defaultTarget = new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS));
        AttunementDefinition defaultAttunement = new AttunementDefinition("default", null, defaultTarget, 0x111111, null);
        AttunementLookup lookup = AttunementLookup.of(defaultAttunement, Map.of());
        PortalRecord portal = new PortalRecord(
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        assertEquals(defaultAttunement, PortalTravelService.foundingAttunement(lookup, portal, LogManager.getLogger("test")));
        assertEquals(defaultTarget, PortalTravelService.foundingDestinationTarget(defaultAttunement));
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

    @Test
    void noneModeBuildsOutboundOnlyRegistration() {
        PortalTravelService.FoundingRegistration registration = PortalTravelService.buildFoundingRegistration(
                Config.DestinationPortalMode.NONE,
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(0, 64, 0),
                Level.OVERWORLD,
                new net.minecraft.core.BlockPos(100, 70, 100)
        );

        assertEquals(Optional.empty(), registration.reversePortal());
        assertEquals(Optional.of(new net.minecraft.core.BlockPos(100, 70, 100)), registration.outboundPortal().destinationAnchor());
    }

    @Test
    void brokenAndCompleteModesBuildLinkedReverseRegistrations() {
        for (Config.DestinationPortalMode mode : new Config.DestinationPortalMode[]{Config.DestinationPortalMode.BROKEN, Config.DestinationPortalMode.COMPLETE}) {
            PortalTravelService.FoundingRegistration registration = PortalTravelService.buildFoundingRegistration(
                    mode,
                    Level.OVERWORLD,
                    new net.minecraft.core.BlockPos(0, 64, 0),
                    Level.OVERWORLD,
                    new net.minecraft.core.BlockPos(100, 70, 100)
            );

            assertTrue(registration.reversePortal().isPresent());
            assertEquals(Optional.of(new net.minecraft.core.BlockPos(100, 70, 100)), registration.outboundPortal().destinationAnchor());
            assertEquals(Optional.of(new net.minecraft.core.BlockPos(0, 64, 0)), registration.reversePortal().orElseThrow().destinationAnchor());
        }
    }
}
