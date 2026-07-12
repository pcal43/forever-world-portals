# Pass 70: Data-Driven Biome Attunements

Status: Completed
Date: 2026-07-12

## Prompt

Implement data-driven biome attunements and integrate them with portal destination selection.

Before making changes, review the current implementation and the recent prompts in `doc/prompts`, especially the biome-targeting work. Preserve the current architecture and terminology where possible rather than introducing parallel abstractions.

Save this prompt verbatim in `doc/prompts` with the next appropriate numbered filename.

## Goal

Replace the currently hardcoded destination-biome predicate with a reloadable, data-pack-driven mapping from a held item to a destination target.

When a player activates a new source portal while holding a recognized attunement item in their main hand, use the corresponding biome target for that portal's destination search.

For now:

* support main-hand items only;
* support biome targets only;
* do not consume the attunement item;
* do not implement offhand combinations;
* do not implement structure targeting;
* do not implement cross-dimensional teleportation yet;
* nevertheless, model the loaded destination definition so that dimension and other future target kinds can be added cleanly.

## Data-pack file layout

Forever World Portals should load attunement data from:

```text
data/<namespace>/forever_world_portals/attunements.json
```

The built-in definitions should therefore live at:

```text
common/src/main/resources/data/forever_world_portals/forever_world_portals/attunements.json
```

A modpack can contribute additional or overriding definitions from a path such as:

```text
data/my_modpack/forever_world_portals/attunements.json
```

The first `forever_world_portals` in the built-in path is the resource namespace. The second is the custom data directory scanned by this mod.

## JSON format

Each file contains a JSON object whose top-level keys are logical attunement IDs.

The keys are not item IDs. Keep the item inside each definition so that the schema can later support more complex inputs such as an optional offhand item without changing how definitions are identified.

Example:

```json
{
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

Use those three mappings as the initial built-in data unless an item ID is unavailable in the currently targeted Minecraft version. If an ID is unavailable, choose the closest sensible vanilla item and explain the substitution.

Each current definition must have:

* a nonblank logical attunement ID from its top-level key;
* one valid `item` ID;
* one valid `dimension` ID;
* a nonempty `biomes` array containing valid biome IDs.

Do not add item-tag support or biome-tag support in this pass. This is intentionally a small set of specific secret items.

## Definition identity and pack merging

Read every matching:

```text
data/*/forever_world_portals/attunements.json
```

from the active server data packs.

Merge the top-level definitions by logical attunement ID in data-pack priority order:

* process lower-priority definitions first;
* a higher-priority definition with the same logical ID replaces the entire lower-priority definition;
* definitions with other IDs remain in place;
* do not merge individual fields or biome lists.

This is custom entry-level merging. Do not merely request the final visible resource for one fixed namespace, because that would prevent different namespaces from contributing definitions.

Retain enough source information to produce useful errors or warnings showing the definition ID and source resource.

## Internal model

Do not make the item mapping return only a raw biome predicate.

Represent the semantic destination data explicitly. A suitable model would be conceptually similar to:

```java
record AttunementDefinition(
    String id,
    Item item,
    DestinationTarget target
) {
}

sealed interface DestinationTarget {
}

record BiomeDestinationTarget(
    ResourceKey<Level> dimension,
    Set<ResourceKey<Biome>> biomes
) implements DestinationTarget {
}
```

Adapt names and types to the mappings and architecture already used by this repository.

It is acceptable for `BiomeDestinationTarget` to be the only supported target implementation now. The important points are:

* the target retains its dimension;
* the target retains biome registry keys;
* the lookup does not discard the semantic data by immediately converting everything into an anonymous predicate;
* future structure or dimension-only target implementations can be added without redesigning attunement identity or the root lookup API.

Do not build an extensible third-party target-type registration system. Forever World Portals will own a small, closed set of target kinds.

The JSON does not need a `"type"` field. Future target kinds will be inferred from mutually exclusive fields such as `biomes` or `structures`.

## Reloading

Register the data loader as a server-data reload listener on both Fabric and NeoForge, keeping loader-specific registration code as thin as possible.

Put parsing, merging, validation, lookup construction, and resolution behavior in common code.

On server startup and `/reload`:

1. Read all matching attunement resources.
2. Merge definitions by logical ID and pack priority.
3. Parse and validate the effective definitions.
4. Resolve item, dimension, and biome IDs through the appropriate registries.
5. Build a new immutable resolved lookup.
6. Publish the new lookup atomically only after the reload succeeds.

Do not expose a partially rebuilt lookup if parsing or validation fails.

Use the project’s existing serialization conventions. Prefer Mojang codecs if that fits the current codebase cleanly, but do not introduce excessive codec machinery merely for appearance. Clear validation and maintainable common code are more important.

Log a concise summary after a successful reload, including the number of effective attunements loaded.

Invalid definitions should fail the reload with an actionable message that identifies:

* the logical attunement ID;
* the source resource where possible;
* the invalid field or registry ID.

## Duplicate input items

After definitions have been merged by logical ID, two different effective definitions may still name the same main-hand item.

Treat that as a validation error rather than selecting one unpredictably.

The error should identify the item and both attunement IDs.

For the initial implementation, compile the effective definitions into an efficient lookup equivalent to:

```java
Map<Item, AttunementDefinition>
```

Keep that compiled lookup as an implementation detail. Do not make the source format use item IDs as its definition keys.

## Portal activation behavior

Integrate attunement resolution at the point where a new source portal's destination search is initiated.

When the player activates or first enters a portal that does not already have a registered route:

1. Inspect the player's main-hand `ItemStack`.
2. Resolve it through the current attunement lookup.
3. If it matches a biome attunement, capture that resolved destination target for this search.
4. Convert that target's biome keys into the predicate or holder predicate expected by the biome-search code.
5. Run the existing biome-targeted destination search using that predicate.
6. Continue through the existing landing-site selection, destination-portal generation, and route-registration behavior.

Do not repeatedly inspect the player's hand while an asynchronous or incremental search is in progress. Resolve and capture the target when the search begins.

Do not consume, damage, or modify the held item.

Do not apply attunement when traversing an already registered portal. Once a route exists, traversal must continue to use the stored route regardless of what the player is holding.

## Behavior with no recognized item

Preserve the current non-attuned behavior as the fallback.

If the main-hand item has no attunement definition, use the same default biome predicate or destination behavior that exists immediately before this change. Do not prevent ordinary portal activation merely because the player is not holding a recognized attunement item.

Centralize that default rather than leaving the old hardcoded predicate mixed into the attunement loader.

## Dimension handling in this pass

Every attunement definition includes a required dimension, but cross-dimensional travel is not being implemented yet.

For now:

* accept only `minecraft:overworld` as an executable target dimension;
* validate and retain the dimension as part of the semantic target;
* if a loaded definition uses another dimension, either reject it clearly as unsupported or allow it to load but refuse activation clearly.

Prefer rejecting unsupported dimensions during reload for now, because that avoids apparently valid definitions that fail only during gameplay.

Structure the search invocation so that a destination level can later be supplied explicitly rather than permanently assuming the source player's level. Do not implement Nether or End search behavior in this change.

## User feedback

Use the project's existing messaging style.

When a recognized attunement is selected, provide concise feedback indicating that the portal is attuned. Do not reveal every secret mapping in advance or add a command that lists all attunements.

A suitable message would communicate the destination theme without exposing unnecessary internals, for example:

```text
The portal resonates with the pale garden.
```

Derive a readable description from the logical attunement ID or add an optional internal display representation if necessary. Do not create client-side-only UI or require a client mod.

If the search ultimately fails, retain the existing failure behavior, but make it possible for diagnostics to identify which attunement was being used.

## Persistence

The registered route remains the authoritative persistent result.

Do not require the item to be held on later traversals.

If the current search state is persisted across saves or restarts, persist enough information to resume with the captured target rather than re-reading the player's hand. If searches are not currently persisted, do not add a new persistence mechanism solely for this task.

There is no need to store the attunement ID permanently after the route has been successfully created unless it is useful for existing diagnostics. The destination portal location and route remain the gameplay state that matters.

## Tests

Add focused tests where practical for:

* parsing a valid attunement file;
* loading several definitions from one file;
* merging files from multiple namespaces;
* higher-priority replacement by logical ID;
* preservation of unrelated lower-priority definitions;
* rejection of an unknown item;
* rejection of an unknown biome;
* rejection of an empty biome list;
* rejection of a duplicate effective input item;
* lookup by main-hand item;
* fallback behavior for an unrecognized item;
* conversion of a loaded target into the biome predicate used by the current search;
* ensuring attunement affects only creation of a new route, not traversal of an existing route.

Run the relevant tests and builds for all supported loaders.

## Scope control

Do not implement any of the following in this pass:

* offhand attunement;
* two-item combinations;
* item tags;
* biome tags;
* structure lookup;
* structure JSON fields;
* Nether or End portal destinations;
* dimension-only targets;
* item consumption;
* a public attunement-list command;
* a third-party target-type registration API;
* migration of old attunement files, since none exist yet.

However, avoid names such as `BiomeAttunementRegistry` if the same registry will eventually contain structure or dimension targets. Prefer general names such as `AttunementRegistry`, `AttunementDefinition`, and `DestinationTarget`, with biome-specific names only for the currently supported target subtype.

At the end, summarize:

* the data-file layout;
* the merge and override behavior;
* the built-in mappings added;
* where main-hand attunement is captured;
* how the selected target reaches the biome search;
* fallback behavior with no recognized item;
* validation behavior;
* tests and builds run;
* any design deviations required by the existing code.

## Outcome

- Added reloadable data-pack attunements from `data/<namespace>/forever_world_portals/attunements.json`, with built-in definitions under `data/forever_world_portals/forever_world_portals/attunements.json`.
- Implemented pack-priority merging by logical attunement ID, whole-definition replacement, duplicate effective item validation, and fail-fast validation for malformed item, biome, and dimension data.
- Captured main-hand attunement only when founding a new route, routed the selected biome target into the existing spiral-plus-biome destination search, and preserved the previous default behavior when no attunement matches.
- Added loader-neutral common parsing and lookup code plus thin Fabric and NeoForge reload-listener registration hooks.
- Verified with `./gradlew :common:test` and `./gradlew :fabric:build :neoforge:build`.
- NeoForge registration currently uses the event's deprecated `getRegistryAccess()` to inject registry lookup into the shared reload logic; this is the main implementation compromise in the current API setup.
