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
        assertEquals(4, lookup.size());
        assertEquals(Level.OVERWORLD, lookup.defaultTarget().dimension());
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.SUNFLOWER_PLAINS));
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.FLOWER_FOREST));
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.PALE_GARDEN));
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
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
                                  "desert": {
                                    "dimension": "minecraft:overworld",
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
                List.of(source("forever_world_portals", "builtin", 0, """
                        {
                          "default": {
                            "dimension": "minecraft:overworld",
                            "biomes": ["minecraft:sunflower_plains", "minecraft:flower_forest"]
                          },
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

        assertEquals(3, lookup.size());
        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isPresent());
    }

    @Test
    void retrievesEffectiveDefaultAttunement() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(source("forever_world_portals", "builtin", 0, """
                        {
                          "default": {
                            "dimension": "minecraft:overworld",
                            "biomes": ["minecraft:flower_forest"]
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

        assertEquals("default", lookup.defaultAttunement().id());
        assertEquals(Level.OVERWORLD, lookup.defaultTarget().dimension());
        assertEquals(1, lookup.defaultTarget().biomes().size());
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.FLOWER_FOREST));
    }

    @Test
    void mergesDefinitionsFromMultipleNamespacesByPackPriority() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
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

        assertEquals(3, lookup.size());
        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isPresent());
    }

    @Test
    void higherPriorityDefinitionReplacesLowerPriorityDefinitionByLogicalId() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
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

        assertTrue(lookup.resolve(Items.DANDELION).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isEmpty());
    }

    @Test
    void higherPriorityDefaultReplacesOnlyDefaultDefinition() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains", "minecraft:flower_forest"]
                                  },
                                  "sunflower_plains": {
                                    "item": "minecraft:sunflower",
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """),
                        source("my_modpack", "modpack", 1, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:plains", "minecraft:forest"]
                                  }
                                }
                                """)
                ),
                registryLookup()
        );

        assertEquals(2, lookup.defaultTarget().biomes().size());
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.PLAINS));
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.FOREST));
        assertFalse(lookup.defaultTarget().biomes().contains(Biomes.SUNFLOWER_PLAINS));
    }

    @Test
    void preservesUnrelatedDefinitionsWhenDefaultIsReplaced() {
        AttunementLookup lookup = AttunementLoader.load(
                List.of(
                        source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
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
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:plains"]
                                  }
                                }
                                """)
                ),
                registryLookup()
        );

        assertTrue(lookup.resolve(Items.SUNFLOWER).isPresent());
        assertTrue(lookup.resolve(Items.ALLIUM).isPresent());
        assertTrue(lookup.defaultTarget().biomes().contains(Biomes.PLAINS));
    }

    @Test
    void rejectsUnknownItem() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
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
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
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
    void rejectsInvalidDefaultDimension() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:the_nether",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );

        assertTrue(ex.getMessage().contains("default"));
        assertTrue(ex.getMessage().contains("minecraft:the_nether"));
    }

    @Test
    void rejectsInvalidOrEmptyDefaultBiomeList() {
        IllegalStateException emptyList = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": []
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );
        assertTrue(emptyList.getMessage().contains("default"));
        assertTrue(emptyList.getMessage().contains("must not be empty"));

        IllegalStateException unknownBiome = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:not_a_biome"]
                                  }
                                }
                                """)),
                        registryLookup()
                )
        );
        assertTrue(unknownBiome.getMessage().contains("default"));
        assertTrue(unknownBiome.getMessage().contains("minecraft:not_a_biome"));
    }

    @Test
    void rejectsDuplicateEffectiveInputItems() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AttunementLoader.load(
                        List.of(source("forever_world_portals", "builtin", 0, """
                                {
                                  "default": {
                                    "dimension": "minecraft:overworld",
                                    "biomes": ["minecraft:sunflower_plains"]
                                  },
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
                          "default": {
                            "dimension": "minecraft:overworld",
                            "biomes": ["minecraft:sunflower_plains"]
                          },
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
