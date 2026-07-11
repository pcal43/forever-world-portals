# Pass 2: First-Entry Teleportation and Persistent Portal Linkage

Status: Completed
Date: 2026-07-11

## Prompt

# Forever World Portals — Pass 2: First-Entry Teleportation and Persistent Portal Linkage

Before making any code changes:

1. Read and follow the repository's `AGENTS.md`.
2. Archive this complete implementation prompt under `docs/prompts/` exactly as described in `AGENTS.md` before modifying production code.

Build on the existing implementation from Pass 1.

Do **not** regenerate or restructure the project.

The project currently supports:

* detection of valid diamond-block portal frames
* activation using flint and steel
* vanilla Nether portal block generation
* recognition of Forever World portals
* recognition when an entity enters a Forever World portal
* normal obsidian Nether portals continuing to behave exactly as vanilla

The goal of this pass is to implement the **first founding trip**.

When a player enters a Forever World portal for the first time, the mod should:

* choose a distant destination
* find a safe landing location
* create persistent portal records
* reserve the future return portal location
* persist the linkage
* teleport the player

Do **not** generate a return portal yet.

Do **not** implement inventory stripping yet.

---

# Core lifecycle

Lighting a diamond portal should **not** create persistent world data.

A lit portal is simply a valid Forever World portal.

The first time a player successfully enters that portal is when the world permanently records the existence of a new frontier.

The lifecycle should be:

```
Player lights portal
    ↓
Portal exists physically
    ↓
Player enters portal
    ↓
Normalize frame
    ↓
Check for existing portal record
    ↓
If none exists:
    • select distant destination
    • validate landing site
    • create origin portal record
    • create reserved destination record
    • link them together
    • persist world data
    ↓
Teleport player
```

If the portal already has a registry entry, reuse the existing destination.

Never generate a new destination for an existing origin portal.

---

# Persistent world data

Use Minecraft's normal persistent world-data mechanism.

Follow the conventions already established by the project.

Do not use JSON files or Java serialization.

The portal registry is the authoritative source of truth.

Portal blocks are ephemeral.

Portal identity comes from:

* normalized frame anchor
* persistent registry

not from individual Nether portal blocks.

---

# Portal registry

Create a persistent registry capable of storing portal records.

Implement an appropriate world-scoped abstraction.

Do not use global static storage.

The singleton service should own access to the registry, but the registry itself should remain world-specific.

Begin with a `dataVersion` field.

Create a migration entry point even if version 1 requires no migration.

Unknown or malformed records should be skipped with warnings rather than crashing world loading.

---

# Portal records

Implement two record types conceptually.

## Origin portal

Represents the physical portal the player entered.

Record at least:

* UUID
* dimension
* canonical frame anchor
* portal axis
* frame width
* frame height
* representative portal position
* linked portal UUID
* creation game time
* data version

## Reserved destination

Represents the future return portal.

This portal does **not** physically exist yet.

Record at least:

* UUID
* reserved destination position
* linked origin UUID
* state indicating that no physical portal exists yet
* creation game time
* data version

Do not generate a destination portal during this pass.

---

# Destination selection

Implement a reliable first-pass destination selector.

Choose a destination at least a configurable minimum distance from every existing Forever World portal.

Perfection is not required.

Reliability is.

Prefer correctness over optimization.

Avoid generating excessive numbers of chunks.

If useful vanilla APIs exist for biome queries or safe-position selection, inspect and reuse them where appropriate.

---

# Landing-site validation

Find a safe arrival position.

Avoid placing the player:

* inside solid blocks
* underwater
* in lava
* in powder snow
* in the void

Reuse vanilla safe-placement logic where practical.

Only modify terrain if absolutely necessary to prevent an invalid arrival.

Do not generate a return portal.

Do not generate unnecessary structures.

---

# Transaction ordering

Do not create persistent portal records until destination selection has succeeded.

The preferred order is:

1. Detect and normalize the portal frame.
2. Check for an existing registry entry.
3. If one exists, reuse it.
4. Otherwise:

   * choose destination
   * validate landing site
   * construct origin and reserved records
   * insert both into the registry
   * establish the bidirectional linkage
   * mark world data dirty
5. Teleport the player.

Avoid leaving partially-created portal state.

If destination selection fails:

* do not create portal records
* do not teleport the player
* leave the portal usable for another attempt
* report a useful error to the player
* log sufficient diagnostic information

---

# Teleportation

Suppress vanilla Nether travel only for valid Forever World portals.

Leave ordinary obsidian portals completely unchanged.

Teleport only players.

Ignore non-player entities for this pass.

Preserve:

* health
* hunger
* experience
* inventory
* armor
* offhand
* status effects
* Ender Chest contents

Inventory stripping will be implemented later.

---

# Configuration

Expand the existing configuration only as needed.

At minimum expose:

```properties
minimumPortalSeparationBlocks=25000
destinationSearchAttempts=64
```

Continue using the project's typed configuration system.

Validate configuration values.

Provide sensible defaults.

Do not introduce speculative configuration for future mechanics.

---

# Logging

Log:

* first-time portal registration
* destination reservation
* teleport success
* destination selection failures
* persistent data loading
* malformed portal records

Avoid noisy per-tick logging.

---

# Explicitly out of scope

Do **not** implement:

* destination portal generation
* automatic return portals
* inventory stripping
* portal pairing completion
* player-built return portal matching
* advancement gating
* Ancient City activation
* Echo Shards
* Nether Star requirements
* commands beyond what already exists
* custom blocks
* custom items
* client-side rendering
* networking

Remain focused on first-entry reservation and teleportation.

---

# Testing

Add unit tests where practical.

At minimum verify:

* registry serialization/deserialization
* canonical frame lookup
* registry persistence across world reload
* destination selection obeys minimum separation
* existing portals reuse their reserved destination

If game tests are available, verify:

* entering an unregistered portal creates registry records
* world reload preserves portal linkage
* re-entering an existing portal does not reroll the destination
* ordinary Nether portals continue working normally

---

# Build verification

Run the project's normal build workflow.

Verify:

* Fabric builds successfully
* NeoForge builds successfully
* Fabric development server launches
* NeoForge development server launches

Manually verify:

* light a diamond portal
* enter it
* teleport to a distant location
* save and reload the world
* enter the original portal again
* verify the same destination is reused

Fix implementation issues rather than documenting them.

---

# Documentation

Update the README.

Document:

* first-entry behavior
* persistent portal reservation
* current implementation status
* known limitations

Clearly state that:

* return portals are not yet implemented
* inventory stripping is not yet implemented

---

# Deliverables

At the end, provide:

1. A summary of the implementation.
2. Files added or modified.
3. The archived prompt filename created under `docs/prompts/`.
4. Build and launch results.
5. Tests performed.
6. Important implementation decisions.
7. Known limitations.
8. Recommendations for Pass 3.

The successful outcome of this pass is:

* entering a new Forever World portal permanently creates a frontier
* the destination is selected exactly once
* the linkage is recorded in persistent world data
* subsequent entries reuse the same destination
* the player is teleported safely
* no return portal is generated yet
* the project is ready for implementing player-built return portals in the next pass.

## Outcome

- Implemented persistent Forever World portal registry data backed by Minecraft `SavedData`, with origin portal records, reserved destination records, data-versioning, and malformed-record skipping.
- Added first-entry player teleportation for valid diamond-frame portals, with destination reservation, minimum-separation checks, safe-landing validation, and reuse of the same reserved destination on later entries.
- Kept ordinary obsidian Nether portals on vanilla behavior and suppressed custom travel only for recognized Forever World portals.
- Updated configuration defaults and parsing for `minimumPortalSeparationBlocks` and `destinationSearchAttempts`, and updated the README for Pass 2 behavior and limitations.
- Ran `./gradlew test build --console=plain`, `./gradlew :fabric:runServer --console=plain`, and `./gradlew :neoforge:runServer --console=plain`.
- Deferred return-portal generation, paired return travel, inventory stripping, and automated in-world game tests to later passes.
