# Destination Search Budgeting And Biome Geometry

Status: Completed
Date: 2026-07-12

## Prompt

Please streamline destination search budgeting and biome-search geometry.

First inspect the current implementation around:

* `PortalDestinationSelector`
* `DestinationBiomeLocator`
* the spiral iterator or spiral-grid implementation
* `PortalPlacementService`
* `ForeverWorldPortalsConfig`
* config loading, validation, and generated defaults
* destination-search and placement tests

Also review the existing prompts in `doc/prompts` so this change remains consistent with the current architecture.

Save a copy of this implementation prompt in `doc/prompts`, following the existing filename and numbering convention.

# Goals

1. Decouple cheap spiral-grid scanning from expensive biome searches.
2. Allow the spiral to inspect substantially more candidate centers without proportionally increasing biome-locator calls.
3. Derive the biome-search radius from the spiral spacing rather than configuring it independently.
4. Reduce unnecessary overlap between neighboring circular biome searches while maintaining complete planar coverage.
5. Add a configurable per-biome-result limit on concrete portal placement attempts.
6. Remove or simplify code and configuration made obsolete by these changes.

# Search budgets

Introduce three clearly defined configuration settings:

```properties
maximumSpiralSearchPositions=512
maximumBiomeSearches=64
maximumPortalPlacementAttemptsPerBiome=64
```

Use names that fit the repository’s existing configuration style, but ensure each name clearly describes what it counts.

Remove the old ambiguous combined destination-attempt setting if it no longer has one precise meaning.

## Maximum spiral search positions

This counter applies to the entire destination search.

Increment it every time the spiral iterator yields a grid position, including positions rejected before biome lookup because they are too close to the snapshot of pre-existing generated regions.

These checks are relatively cheap, so use a default of:

```properties
maximumSpiralSearchPositions=512
```

Cheap spiral-center rejection must not consume the biome-search budget.

## Maximum biome searches

This counter also applies to the entire destination search.

Increment it only when the code actually invokes the biome locator for a spiral center that passed the initial cheap eligibility checks.

Use a default of:

```properties
maximumBiomeSearches=64
```

A biome search that returns no matching biome still consumes one biome-search attempt because the expensive lookup occurred.

The outer search must stop when either:

* `maximumSpiralSearchPositions` is exhausted; or
* `maximumBiomeSearches` is exhausted.

Make the termination reason clear in debug diagnostics.

## Maximum portal placement attempts per biome

This limit resets independently for each eligible biome result.

Use a default of:

```properties
maximumPortalPlacementAttemptsPerBiome=64
```

A concrete portal placement attempt means evaluating one orientation-specific proposed generated portal layout consisting of:

* one candidate X/Z location;
* its locally derived surface Y;
* one portal orientation;
* its complete foundation, clearance volume, frame, and anchor geometry.

Count each orientation-specific layout as one placement attempt.

Do not count:

* spiral positions;
* biome-locator calls;
* biome searches that return no result;
* biome results rejected by the generated-region-distance rule before placement begins;
* work performed before `PortalPlacementService` evaluates a concrete layout.

When one biome result exhausts its local placement budget without finding a valid layout:

1. return a normal placement failure for that biome result;
2. resume the outer spiral and biome search if the global budgets remain;
3. reset the full placement-attempt budget for the next eligible biome result.

Do not add a global portal-placement-attempt budget unless the existing architecture has a separate concrete need for one.

# Spiral spacing and biome-search radius

Keep the spiral-grid spacing configurable using the current setting or a clearly named equivalent, conceptually:

```properties
destinationSpiralSpacingBlocks=10000
```

Do not expose an independently configurable biome-search radius.

The biome locator searches horizontally within a circular radius around each spiral center. Since the spiral centers form a square grid, derive the biome-search radius as:

```text
spiral spacing / sqrt(2)
```

This is the distance from a grid center to the corner of its square coverage cell. It provides complete planar coverage with substantially less overlap than using the full grid spacing.

Implement the radius deterministically, for example:

```java
int biomeSearchRadius =
        (int) Math.ceil(destinationSpiralSpacingBlocks / Math.sqrt(2.0));
```

Use `ceil` so integer conversion does not introduce small uncovered gaps.

For a spacing of 10,000 blocks, the derived radius should be:

```text
7,072 blocks
```

Extract the calculation into a clearly named helper if that improves testability.

Do not persist the derived radius in configuration.

# Keep distance concepts separate

The following are distinct concepts and must remain separate:

* spiral-grid spacing;
* derived biome-search radius;
* minimum allowed distance from the snapshot of pre-existing generated regions.

The minimum generated-region distance must not automatically become the biome-search radius.

For each spiral center:

1. increment the spiral-position counter;
2. check whether the center is sufficiently far from the pre-search region snapshot;
3. if not, reject it without invoking the biome locator;
4. if eligible and the biome-search budget remains, increment the biome-search counter and invoke the biome locator using the derived radius;
5. if a biome position is returned, independently check the returned position against the generated-region-distance rule;
6. reject a returned biome position that is too close to pre-existing generated regions;
7. only then begin the per-biome portal placement search.

Preserve the existing rule that region files created during the current search do not affect the minimum-distance constraint.

# Intended search flow

The resulting control flow should be conceptually similar to:

```java
while (spiralPositionsExamined < maximumSpiralSearchPositions
        && biomeSearchesPerformed < maximumBiomeSearches) {

    BlockPos spiralCenter = spiral.next();
    spiralPositionsExamined++;

    if (!isFarEnoughFromExistingRegions(spiralCenter)) {
        continue;
    }

    biomeSearchesPerformed++;

    Optional<BlockPos> biomeResult = biomeLocator.find(
            spiralCenter,
            derivedBiomeSearchRadius
    );

    if (biomeResult.isEmpty()) {
        continue;
    }

    BlockPos biomePosition = biomeResult.get();

    if (!isFarEnoughFromExistingRegions(biomePosition)) {
        continue;
    }

    Optional<GeneratedPortal> portal =
            portalPlacementService.findAndPlace(
                    biomePosition,
                    maximumPortalPlacementAttemptsPerBiome
            );

    if (portal.isPresent()) {
        return portal;
    }
}
```

Adapt this to the current architecture rather than copying it literally.

# Portal placement service

Update `PortalPlacementService` so its local search accepts or otherwise obtains the per-biome placement-attempt limit.

The limit applies to concrete orientation-specific layouts, not merely candidate columns.

For example, if one X/Z position is tested in both horizontal orientations, that consumes two placement attempts.

Stop evaluating further layouts for that biome result once the local limit is reached.

The placement service should distinguish, at least internally or in debug diagnostics, between:

* no valid layout found before the candidate search ended;
* the per-biome placement-attempt limit being exhausted;
* successful placement.

Do not turn local placement-budget exhaustion into a fatal destination-search failure.

# Configuration changes

Update all relevant configuration code:

* config record fields;
* parsing;
* validation;
* defaults;
* generated config output;
* inline comments;
* README or other config documentation;
* tests.

Use defaults:

```properties
maximumSpiralSearchPositions=512
maximumBiomeSearches=64
maximumPortalPlacementAttemptsPerBiome=64
```

Retain the current spiral-spacing default unless there is a clear reason to rename it.

Validate that:

* spiral spacing is positive;
* maximum spiral positions is positive;
* maximum biome searches is positive;
* maximum placement attempts per biome is positive;
* the derived biome radius is positive and cannot overflow.

This is a development-stage project. Migration support for old local config files is not required unless the repository already has an explicit migration policy.

Remove old compatibility aliases and stale config keys rather than preserving unnecessary complexity.

# Diagnostics

Add concise debug-level diagnostics that make search exhaustion understandable.

At minimum, report:

* spiral positions examined;
* biome searches performed;
* which global budget ended the search;
* when one biome result exhausts its per-biome placement budget;
* whether the outer search will continue after that local failure.

Avoid noisy per-candidate normal-level logging.

# Cleanup

Aggressively remove code made redundant or unused by this refactor, including:

* the old combined destination-attempt counter;
* independently configured biome-search-radius fields;
* any proposed global placement-attempt counter with no remaining purpose;
* obsolete constants;
* obsolete parsing branches;
* unused constructors or method parameters;
* outdated log messages;
* stale comments;
* outdated tests;
* stale documentation.

Do not leave duplicate or ambiguous configuration concepts in place.

# Scope

Do not redesign:

* spiral traversal order;
* biome predicates or attunement definitions;
* region-snapshot semantics;
* minimum generated-region-distance calculations;
* generated portal geometry;
* portal registry behavior;
* teleportation;
* asynchronous execution.

Keep this focused on destination-search accounting, biome-search radius derivation, local placement budgeting, configuration, and cleanup.

# Tests

Add or update tests covering at least:

1. Every consumed spiral position increments the spiral-position counter.
2. A spiral position rejected before biome lookup does not increment the biome-search counter.
3. Every biome-locator invocation increments the biome-search counter exactly once.
4. A biome lookup returning no result still consumes one biome-search attempt.
5. The search stops when the spiral-position budget is exhausted.
6. The search stops when the biome-search budget is exhausted.
7. Diagnostics distinguish the two global exhaustion reasons.
8. The biome-search radius is derived as `ceil(spacing / sqrt(2))`.
9. A spacing of 10,000 produces a radius of 7,072.
10. The derived circular search radius reaches the corner shared by four neighboring square-grid cells.
11. Returned biome positions are independently checked against the generated-region-distance rule.
12. Each orientation-specific proposed layout consumes one per-biome placement attempt.
13. Testing both orientations at one candidate X/Z consumes two attempts.
14. The placement counter resets for each new eligible biome result.
15. Exhausting one biome result’s placement budget returns a local failure and allows the outer search to continue.
16. The next eligible biome result receives the full placement budget.
17. Local placement attempts do not affect the global spiral or biome counters.
18. Zero or negative values for any configured limit are rejected.
19. The old combined attempt setting no longer remains.
20. No independently configurable biome-search radius remains.

# Verification

After implementation:

1. Search the repository for the old combined destination-attempt setting.
2. Search for any independently configured biome-search-radius setting.
3. Search for any obsolete global placement-attempt configuration.
4. Remove remaining dead code, stale comments, and outdated documentation.
5. Run destination-search and portal-placement tests.
6. Run the full build for Fabric and NeoForge.
7. Report:

   * the new configuration keys and defaults;
   * the exact semantics of all three limits;
   * the derived-radius formula;
   * the radius produced for the default spacing;
   * how per-biome placement exhaustion is handled;
   * code and configuration removed;
   * confirmation that cheap spiral rejections no longer consume biome-search attempts.
