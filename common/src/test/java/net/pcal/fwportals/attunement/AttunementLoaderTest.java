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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttunementLoaderTest {

    @Test
    void parsesSeveralDefinitionsFromOneFile() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, """
                        {
                          "sunflower_plains": {
                            "item": "minecraft:sunflower",
                            "dimension": "minecraft:overworld",
                            "biomes": ["minecraft:sunflower_plains"]
                          },
                          "flower_forest": {
                            "item": "minecraft:allium",
                            "dimension": "minecraft:overworld",
                            "biomes": ["minecraft:flower_forest"]
                          }
                        }
                        """)),
                registryLookup()
        );

        assertEquals(2, lookup.size());
        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isPresent());
    }

    @Test
    void mergesDefinitionsFromMultipleNamespacesByPackPriority() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "sunflower_plains": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """),
                        source("my_modpack", "modpack", 1, """
                                {
                                  "flower_forest": {
                                    "item": "minecraft:allium",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """)
                ),
                registryLookup()
        );

        assertEquals(2, lookup.size());
        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isPresent());
    }

    @Test
    void higherPriorityDefinitionReplacesLowerPriorityDefinitionByLogicalId() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "flower_forest": {
                                    "item": "minecraft:allium",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """),
                        source("my_modpack", "modpack", 1, """
                                {
                                  "flower_forest": {
                                    "item": "minecraft:dandelion",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """)
                ),
                registryLookup()
        );

        assertTrue(lookup.resolve(Items.DANDELION).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isEmpty());
    }

    @Test
    void preservesUnrelatedLowerPriorityDefinitions() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "sunflower_plains": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
                                  "flower_forest": {
                                    "item": "minecraft:allium",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """),
                        source("my_modpack", "modpack", 1, """
                                {
                                  "flower_forest": {
                                    "item": "minecraft:dandelion",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """)
                ),
                registryLookup()
        );

        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.DANDELION).isPresent());
    }

    @Test
    void rejectsUnknownItem() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "bad_item": {
                                    "item": "minecraft:not_an_item",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """)),
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
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "bad_biome": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:not_a_biome"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("bad_biome"));
        assertTrue(ex.getMessage().contains("minecraft:not_a_biome"));
    }

    @Test
    void rejectsEmptyBiomeList() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "empty_biomes": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": []
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("empty_biomes"));
        assertTrue(ex.getMessage().contains("must not be empty"));
    }

    @Test
    void rejectsDuplicateEffectiveInputItems() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "first": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
                                  "second": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:flower_forest"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("first"));
        assertTrue(ex.getMessage().contains("second"));
        assertTrue(ex.getMessage().contains("minecraft:sunflower"));
    }

    @Test
    void loadedTargetConvertsIntoBiomePredicate() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, """
                        {
                          "sunflower_plains": {
                            "item": "minecraft:sunflower",
                            "dimension": "minecraft:overworld",
                            "biomes": ["minecraft:sunflower_plains"]
                          }
                        }
                        """)),
                registryLookup()
        );

        AttunementDefinition definition = lookup.resolve(Items.SUNFLOWER).orElseThrow();
        BiomeDestinationTarget target = (BiomeDestinationTarget) definition.target();
        HolderLookup.RegistryLookup<net.minecraft.world.level.biome.Biome> biomes = registryLookup().lookupOrThrow(Registries.BIOME);

        assertEquals(Level.OVERWORLD, target.dimension());
        assertTrue(target.asBiomePredicate().test(biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS)));
        assertFalse(target.asBiomePredicate().test(biomes.getOrThrow(Biomes.PLAINS)));
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
