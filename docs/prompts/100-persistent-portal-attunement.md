# Pass 100: Persistent Portal Attunement

Status: Completed
Date: 2026-07-12

## Prompt

Please implement persistent portal attunement using items thrown into a portal.

First inspect the current code and the existing prompts in `doc/prompts` so that this change fits the current portal identity, registry, and datapack-driven attunement system. Preserve the existing architecture wherever practical.

Save a copy of this implementation prompt in `doc/prompts`, following the existing filename and numbering convention.

# Goal

Implement a portal-level attunement system based on items thrown into an unresolved Forever World Portal.

The interaction should be:

1. A player throws a recognized attunement item into an unresolved Forever World Portal.
2. The portal visibly consumes one item.
3. The portal immediately persists that item as its current attunement.
4. Throwing another recognized attunement item into the same unresolved portal simply replaces the previous attunement.
5. When the portal is first entered, the stored attunement determines the biome predicate used during destination selection.

Do not redesign the biome search itself.

# Simplified attunement model

A portal stores exactly one attunement item.

There is **no** concept of:

* multiple offerings
* recipes
* combinations
* item counts
* partial matches

The last accepted offering always wins.

Example:

```text
throw snowball
→ portal attuned to snowy biomes

throw cactus
→ portal now attuned to desert biomes
```

Changing your mind simply costs another item.

# Portal lifecycle

A source portal may exist in two states.

## Pending portal

* identified by the existing canonical portal identity
* destination has not yet been resolved
* may have a stored attunement item

## Resolved portal

* destination exists
* attunement can no longer change

A lit portal with neither a destination nor an attunement does not require a registry entry.

Create the registry entry when the first valid attunement item is accepted.

# Registry changes

Update the portal record so that it can represent an unresolved portal.

A record should now contain:

* existing portal identity
* optional destination
* optional attunement item

Conceptually:

```java
Optional<ResourceLocation> attunementItem;
```

Use the project's existing registry identifier type.

Persist only the item's registry identifier.

Do **not** persist a full `ItemStack`.

Destination and attunement should both be optional.

Resolved records without an attunement should continue to load normally.

Migration of old development data is not required.

# Attunement registry

Keep the existing datapack-driven attunement registry.

Extend each attunement definition so that it also contains presentation metadata for future visual effects.

Each attunement definition should now contain:

* attunement item
* destination biome target or biome predicate
* RGB color
* optional vanilla particle identifier

These presentation properties are **not** used yet beyond loading, validation, and storage.

They exist so that future work can display attunement-specific colors and particles without redesigning the data format again.

Keep the implementation simple.

Do not implement custom particles, colored particles, client-side rendering, or any visual effects based on these fields yet.

# Detecting offerings

Detect server-side `ItemEntity` instances entering the interior of a valid unresolved Forever World Portal.

Reuse the existing portal-frame detection and canonical portal identity.

Do not introduce a second portal-identification mechanism.

Only accept an item if:

* it is inside a valid Forever World Portal
* the portal has not yet been resolved
* the item is recognized by the existing attunement registry

Unrecognized items should behave exactly as they do today.

They must not be consumed.

Resolved portals ignore thrown offerings.

Ordinary Nether portals ignore them.

# Consuming offerings

When a valid offering is accepted:

* immediately consume exactly one item from the `ItemEntity` stack so the portal visibly absorbs the offering
* replace the portal's stored attunement item with the accepted item's registry identifier
* immediately mark the registry dirty
* if the `ItemEntity` stack becomes empty, remove the entity
* otherwise leave the remaining stack in the world

Treat consumption and persistence as one logical server-thread operation so that an accepted item cannot disappear without being recorded.

Ensure that the same `ItemEntity` cannot be processed repeatedly during consecutive ticks.

If the portal already had an attunement, simply overwrite it.

Do not refund or preserve the previous offering.

# Using the attunement

When resolving a portal destination, obtain the biome predicate from the portal's stored attunement item.

Keep the existing datapack-driven mapping from attunement item to biome predicate.

If a portal has no stored attunement, continue using the existing default destination-selection behavior.

# Resolving the portal

When the portal is entered for the first time:

* read the stored attunement
* resolve the biome predicate
* perform the existing destination search
* persist the destination

Only after destination creation and registry updates have completed successfully:

* clear the stored attunement

If destination generation fails for any reason, leave the attunement intact so the player can retry.

# Feedback

Keep all feedback server-side using vanilla assets.

When an offering is successfully accepted:

* play one suitable vanilla sound
* emit a brief generic vanilla particle burst
* if the `ItemEntity` has a player owner and that player is still online, display the message **"Portal attuned"** to that player

Only display the message when an item is actually accepted.

Do not display any message when an item is rejected or ignored.

Future work will use the color and particle metadata stored in the attunement registry to provide richer visual feedback.

Do not implement:

* portal recoloring
* custom particles
* colored particles
* client networking
* localization
* action bar messages describing the attunement
* chat messages describing the attunement

# Performance

Do not globally scan every portal or every item entity every tick.

Use the narrowest loader-neutral hook practical.

Keep loader-specific entry points thin.

Place shared behavior in the common module.

# Tests

Add or update tests covering at least:

* unresolved portal serialization
* resolved portal serialization
* replacing one attunement with another
* rejecting unrecognized items
* ignoring offerings on resolved portals
* consuming exactly one item from a stack
* preserving attunement when destination generation fails
* clearing attunement after successful destination creation
* default behavior when no attunement exists
* loading attunement registry color and particle metadata

# Documentation

Update project documentation.

Describe that:

* portals are attuned by throwing a single item into them before first use
* accepted offerings are consumed
* the most recently accepted offering replaces any previous attunement
* established portals cannot be re-attuned

# Scope

Do not:

* add client-side code
* redesign the registry beyond supporting unresolved portals and an optional attunement
* redesign the attunement datapack format beyond adding color and optional particle metadata
* redesign biome searching
* add commands
* add configuration
* implement recipe-based attunements
* allow resolved portals to be re-attuned
* implement the future color/particle effects

Run the relevant Fabric and NeoForge builds and tests.

At completion, summarize:

* registry changes
* attunement registry changes
* thrown-item detection
* item consumption behavior
* destination resolution flow
* tests added or updated
* any remaining limitations or follow-up work.
