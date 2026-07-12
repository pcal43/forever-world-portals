# Pass 130: Serene Attunement Awakening

Status: Completed
Date: 2026-07-12

## Prompt

Please refine the visual feedback that occurs when an unresolved Forever World Portal accepts a recognized attunement offering.

First inspect the current implementation and the existing prompts in `doc/prompts`, particularly the recent work implementing attunement acceptance particles. Preserve the current attunement behavior, registry logic, destination resolution, sound effects, and action-bar messages.

Save a copy of this implementation prompt in `doc/prompts` using the existing numbering and filename convention.

# Goal

Replace the current acceptance particle effect with something that feels magical and serene rather than explosive.

The effect should resemble the visual language of an End Gateway or magical portal awakening rather than TNT or an explosion.

The implementation must remain completely server-side and work with unmodified vanilla clients.

Do not change gameplay behavior.

# Remove explosion particles

Remove any use of:

* `EXPLOSION`
* `EXPLOSION_EMITTER`

The portal should no longer appear to explode when an attunement is accepted.

# New particle effect

When an unresolved portal accepts a recognized attunement offering:

* Spawn approximately 30–40 `REVERSE_PORTAL` particles.
* Spawn approximately 25–35 `END_ROD` particles.
* Spawn approximately 15–20 colored `DUST` particles using the RGB color from the accepted attunement definition.

These counts are guidelines. Adjust slightly if another balance looks better in-game.

# Portal-wide distribution

Do not emit all particles from a single point.

Reuse the existing portal geometry and distribute particles randomly throughout the entire portal interior.

The visual effect should make it appear that the entire portal has awakened simultaneously.

Do not duplicate portal-shape detection.

# Colored dust

Continue using the configured attunement color.

Do not hardcode colors.

Reuse the existing helper that converts the configured RGB value into `DustParticleOptions`.

Use a modest particle scale so the dust acts as an accent rather than obscuring the portal.

# Timing

Instead of spawning every particle during a single server tick, spread the effect over approximately 6–8 ticks.

Each tick should emit a portion of the total particles.

The goal is to create a brief magical surge rather than an instantaneous burst.

Keep the total duration under half a second.

Implement this with a small transient animation helper or scheduled effect. Do not introduce a permanent ticking system or global particle manager.

# Existing feedback

Preserve all existing behavior:

* attunement persistence;
* item consumption;
* confirmation sound;
* action-bar message.

Only refine the particle presentation.

# Code organization

Keep particle counts together as private named constants so they can be tuned easily later.

Reuse existing helpers where practical.

Do not duplicate particle-spawning logic.

# Validation

Run the normal formatting, tests, and builds for Fabric and NeoForge.

Verify in-game that:

* the portal feels like it briefly "awakens";
* the effect is noticeably more magical than explosive;
* the colored dust reflects the configured attunement color;
* ordinary Nether portals remain unchanged.

At the end, summarize:

* the final particle counts;
* the animation duration;
* how particles are distributed throughout the portal;
* the files changed.
