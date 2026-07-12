Status: Completed

Update the attunement registry so that the unattuned/default destination is defined entirely by data rather than by hardcoded Java constants.

Save this prompt verbatim in `doc/prompts` with the next appropriate numbered filename.

## Goal

Make the required attunement definition named `default` the authoritative destination target for an unattuned portal.

As a result, remove the hardcoded default biome target currently represented by code like:

```java
public final class DestinationTargets {

    private static final BiomeDestinationTarget DEFAULT_TARGET = new BiomeDestinationTarget(
            Level.OVERWORLD,
            Set.of(
                    Biomes.SUNFLOWER_PLAINS,
                    Biomes.FLOWER_FOREST,
                    Biomes.PALE_GARDEN
            )
    );
```

The default target must come from the merged attunement data instead.

Do not add or restore any logic concerning held items, player hands, item inspection, or item-based attunement selection as part of this change. This task concerns only the attunement registry, the required `default` definition, and removal of the hardcoded fallback.

## Data format

The built-in attunement file should contain a required top-level entry named `default`:

```json
{
  "default": {
    "dimension": "minecraft:overworld",
    "biomes": [
      "minecraft:sunflower_plains",
      "minecraft:flower_forest",
      "minecraft:pale_garden"
    ]
  },

  "sunflower_plains": {
    "item": "minecraft:sunflower",
    "dimension": "minecraft:overworld",
    "biomes": [
      "minecraft:sunflower_plains"
    ]
  },

  "flower_forest": {
    "item": "minecraft:allium",
    "dimension": "minecraft:overworld",
    "biomes": [
      "minecraft:flower_forest"
    ]
  },

  "pale_garden": {
    "item": "minecraft:pale_oak_sapling",
    "dimension": "minecraft:overworld",
    "biomes": [
      "minecraft:pale_garden"
    ]
  }
}
```

Use the existing built-in attunement mappings if they differ from this example. The important change is adding the required `default` entry with the current default biome list.

## Validation rules

After merging all attunement files by logical ID and data-pack priority, enforce these rules:

1. An effective definition named `default` must exist.
2. The `default` definition must not contain an `item` field.
3. Every definition other than `default` must contain an `item` field.
4. `default` is a reserved logical attunement ID.
5. A higher-priority data pack may replace the complete `default` definition in the same way it replaces any other logical definition.
6. After merging is complete, there must be exactly one effective `default` definition.
7. The `default` definition must otherwise satisfy the same destination-target validation as other definitions:

   * valid dimension;
   * nonempty biome list for the currently supported biome target;
   * valid biome IDs;
   * currently supported destination dimension.

Produce clear validation errors such as:

```text
Missing required 'default' attunement.
```

```text
The 'default' attunement must not specify an item.
```

```text
Attunement 'desert' must specify an item.
```

Do not interpret an omitted `item` field on an arbitrary entry as a default. Only the entry whose logical ID is exactly `default` has that meaning.

Do not use `minecraft:air`, an empty string, `null`, or another sentinel item ID to represent the default.

## Registry model

The resolved attunement registry should explicitly retain the effective default definition, conceptually:

```java
AttunementDefinition defaultAttunement;
Map<Item, AttunementDefinition> attunementsByItem;
```

Adapt the exact types and naming to the existing implementation.

Expose a direct way to retrieve the effective default attunement or its destination target, for example:

```java
AttunementDefinition defaultAttunement();
```

or:

```java
DestinationTarget defaultTarget();
```

Use whichever form best matches the current architecture.

The registry must always have a valid default after a successful reload.

Do not add player-facing resolution logic in this task. In particular, do not add methods that accept an `ItemStack`, inspect player inventory, or determine an attunement from either hand.

## Portal behavior

Wherever the portal destination-selection code currently needs the unattuned/default destination target, obtain it from the attunement registry.

The portal code should no longer know the hardcoded default biome set.

The expected flow is conceptually:

```java
BiomeDestinationTarget target =
        attunementRegistry.defaultTarget();
```

Adapt this to the existing types and dependency structure.

This change should not alter when or how other attunements are selected. It should only replace the existing hardcoded fallback with the effective data-driven `default` entry.

## Remove hardcoded defaults

Remove the hardcoded default target from `DestinationTargets`, including:

```java
private static final BiomeDestinationTarget DEFAULT_TARGET = ...
```

and any accessor such as:

```java
DestinationTargets.defaultTarget()
```

Update all callers to obtain the default through the attunement registry.

If `DestinationTargets` has no remaining responsibilities after this change, remove the class entirely.

If it still contains unrelated useful behavior, retain only that behavior and rename or refactor it if the old name is no longer accurate.

There should be no remaining Java constant that duplicates the default biome set from the data file.

The biome IDs:

```text
minecraft:sunflower_plains
minecraft:flower_forest
minecraft:pale_garden
```

should exist as default-selection policy in the built-in attunement JSON, not as a hardcoded Java `Set` used by normal destination selection.

It is fine for biome constants to appear in tests that verify the loaded built-in data.

## Reload semantics

Continue to publish a newly built registry only after the entire reload succeeds.

Because `default` is required, a missing or invalid default definition must cause the new reload to fail rather than publishing a registry without fallback behavior.

Do not replace the current valid registry with an invalid or incomplete registry.

If the existing reload framework retains the previous successful data after a failed `/reload`, preserve that behavior.

## Overrides

A modpack should be able to replace only the default destination policy with a higher-priority file containing:

```json
{
  "default": {
    "dimension": "minecraft:overworld",
    "biomes": [
      "minecraft:plains",
      "minecraft:forest"
    ]
  }
}
```

The effective `default` entry should replace the lower-priority built-in `default` entry as a whole.

All unrelated built-in attunement definitions should remain effective.

Do not merge the higher-priority default biome list with the lower-priority list.

## Tests

Add or update tests covering:

* successful loading of the built-in `default`;
* failure when no effective `default` exists;
* failure when `default` contains an `item`;
* failure when a non-default definition omits `item`;
* retrieval of the effective default attunement;
* higher-priority replacement of only the `default` definition;
* preservation of unrelated attunement definitions when `default` is replaced;
* rejection of an invalid default dimension;
* rejection of an invalid or empty default biome list;
* confirmation that portal destination selection no longer obtains its fallback from `DestinationTargets`;
* removal of the hardcoded Java default biome set.

Run the relevant common, Fabric, and NeoForge tests and builds.

## Scope control

Do not add or restore:

* held-item inspection;
* main-hand attunement behavior;
* offhand attunement;
* two-item combinations;
* item consumption;
* item tags;
* biome tags;
* structure targeting;
* cross-dimensional portal behavior;
* commands for editing attunements.

This change should only make the required data-driven `default` entry replace the existing hardcoded fallback target.

At the end, summarize:

* the validation rules added for `default`;
* how the effective default is represented and retrieved;
* the hardcoded constants and classes removed or changed;
* the built-in JSON update;
* override behavior;
* tests and builds run.
