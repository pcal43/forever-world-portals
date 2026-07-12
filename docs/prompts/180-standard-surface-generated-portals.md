# Standard Surface Generated Portals

Status: Completed
Date: 2026-07-12

## Prompt

Please simplify and improve generated destination portal placement.

First inspect the current implementation around:

* `PortalDestinationSelector`
* `PortalPlacementService`
* `PortalLayout`
* `GeneratedPortal`
* `SafeLandingFinder`
* `PortalFrameDetector`
* `PortalIdentity`
* `PortalTravelService`
* any placement rollback or transaction classes still present

Also review the existing prompts in `doc/prompts` so this change remains consistent with the current architecture.

Save a copy of this implementation prompt in `doc/prompts`, following the existing filename and numbering convention.

# Goals

Make generated destination portals predictable and safe:

1. Always generate a standard portal with a 2×3 interior and 4×5 outer frame.
2. Do not reproduce the dimensions of the source portal.
3. Choose placement candidates at the local terrain surface.
4. Require a safe 4×3 foundation.
5. Require a fully clearable 4×3×5 volume above that foundation.
6. Validate the entire candidate without changing any blocks.
7. Once a candidate passes validation, clear the volume and place the portal exactly once.
8. Remove code that becomes unnecessary as a result.

# Generated portal dimensions

Player-built source portals may retain whatever supported dimensions they have.

Automatically generated destination portals must always use vanilla’s standard minimum portal dimensions:

* interior width: 2 blocks
* interior height: 3 blocks
* outer frame width: 4 blocks
* outer frame height: 5 blocks

Remove the source portal width and height from generated destination placement APIs if they are no longer needed.

Do not preserve arbitrary source-frame geometry merely for symmetry.

# Candidate surface height

The destination-selection code currently uses:

```java
Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
```

to select a surface Y level. Preserve that surface-oriented behavior.

However, nearby placement candidates must not all reuse the original requested anchor’s Y coordinate.

For every candidate X/Z examined during the nearby placement search:

1. Query `MOTION_BLOCKING_NO_LEAVES` at that candidate location.
2. Derive the candidate foundation or portal Y from that local heightmap result.
3. Construct and validate the proposed portal volume at that candidate’s own surface height.

This should allow the search to adapt to slopes and uneven terrain instead of testing all nearby positions at one fixed Y.

Be precise about whether the heightmap result denotes:

* the first air block above the motion-blocking surface; or
* the surface block itself.

Use the actual Minecraft API semantics and make the foundation and clearance coordinates explicit. Avoid accidental off-by-one placement.

# Candidate geometry

For each candidate position and each supported horizontal portal orientation, define:

* a 4×3 rectangular foundation;
* a 4×3×5 clearance volume immediately above the foundation;
* a standard 4×5 portal frame centered in the middle depth plane;
* one clear depth layer in front of the portal;
* one clear depth layer behind the portal.

Conceptually, along the depth axis:

```text
clear layer | portal plane | clear layer
```

The portal frame occupies the complete 4-block width and 5-block height of the middle plane.

The 2×3 portal interior is centered within that frame.

Create a small, explicit geometry representation if helpful, but avoid an unnecessarily general arbitrary-size layout abstraction.

# Read-only validation

All candidate validation must occur before any block changes.

For each proposed candidate, validate the complete foundation, clearance volume, frame, portal interior, canonical anchor, and registry constraints before modifying the world.

If any validation fails:

* reject the candidate;
* make no block changes;
* proceed to the next candidate.

There must be no normal candidate-rejection path that places blocks and then rolls them back.

# Foundation validation

Every block in the 4×3 foundation must be safe to stand on.

Use a clear shared predicate, such as `isSafeFoundationBlock`, based on existing Minecraft block-state behavior where practical.

At minimum, reject foundation blocks that are:

* air;
* fluid;
* non-sturdy on the upward face;
* hazardous to stand on;
* portals;
* falling or otherwise unstable;
* block entities where replacement or interaction would be inappropriate.

Do not hardcode a narrow list of ordinary terrain blocks. Normal terrain such as dirt, grass, stone, sand, gravel, terracotta, deepslate, and similar natural surfaces should work when they satisfy the chosen safety rules.

If sand or gravel is considered unsuitable as a stable foundation, reject it explicitly and document the decision.

# Clearance-volume validation

Every block in the 4×3×5 volume above the foundation must be replaceable using Minecraft’s normal block-state replacement semantics.

Use the current Minecraft API equivalent of:

```java
state.canBeReplaced()
```

Do not broaden this into arbitrary excavation through stone, dirt, or constructed blocks.

The purpose of this rule is to ensure that destination generation finds an already open or naturally replaceable surface space rather than carving a sealed room underground or destroying substantial terrain.

Also reject candidate volumes containing:

* fluids;
* block entities;
* existing portal blocks;
* other special blocks that should not be silently removed.

If these exclusions are already implied by the replacement predicate, keep the implementation simple and avoid redundant checks unless they improve safety or clarity.

# Surface behavior

Do not add a skylight requirement in this change.

The use of `MOTION_BLOCKING_NO_LEAVES` for each candidate X/Z should continue to bias placement toward the surface and avoid deep underground generation.

The combination of:

* per-candidate surface-height selection;
* a safe 4×3 foundation;
* a fully replaceable 4×3×5 clearance volume;

should be the primary placement contract.

# Canonical anchor

Define the generated portal’s canonical anchor deterministically.

For a 2-block-wide interior, retain the project’s existing tie-breaking convention when choosing between the two bottom interior blocks.

The resulting anchor must:

* be an interior portal block;
* be derived directly from the proposed layout;
* be known before placement;
* be stored as the destination portal’s registered anchor;
* remain consistent with general portal identity and containment matching.

Do not place the portal and rediscover its anchor through `PortalFrameDetector`.

Verify layout-to-anchor invariants with tests.

# Registry collision check

Before placement, reject a proposed generated frame if its interior would contain the stored anchor of any existing registered portal in the same dimension.

Perform this check against the proposed frame geometry before any block changes.

Preserve deterministic behavior if registry APIs return multiple records.

# Placement commit

Once all validation succeeds:

1. Convert the complete 4×3×5 clearance volume to air.
2. Place the standard 4×5 frame in the center depth plane.
3. Fill the 2×3 interior with vanilla Nether portal blocks using the correct axis state.
4. Return a simplified `GeneratedPortal` containing only information actually used by callers.

The placement phase should be a simple commit operation. It should not contain ordinary validation branches or candidate-search behavior.

Use appropriate block update flags consistent with the existing implementation.

# Cleanup and simplification

Aggressively remove code that becomes redundant or unused as a result of this change.

Inspect and clean up:

* arbitrary source-dimension parameters passed to generated placement;
* generalized generated-layout code that only existed to preserve source shape;
* source width and height calculations no longer needed by placement;
* post-placement frame rediscovery;
* post-placement anchor verification;
* generated-portal safe-landing searches that are replaced by the validated chamber geometry;
* rollback classes and placement transactions;
* unused records, constructors, methods, imports, constants, comments, tests, and documentation.

Delete dead code rather than preserving it for hypothetical future use.

Keep the general-purpose `PortalFrameDetector` and portal identity logic used for player-built and existing portals. Only remove their unnecessary use in generated destination placement.

# Scope

Do not redesign:

* biome targeting;
* destination-region selection;
* minimum-distance rules;
* attunement;
* portal registry persistence;
* source portal matching;
* teleportation timing;
* inventory restrictions;
* loader-specific integration.

This change should focus on generated destination portal geometry, surface-height selection, validation, placement, and resulting cleanup.

# Tests

Add or update tests covering at least:

1. Generated portals always have a 2×3 interior and 4×5 frame.
2. Source portal dimensions do not affect generated destination dimensions.
3. Every nearby X/Z candidate recomputes its own Y using `MOTION_BLOCKING_NO_LEAVES`.
4. Sloped terrain can succeed when a nearby candidate has a different surface Y from the original target.
5. All 12 foundation blocks must pass the safe-foundation predicate.
6. All 60 blocks in the 4×3×5 clearance volume must be replaceable.
7. A single non-replaceable clearance block rejects the candidate without mutation.
8. A single unsafe foundation block rejects the candidate without mutation.
9. Invalid candidates cause no block changes.
10. A successful candidate clears the complete volume and places one standard portal.
11. The generated anchor is deterministic and lies inside the generated frame.
12. Proposed frames enclosing an existing registered anchor are rejected before placement.
13. The completed generated portal is recognized by the normal frame detector.
14. No obsolete arbitrary-size placement or rollback code remains.

# Verification

After implementation:

1. Search for remaining generated-placement dependencies on source portal width or height.
2. Search for:

   * `PortalPlacementRollback`
   * `PlacementTransaction`
   * rollback components on `GeneratedPortal`
   * post-placement frame rediscovery used only by generated placement
3. Remove all code made unused by the refactor.
4. Run the relevant common tests.
5. Run the full build for all supported loaders.
6. Report:

   * files changed;
   * how candidate-local surface Y is calculated;
   * the exact foundation and clearance geometry;
   * the precise replaceability and foundation rules;
   * code deleted or simplified;
   * confirmation that invalid candidates do not mutate the world;
   * confirmation that generated destination portals are always standard 2×3 portals.
