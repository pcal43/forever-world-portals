# Pass 80: Remove Held-Item Attunement

Status: Completed
Date: 2026-07-12

## Prompt

Remove the newly added held-item attunement behavior, while preserving the data-pack-driven attunement definitions and lookup infrastructure.

Save this prompt verbatim in `doc/prompts` with the next appropriate numbered filename.

## Context

The previous implementation added two related pieces:

1. A data-pack-driven attunement system loaded from:

```text
data/<namespace>/forever_world_portals/attunements.json
```

2. Gameplay integration that inspects the player’s held item when a new portal route is created and uses the matching attunement definition to select the destination biome.

We have decided not to use held-item attunement. Attunement will instead eventually be driven by items thrown into a portal.

This task is only to remove the held-item gameplay integration. Preserve the data loading, merging, validation, semantic target model, and lookup infrastructure so it can be reused by the future thrown-item mechanism.

## Required changes

Remove all behavior that inspects either of the player’s hands to determine portal attunement.

Specifically, remove or revert code that:

* reads the player’s main-hand or offhand `ItemStack` when entering, activating, or creating a portal route;
* resolves an attunement directly from a held item;
* captures a held-item-derived target when a destination search begins;
* changes the biome search predicate based on the player’s currently held item;
* sends player feedback specifically because a held item was recognized;
* treats an unrecognized held item as an attunement fallback case;
* passes player hand state through portal activation or destination-search APIs.

After this change, what the player is holding must have no effect on portal behavior.

## Restore the pre-integration search behavior

Restore the portal destination-search behavior that existed immediately before held-item attunement was integrated.

When a new portal route is created:

* use the existing default or hardcoded biome target behavior;
* do not consult the attunement registry;
* do not require an attunement item;
* do not alter traversal of existing routes;
* do not change portal registration, landing-site selection, destination-portal generation, or route persistence except where necessary to remove held-item plumbing.

If the previous implementation centralized the default biome target while adding attunement support, it is fine to retain that cleanup. The resulting behavior should simply always use that default target for now.

## Preserve the attunement data system

Keep all reusable data-pack and model code, including:

* loading resources from:

```text
data/<namespace>/forever_world_portals/attunements.json
```

* merging definitions by logical attunement ID and data-pack priority;
* validation of item, dimension, and biome IDs;
* duplicate effective input-item validation, if currently implemented;
* immutable publication of the loaded definitions;
* Fabric and NeoForge server-data reload registration;
* built-in attunement definitions;
* the semantic destination model, such as:

  * `AttunementDefinition`;
  * `DestinationTarget`;
  * `BiomeDestinationTarget`;
* lookup APIs that resolve a definition from an item or `ItemStack`;
* tests for parsing, merging, validation, and item lookup;
* logging that reports successful attunement-data loading.

The item lookup is still useful. The future thrown-item mechanism will use the thrown `ItemEntity`’s stack to resolve an attunement.

Do not delete or redesign this infrastructure merely because it is temporarily unused by portal activation.

## Avoid misleading API names

Review any APIs introduced specifically around player hands.

For example, remove or rename methods such as:

```java
resolveHeldAttunement(...)
resolveMainHandAttunement(...)
capturePlayerAttunement(...)
```

The reusable data-layer API should instead be neutral, such as:

```java
findByItem(ItemStack stack)
findByItem(Item item)
resolve(ItemStack stack)
```

Use names consistent with the existing project.

The registry should understand item mappings. It should not understand players, hands, portal entry, or activation.

## Remove unnecessary gameplay plumbing

Simplify method signatures and state objects that were expanded only to carry held-item attunement information.

Examples include:

* player parameters passed solely to inspect held items;
* captured attunement IDs passed into the search only from hand resolution;
* optional destination targets added to route-creation contexts solely for held-item behavior;
* diagnostics fields whose only source was the held item;
* temporary search state that exists only to preserve the held-item selection.

Do not remove target-related abstractions that are useful independently of held items or that prepare the search code for future target selection.

Use judgment to distinguish:

* reusable destination-target architecture, which should remain;
* held-item integration plumbing, which should be removed.

## Messaging

Remove messages such as:

```text
The portal resonates with the pale garden.
```

when those messages are triggered by inspecting a held item.

Do not add replacement attunement messaging yet. The future thrown-item mechanic will define its own feedback behavior.

Preserve unrelated portal-search and failure messages.

## Tests

Update or remove tests that specifically assert held-item behavior, including tests that verify:

* a player’s main-hand item changes the destination biome;
* an unrecognized held item uses a fallback;
* attunement is captured from the player’s hand;
* existing routes ignore held items.

Keep tests for:

* attunement JSON parsing;
* logical-ID merging and override priority;
* validation;
* duplicate item detection;
* lookup by `Item` or `ItemStack`;
* semantic biome destination targets;
* reload behavior;
* the normal default biome-search behavior after gameplay integration is removed.

Add or update a focused test demonstrating that portal route creation no longer consults the player’s held items, where practical.

Run the relevant tests and builds for all supported loaders.

## Scope control

Do not implement the thrown-item mechanism in this task.

In particular, do not yet:

* detect `ItemEntity` objects inside portal blocks;
* attach NBT or data components to thrown items;
* persist a list of items thrown into a portal;
* resolve attunement from accumulated portal offerings;
* consume thrown items;
* add particles or player feedback for thrown items;
* alter the portal registry to store pending offerings.

This is a cleanup step that leaves the codebase in this state:

* the attunement data and item-to-target mapping system exists and is tested;
* portal creation currently ignores attunements and uses the default biome target;
* no player-hand-specific attunement code remains;
* the future thrown-item implementation can call the existing item lookup directly.

At the end, summarize:

* which held-item integration points were removed;
* which attunement data and lookup components were preserved;
* what destination behavior is now used for new portals;
* any hand-specific APIs that were renamed or removed;
* tests and builds run.

## Outcome

- Removed the held-item attunement gameplay path from portal founding. New portals no longer inspect player hand state, resolve attunements from held items, or send held-item attunement messages.
- Restored new-route destination search to always use the default biome target via the shared `DestinationTargets.defaultBiomeTarget()` helper.
- Preserved the attunement data system: pack-driven JSON loading, merge/override behavior, validation, immutable publication, built-in definitions, item lookup APIs, and Fabric/NeoForge reload registration.
- Simplified gameplay plumbing by removing held-item-derived target selection from `PortalTravelService` and restoring a default-only `beginSearch(...)` entry point in `PortalDestinationSelector`, while keeping target-aware search support available internally for future work.
- Updated tests to drop held-item assertions and instead verify that founding travel now uses the default destination target.
- Verified with `./gradlew :common:test` and `./gradlew :fabric:build :neoforge:build`.
