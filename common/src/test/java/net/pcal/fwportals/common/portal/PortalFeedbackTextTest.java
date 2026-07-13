package net.pcal.fwportals.common.portal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.common.attunement.AttunementDefinition;
import net.pcal.fwportals.common.attunement.BiomeDestinationTarget;
import net.pcal.fwportals.common.portal.PortalFeedbackText;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PortalFeedbackTextTest {

    @Test
    void formatsLogicalIdsIntoReadableNames() {
        assertEquals("Cherry Grove", PortalFeedbackText.readableAttunementName("cherry_grove"));
        assertEquals("Old Growth Taiga", PortalFeedbackText.readableAttunementName("old_growth_taiga"));
        assertEquals("Cherry Grove", PortalFeedbackText.readableAttunementName("forever_world_portals:cherry_grove"));
    }

    @Test
    void usesSeekingNewFrontierForDefaultAttunement() {
        AttunementDefinition defaultAttunement = new AttunementDefinition(
                "default",
                null,
                new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.PLAINS)),
                0x111111,
                null
        );

        assertEquals("Seeking a new frontier...", PortalFeedbackText.seekingFrontierMessage(defaultAttunement).getString());
        assertTranslationKey(PortalFeedbackText.seekingFrontierMessage(defaultAttunement), "message.fwportals.seeking_new_frontier");
    }

    @Test
    void usesReadableAttunementNameForNonDefaultMessages() {
        AttunementDefinition cherryGrove = new AttunementDefinition(
                "cherry_grove",
                net.minecraft.world.item.Items.CHERRY_SAPLING,
                new BiomeDestinationTarget(Level.OVERWORLD, Set.of(Biomes.CHERRY_GROVE)),
                0x222222,
                null
        );

        assertEquals("Portal attuned to Cherry Grove", PortalFeedbackText.acceptedAttunementMessage(cherryGrove).getString());
        assertEquals("Seeking a Cherry Grove frontier...", PortalFeedbackText.seekingFrontierMessage(cherryGrove).getString());
        assertTranslationKey(PortalFeedbackText.acceptedAttunementMessage(cherryGrove), "message.fwportals.attuned");
        assertTranslationKey(PortalFeedbackText.seekingFrontierMessage(cherryGrove), "message.fwportals.seeking_frontier");
    }

    private static void assertTranslationKey(Component component, String key) {
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, component.getContents());
        assertEquals(key, contents.getKey());
    }
}
