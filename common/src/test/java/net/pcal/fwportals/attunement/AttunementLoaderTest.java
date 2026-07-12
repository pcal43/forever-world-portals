package net.pcal.fwportals.attunement;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.pcal.fwportals.TestBootstrap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttunementLoaderTest {

    @Test
    void loadsBuiltInDefaultDefinitionFromResource() throws IOException {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, builtInAttunementsJson())),
                registryLookup()
        );

        assertEquals("default", lookup.defaultAttunement().id());
        assertEquals(Level.OVERWORLD, lookup.defaultTarget().dimension());
        assertEquals(0xF2B84B, lookup.defaultAttunement().colorRgb());
        assertEquals(Identifier.parse("minecraft:happy_villager"), lookup.defaultAttunement().particleId());
    }

    @Test
    void failsWhenNoEffectiveDefaultExists() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "sunflower_plains": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "color": "#f7d13d",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertEquals("Missing required 'default' attunement.", ex.getMessage());
    }

    @Test
    void failsWhenDefaultContainsItem() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "color": "#f2b84b",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertEquals("The 'default' attunement must not specify an item.", ex.getMessage());
    }

    @Test
    void failsWhenNonDefaultOmitsItem() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "color": "#f2b84b",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
                                  "desert": {
                                    "dimension": "minecraft:overworld",
                                    "color": "#cfa84f",
                                    "biomes": ["minecraft:plains"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertEquals("Attunement 'desert' must specify an item.", ex.getMessage());
    }

    @Test
    void parsesSeveralDefinitionsFromOneFile() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, root(
                        defaultDef("#f2b84b", "minecraft:sunflower_plains", "minecraft:flower_forest"),
                        itemDef("sunflower_plains", "minecraft:sunflower", "#f7d13d", "minecraft:happy_villager", "minecraft:sunflower_plains"),
                        itemDef("flower_forest", "minecraft:allium", "#c58cff", null, "minecraft:flower_forest")
                ))),
                registryLookup()
        );

        assertEquals(3, lookup.size());
        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isPresent());
    }

    @Test
    void retrievesEffectiveDefaultAttunement() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, root(
                        defaultDef("#f2b84b", "minecraft:flower_forest"),
                        itemDef("flower_forest", "minecraft:allium", "#c58cff", "minecraft:enchant", "minecraft:flower_forest")
                ))),
                registryLookup()
        );

        assertEquals("default", lookup.defaultAttunement().id());
        assertEquals(Level.OVERWORLD, lookup.defaultTarget().dimension());
        assertEquals(1, lookup.defaultTarget().biomes().size());
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.FLOWER_FOREST));
    }

    @Test
    void higherPriorityDefaultReplacesOnlyDefaultDefinition() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, root(
                                defaultDef("#f2b84b", "minecraft:sunflower_plains", "minecraft:flower_forest"),
                                itemDef("sunflower_plains", "minecraft:sunflower", "#f7d13d", null, "minecraft:sunflower_plains")
                        )),
                        source("my_modpack", "modpack", 1, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "color": "#336699",
                                    "particle": "minecraft:poof",
                                    "biomes": ["minecraft:plains", "minecraft:forest"]
                                  }
                                }
                                """)
                ),
                registryLookup()
        );

        assertEquals(0x336699, lookup.defaultAttunement().colorRgb());
        assertEquals(Identifier.parse("minecraft:poof"), lookup.defaultAttunement().particleId());
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.PLAINS));
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.FOREST));
        assertFalse(lookup.defaultTarget().biomes().contains(Biomes.SUNFLOWER_PLAINS));
        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
    }

    @Test
    void rejectsUnknownItem() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, root(
                                defaultDef("#f2b84b", "minecraft:sunflower_plains"),
                                itemDef("bad_item", "minecraft:not_an_item", "#123456", null, "minecraft:flower_forest")
                        ))),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("bad_item"));
        assertTrue(ex.getMessage().contains("minecraft:not_an_item"));
    }

    @Test
    void rejectsUnknownBiome() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, root(
                                defaultDef("#f2b84b", "minecraft:sunflower_plains"),
                                itemDef("bad_biome", "minecraft:sunflower", "#f7d13d", null, "minecraft:not_a_biome")
                        ))),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("bad_biome"));
        assertTrue(ex.getMessage().contains("minecraft:not_a_biome"));
    }

    @Test
    void rejectsDuplicateEffectiveInputItems() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, root(
                                defaultDef("#f2b84b", "minecraft:sunflower_plains"),
                                itemDef("first", "minecraft:sunflower", "#111111", null, "minecraft:sunflower_plains"),
                                itemDef("second", "minecraft:sunflower", "#222222", null, "minecraft:flower_forest")
                        ))),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("first"));
        assertTrue(ex.getMessage().contains("second"));
        assertTrue(ex.getMessage().contains("minecraft:sunflower"));
    }

    @Test
    void rejectsInvalidColor() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "color": "red",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("field 'color'"));
    }

    @Test
    void rejectsUnknownParticle() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "color": "#f2b84b",
                                    "particle": "minecraft:not_a_particle",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("minecraft:not_a_particle"));
    }

    @Test
    void loadedTargetConvertsIntoBiomePredicate() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, root(
                        defaultDef("#f2b84b", "minecraft:sunflower_plains"),
                        itemDef("sunflower_plains", "minecraft:sunflower", "#f7d13d", "minecraft:happy_villager", "minecraft:sunflower_plains")
                ))),
                registryLookup()
        );

        AttunementDefinition definition = lookup.resolve(Items.SUNFLOWER).orElseThrow();
        BiomeDestinationTarget target = (BiomeDestinationTarget) definition.target();
        HolderLookup.RegistryLookup<net.minecraft.world.level.biome.Biome> biomes = registryLookup().lookupOrThrow(Registries.BIOME);

        assertEquals(Level.OVERWORLD, target.dimension());
        assertEquals(0xF7D13D, definition.colorRgb());
        assertEquals(Identifier.parse("minecraft:happy_villager"), definition.particleId());
        assertTrue(target.asBiomePredicate().test(biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS)));
        assertFalse(target.asBiomePredicate().test(biomes.getOrThrow(Biomes.PLAINS)));
    }

    private static String root(String... entries) {
        return "{\n" + String.join(",\n", entries) + "\n}";
    }

    private static String defaultDef(String color, String... biomeIds) {
        return """
                  "default": {
                    "dimension": "minecraft:overworld",
                    "color": "%s",
                    "biomes": [%s]
                  }
                """.formatted(color, quotedList(biomeIds));
    }

    private static String itemDef(String id, String itemId, String color, String particleId, String... biomeIds) {
        String particleField = particleId == null ? "" : """
                    "particle": "%s",
                """.formatted(particleId);
        return """
                  "%s": {
                    "item": "%s",
                    "dimension": "minecraft:overworld",
                    "color": "%s",
                %s    "biomes": [%s]
                  }
                """.formatted(id, itemId, color, particleField, quotedList(biomeIds));
    }

    private static String quotedList(String... values) {
        return String.join(", ", java.util.Arrays.stream(values).map(value -> "\"" + value + "\"").toList());
    }

    private static String builtInAttunementsJson() throws IOException {
        try (InputStream stream = AttunementLoaderTest.class.getClassLoader().getResourceAsStream(
                "data/forever_world_portals/forever_world_portals/attunements.json"
        )) {
            if (stream == null) {
                throw new IOException("Missing built-in attunements.json resource");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static HolderLookup.Provider registryLookup() {
        TestBootstrap.ensureBootstrapped();
        return VanillaRegistries.createLookup();
    }

    private static AttunementLoader.ResourceSource source(String namespace, String sourcePackId, int packPriority, String contents) {
        return new AttunementLoader.ResourceSource(
                Identifier.fromNamespaceAndPath(namespace, "forever_world_portals/attunements.json"),
                sourcePackId,
                packPriority,
                contents
        );
    }
}
