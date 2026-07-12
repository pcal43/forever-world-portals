# Pass 120: Portal-Wide Attunement Particles

Status: Completed
Date: 2026-07-12

## Prompt

Please enhance the visual feedback that occurs when an unresolved Forever World Portal accepts a recognized attunement offering.

First inspect the current implementation and the existing prompts in `doc/prompts`, particularly the recent work implementing attunement feedback. Preserve the current attunement behavior, registry logic, destination resolution, and server/client architecture.

Save a copy of this implementation prompt in `doc/prompts` using the existing numbering and filename convention.

# Goal

Make attunement acceptance feel like a significant magical event.

The effect must remain completely server-side and visible to vanilla clients. Do not introduce client-side rendering, custom particles, resource packs, or custom assets.

Do not change any gameplay behavior.

# Visual effect

Replace the current small acceptance particle burst with a more dramatic effect centered on the entire portal.

When an attunement offering is accepted:

1. Spawn a single `EXPLOSION_EMITTER` particle at the center of the portal interior.
2. Spawn a generous burst of `END_ROD` particles distributed throughout the portal interior.
3. Spawn a larger burst of `REVERSE_PORTAL` particles distributed throughout the portal interior.
4. Spawn a burst of colored `DUST` particles using the RGB color from the accepted attunement definition.

The overall effect should feel energetic without becoming visually overwhelming.

As a rough guideline:

* 1 `EXPLOSION_EMITTER`
* 20–30 `END_ROD`
* 40–60 `REVERSE_PORTAL`
* 20–30 colored `DUST`

Exact counts may be adjusted after testing if another combination looks better.

# Portal-wide emission

Do not spawn all particles from a single point.

Instead, distribute particles across the detected portal interior.

Reuse the existing portal geometry or portal bounds already available in the codebase.

Randomize particle positions throughout the portal opening so that the entire portal appears to react.

Do not duplicate portal detection logic.

# Colored dust

Use the configured RGB color from the accepted attunement definition.

Do not hardcode colors.

If the attunement definition already exposes a color object or RGB value, reuse it.

If the current color representation must be converted into Minecraft's `DustParticleOptions`, perform that conversion in a small reusable helper.

The dust should be decorative rather than opaque. Choose a reasonable particle scale (approximately 0.8–1.2) that looks good in-game.

# Existing feedback

Preserve the existing:

* confirmation sound;
* action-bar message;
* item consumption behavior.

Only replace and improve the particle effect.

# Performance

This effect occurs only when an attunement is accepted, so it may be more elaborate than normal ambient portal particles.

However:

* avoid excessive allocations;
* avoid spawning hundreds of particles;
* keep implementation straightforward.

# Code organization

Create a small helper responsible for acceptance particle effects if one does not already exist.

That helper should accept:

* the server level;
* the portal geometry or bounds;
* the attunement color.

It should not know anything about registry persistence or destination resolution.

# Validation

Run the normal formatting, tests, and builds for both Fabric and NeoForge.

Verify in-game that:

* the entire portal visibly reacts rather than only the item location;
* the colored dust matches the configured attunement color;
* particles appear correctly on vanilla clients connected to a modded server;
* ordinary Nether portals are unaffected.

At the end, summarize:

* the particle counts ultimately chosen;
* how portal-wide particle positions are generated;
* how the attunement RGB is converted into `DustParticleOptions`;
* the files changed.
