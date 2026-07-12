# Prevalidated Portal Placement

Status: Completed
Date: 2026-07-12

## Prompt

Please streamline portal generation so that candidate placements are fully validated before any blocks are changed.

First inspect the current implementation around:

* `PortalPlacementService`
* `PlacementTransaction`
* `PortalPlacementRollback`
* `GeneratedPortal`
* `PortalLayout`
* `PortalFrameDetector`
* `PortalIdentity`
* `SafeLandingFinder`

Also review the existing prompts in `doc/prompts` so this change remains consistent with the current architecture.

Save a copy of this implementation prompt in `doc/prompts`, following the existing filename and numbering convention.

# Goal

Portal placement should use a simple two-phase flow:

1. Construct and validate a proposed portal layout without modifying the world.
2. Once the candidate is known to be valid, place the complete portal exactly once.

Do not place blocks speculatively and then inspect or roll them back when a normal validation check fails.

As part of this refactoring, aggressively remove any code that becomes redundant or unused. The goal is to simplify the implementation, not merely leave obsolete helper classes and methods in place.

# Current issue

The current placement code appears to:

1. construct a `PortalLayout`;
2. perform some pre-placement validation;
3. modify blocks using a `PlacementTransaction`;
4. run frame detection, anchor verification, or safe-arrival checks against the newly placed structure;
5. roll the changes back if one of those checks fails.

This is unnecessarily complex because the generated structure is deterministic. The code already knows the proposed frame, interior, canonical anchor, floor, and resulting block states before placement.

Normal candidate rejection should happen before any world mutation.

# Required design

Refactor candidate evaluation into an entirely read-only validation phase.

For each candidate anchor:

1. Construct the complete `PortalLayout`.
2. Validate every affected block position against the current world state.
3. Validate that the resulting structure will have the expected canonical anchor.
4. Validate that the resulting arrival position will be safe.
5. Reject the candidate without modifying any blocks if any check fails.
6. Only after all checks pass, place the complete frame and portal interior.
7. Return the resulting `GeneratedPortal`.

There should be no ordinary control-flow path that:

* places part or all of a candidate portal;
* discovers that the candidate is invalid;
* restores the original blocks;
* proceeds to another candidate.

# Layout and anchor validation

`PortalLayout.createForAnchorBlock(...)` should deterministically produce a layout whose canonical bottom-center interior anchor is the requested anchor.

Do not place the portal and then call the general-purpose `PortalFrameDetector` merely to rediscover the structure that `PortalLayout` just defined.

Instead:

* derive the generated `ForeverWorldPortalFrame` directly from `PortalLayout`, or add a clear conversion method if needed;
* compute or expose the canonical anchor directly from the layout;
* validate that the layout anchor matches the requested candidate before placement.

If the layout-to-frame or anchor relationship needs verification, cover that invariant with unit tests rather than runtime speculative placement.

Do not weaken the general-purpose frame detector used for naturally discovered or player-built portals. This refactor only removes its unnecessary use as a post-generation validation step.

# Safe-arrival validation

Move generated-portal arrival validation before placement.

The code knows the expected resulting states of:

* the player's feet position;
* the player's head position;
* the supporting block below;
* the rest of the generated frame and interior.

Add or reuse a read-only validation method that evaluates the proposed post-placement geometry without requiring those blocks to already exist in the world.

Avoid duplicating the core safety rules between generated portals and normal portal arrival logic. Extract a small shared predicate or helper if practical, but do not introduce a large abstraction.

# Cleanup

This change is intended to simplify the implementation. Do not preserve obsolete abstractions simply because they previously existed.

After the refactoring, inspect the surrounding code and remove anything that is no longer necessary, including but not limited to:

* unused classes;
* unused records;
* unused helper methods;
* unused interfaces;
* unused constructors;
* unused imports;
* dead private methods;
* obsolete comments;
* obsolete tests;
* stale documentation;
* compatibility code that no longer serves a purpose.

In particular:

* delete `PortalPlacementRollback` if it is no longer needed;
* remove `PlacementTransaction` if it is no longer required;
* remove any rollback component from `GeneratedPortal`;
* eliminate any post-placement validation code that has become redundant;
* simplify method signatures after removing obsolete parameters.

Do not leave behind dead code "just in case." If a class or method has no remaining purpose after this refactoring, delete it.

# Placement phase

Once validation succeeds, placement should be a straightforward commit operation.

Prefer code conceptually shaped like:

```java
PortalLayout layout = PortalLayout.createForAnchorBlock(...);

if (!isValidPlacement(level, layout, frameState, portalState)) {
    continue;
}

placeLayout(level, layout, frameState, portalState);

return Optional.of(new GeneratedPortal(
        layout.frame(),
        layout.anchorBlock()
));
```

Use names and types that fit the existing codebase rather than copying this example literally.

The placement method should not contain normal validation branches. Validation belongs in the read-only phase.

# Scope

Do not redesign:

* destination biome selection;
* spiral or region searching;
* portal registry semantics;
* portal pairing;
* attunement;
* teleportation;
* player inventory checks;
* player-built portal detection.

This should remain a focused simplification of generated portal placement.

# Tests

Add or update tests covering at least:

1. A valid candidate causes no world mutation during validation.
2. An invalid candidate causes no world mutation.
3. A successfully validated candidate is placed once.
4. The generated frame's canonical anchor equals the requested anchor.
5. The proposed landing position is validated before placement.
6. The generated structure is still recognized correctly by the normal frame detector after placement.
7. No obsolete rollback classes or rollback APIs remain.

# Verification

After implementing the change:

1. Search the repository for:

   * `PortalPlacementRollback`
   * `PlacementTransaction`
   * rollback fields or methods on `GeneratedPortal`
2. Remove any remaining dead code discovered as part of this refactoring.
3. Run the relevant common tests.
4. Run the full build for all supported loaders.
5. Report:

   * the files changed;
   * which checks were moved before placement;
   * which classes and methods were removed;
   * any additional dead code eliminated during the cleanup;
* confirmation that invalid candidates make no block changes;
* confirmation that successfully generated portals are still detected and entered correctly.

## Outcome

- Refactored generated portal placement into a read-only validation phase that returns a deterministic `PortalLayout`, followed by a single commit phase that places the frame and portal blocks once.
- Removed `PortalPlacementRollback`, the inner `PlacementTransaction`, block snapshot capture, and the rollback field from `GeneratedPortal`.
- Moved anchor verification, frame derivation, affected-block validation, and arrival-safety checks before placement.
- Added focused placement tests plus layout updates so generated layouts can expose their canonical anchor and derived frame directly.
- Ran `./gradlew --no-daemon :common:test --console=plain` and `./gradlew --no-daemon :fabric:build :neoforge:build --console=plain`.
