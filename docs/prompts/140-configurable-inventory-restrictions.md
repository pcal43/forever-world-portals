# Configurable inventory restrictions for Forever World Portals

Status: Completed
Date: 2026-07-12

## Prompt

Implement configurable inventory restrictions for Forever World Portals.

First inspect the current codebase and the existing prompts in `doc/prompts` so this change follows the current architecture and coding style.

As always, save a copy of this implementation prompt in `doc/prompts` using the existing numbering and filename convention.

# Goal

By default, Forever World Portals should only function when the player is carrying absolutely nothing.

This reinforces the core gameplay loop of beginning each frontier from scratch.

The restriction should be configurable so that server owners who prefer unrestricted travel can disable it.

# Configuration

Add a new property to the existing configuration:

```properties
requireEmptyInventory=true
```

This should default to `true`.

Document the new property alongside the existing configuration options.

When:

```properties
requireEmptyInventory=false
```

the portal should behave exactly as it does today with no inventory restrictions.

# Empty inventory definition

When `requireEmptyInventory=true`, a player may only enter a Forever World Portal if all of the following are empty:

* main inventory
* hotbar
* armor slots
* offhand

Any item in any slot counts.

This includes:

* tools
* food
* maps
* containers (even empty ones)
* armor
* shields
* totems

Ignore:

* experience levels
* status effects
* Ender Chest contents

Do not add compatibility code for optional inventory mods (Curios, Trinkets, etc.) as part of this implementation.

# Portal behavior

The inventory check should occur before a Forever World Portal begins activation.

If the player is carrying anything:

* the Forever World Portal must not begin its activation sequence
* the player must not accumulate portal time
* no destination lookup should begin
* no destination portal generation should begin
* no registry changes should occur
* no attunement logic should run

The portal should simply refuse entry.

Implement this at the earliest practical point in the existing portal entry flow so that players never see the normal portal activation behavior for a rejected entry.

# Player feedback

When entry is rejected, display an action-bar message such as:

> You cannot enter a Forever World Portal while carrying items.

(or equivalent wording if there is already a centralized localization/messages system).

Avoid spamming the player every tick while they continue standing in the portal.

A simple short cooldown for repeated messages is sufficient.

# Scope

This restriction applies to every traversal through a Forever World Portal, not only unresolved portals.

Return portals should enforce the same rule.

# Requirements

* Follow the existing architecture.
* Keep Fabric and NeoForge compatibility.
* Preserve dedicated-server compatibility.
* Reuse the existing configuration infrastructure where possible.
* Do not introduce unnecessary abstractions.
* Add comments only where they meaningfully explain non-obvious behavior.

When finished:

* ensure the project builds successfully
* verify formatting
* update any relevant documentation if needed
* save this prompt in `doc/prompts`
* include a brief implementation summary describing where the inventory check is performed and why that location prevents portal activation.

## Outcome

- Added `requireEmptyInventory=true` to the typed configuration, default properties, parser, and README.
- Enforced the restriction at the `NetherPortalBlock.entityInside` hook for Forever World portals by cancelling vanilla portal handling before portal time can accumulate.
- Added a short action-bar cooldown for the rejection message and kept a defensive inventory check in portal travel resolution.
- Ran `./gradlew --no-daemon :common:test --console=plain` and `./gradlew --no-daemon :fabric:build :neoforge:build --console=plain`.
- No in-game manual verification was run in this pass.
