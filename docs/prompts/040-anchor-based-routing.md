# Pass 40: Anchor-Based Routing

Status: Completed
Date: 2026-07-11

## Prompt

Please revise the Forever World Portals portal-anchor, destination-search, portal-generation, arrival, and registry logic.

The current implementation carries several overlapping concepts:

* reserved destination position;
* landing position;
* generated portal position;
* portal origin block;
* arrival position.

These can disagree. In particular, first travel may use a generated portal’s arrival position while the registry stores the earlier reserved landing position, causing subsequent travel to arrive somewhere different.

Replace this with a simpler and stricter model:

> Every physical Forever World portal has one canonical anchor block.
> Every route is stored as `source portal anchor -> destination portal anchor`.
> Player arrival is derived from the destination portal anchor.

There should no longer be a separate persistent concept of a landing position.

## Canonical portal anchor

Define a portal’s canonical anchor as follows:

* It is a block position inside the portal interior, not a frame block.
* It is in the lowest row of the portal interior, immediately above the bottom frame blocks.
* Horizontally, it is as close as possible to the center of the portal interior.
* For an odd interior width, use the single center block.
* For an even interior width, where the geometric center lies between two blocks, choose one deterministically.
* Break that tie toward the block with the more-negative world coordinate along the portal’s horizontal axis:

  * for an interior extending along X, choose the more-negative X block;
  * for an interior extending along Z, choose the more-negative Z block.
* The result must not depend on:

  * which side the player entered from;
  * frame-detection traversal order;
  * portal-facing direction;
  * hash-map or collection iteration order.

For a standard two-block-wide Nether-style portal interior, the anchor will be one of the two bottom interior blocks, chosen using the negative-coordinate tie rule.

Use precise names such as:

* `anchorBlock`;
* `sourceAnchor`;
* `destinationAnchor`;
* `requestedAnchor`;
* `generatedAnchor`.

Avoid vague or obsolete names such as:

* `landingPosition`;
* `reservedDestinationPosition`;
* `portalOrigin`, where it actually means anchor;
* `arrivalPosition`, where it is redundantly stored rather than derived.

## Portal identity

Review the existing `PortalIdentity.computeOriginBlock(...)` logic.

A registered source portal should now be identified by its canonical bottom-center interior anchor.

Do not retain two separate concepts such as:

* a hidden origin block near the portal’s visual center; and
* a bottom-center portal anchor.

There should be one canonical identity position: the bottom-center interior anchor defined above.

When a player enters a physical portal:

1. Detect the complete portal frame and interior.
2. Iterate through the source portal records registered in that dimension.
3. Find records whose stored source anchor is enclosed by the detected portal interior.
4. If exactly one registered anchor is enclosed, that is the registered source portal.
5. If no registered anchor is enclosed, treat the physical portal as a new source portal.
6. Preserve the existing explicit policy for multiple enclosed registered anchors rather than relying on arbitrary collection ordering.

A rebuilt or reshaped portal represents the same registered source portal whenever its interior still encloses the stored source anchor.

Update documentation, logs, method names, record fields, and tests to use this definition.

## Remove the separate landing-position concept

There should no longer be a workflow like:

```text
find landing position
generate portal nearby
store landing position
teleport to generated portal
```

Instead, the flow should be:

```text
find candidate destination portal anchor
attempt to generate a portal whose canonical anchor is exactly that block
if generation fails, try another candidate anchor
after generation succeeds, verify the generated anchor
store the route using that anchor
teleport to that anchor
```

The selected candidate is not merely a general search location or approximate landing area. It is the exact requested canonical anchor of the destination portal.

Rename, simplify, or remove `PortalReservation` as appropriate.

If a reservation object remains, it should represent a reserved candidate portal anchor, not an independent player landing position.

Do not preserve both:

* `landingPosition`; and
* `destinationAnchor`

unless they serve genuinely different, documented purposes. Under this design, they should not.

## Destination search

Revise destination selection so that it searches for candidate portal anchors.

For each candidate anchor, determine whether a complete destination portal can be safely generated with that exact canonical anchor.

The search process may consider:

* terrain height;
* portal orientation;
* replaceable blocks;
* floor support;
* player clearance;
* nearby hazards;
* existing registered portal anchors;
* chunk availability;
* any existing frontier-distance or biome-selection rules.

However, destination search must ultimately produce an exact candidate `BlockPos` intended to become the generated portal’s canonical anchor.

A candidate should be rejected if a portal cannot be generated with that exact anchor.

Do not accept a candidate and then silently move the generated portal elsewhere.

Do not resolve an unsafe anchor by placing the player outside the portal.

Try another candidate anchor instead.

## Generated portal alignment

Change `PortalPlacementService.generateReturnPortal(...)`, or replace it with a more accurately named method, so that it takes an exact requested portal anchor.

For example:

```java
Optional<GeneratedPortal> tryGeneratePortalAtAnchor(
    ServerLevel level,
    BlockPos requestedAnchor
)
```

The placement service may try multiple supported portal orientations around that anchor.

It may return failure if no supported orientation can be constructed safely.

It must not report success unless all of the following are true:

* the requested anchor is inside the generated portal interior;
* the requested anchor is in the lowest interior row;
* the requested anchor is horizontally the canonical center block under the negative-coordinate tie rule;
* computing the canonical anchor from the returned generated frame produces the exact requested anchor.

Enforce this invariant explicitly:

```java
BlockPos generatedAnchor =
    PortalIdentity.computeAnchorBlock(generatedPortal.frame());

if (!generatedAnchor.equals(requestedAnchor)) {
    // Treat this as generation failure or an internal error.
}
```

Do not allow the placement service to generate the portal “near” the requested anchor and return a different anchor.

If an orientation cannot satisfy the invariant, try another orientation or reject the candidate.

## Portal generation must happen before route registration

Because the destination block position is the canonical anchor of the actual generated portal, the destination portal must be successfully generated before the final route records can be created.

The founding flow should be approximately:

```text
detect original physical portal
compute original source anchor
confirm it is not already registered

search candidate destination anchors:
    attempt to generate destination portal at candidate anchor
    verify generated portal anchor equals candidate anchor
    verify generated portal does not enclose an existing registered source anchor
    compute all route records
    commit registry records
    teleport player
```

Only after a destination portal has been generated successfully and its anchor verified should the source portal’s destination anchor be known and persisted.

The registry relationship should be:

```text
original source anchor
    -> generated destination anchor

generated destination anchor
    -> original source anchor
```

## Transactional behavior and rollback

Generating the destination portal before registration introduces a partial-failure risk.

Make portal founding rollback-safe.

Before modifying the destination area, capture enough information to restore every changed block and relevant block entity.

If any operation after generation fails, including:

* generated-anchor verification;
* existing-anchor conflict checks;
* reverse-record construction;
* registry insertion;
* registry validation;
* any other final founding validation;

then:

* remove any partially added registry records;
* restore all destination blocks changed during portal generation;
* leave the original source portal unregistered;
* do not teleport the player;
* report/log the failure clearly.

Do not leave an orphaned generated portal in the world when route creation fails.

The operation does not need a formal database transaction, but it should behave transactionally from the world and registry’s perspective.

## GeneratedPortal representation

Simplify `GeneratedPortal`.

It should expose the actual generated frame and canonical anchor, for example:

```java
record GeneratedPortal(
    PortalFrame frame,
    BlockPos anchorBlock,
    PortalPlacementRollback rollback
) {}
```

The exact representation is flexible, but maintain this invariant:

```java
anchorBlock.equals(
    PortalIdentity.computeAnchorBlock(frame)
)
```

Avoid redundantly storing an arrival position when it can always be derived from the anchor.

If an arrival helper is useful, derive it:

```java
Vec3 arrivalPosition() {
    return Vec3.atBottomCenterOf(anchorBlock);
}
```

Do not independently calculate or persist an arrival position that can diverge from the anchor.

## Arrival behavior

For every registered route, derive player arrival from the stored destination anchor:

```java
Vec3 arrivalPosition =
    Vec3.atBottomCenterOf(destinationAnchor);
```

This means the player arrives:

* horizontally centered in the destination anchor block;
* at the bottom of that block;
* inside the lowest row of the portal interior.

Use the same rule for:

* the first trip immediately after portal founding;
* all subsequent trips through the original portal;
* reverse travel through the generated portal.

The first trip must not use a special placement result that differs from later travel.

The following invariant should hold:

```text
first-trip arrival
    == subsequent registered-route arrival
    == Vec3.atBottomCenterOf(stored destination anchor)
```

Do not perform a separate safe-arrival search after portal generation.

Safety should be guaranteed by selecting an anchor where the complete portal and player arrival can safely exist.

If the anchor is unsafe, reject it before registration.

## Arrival safety

When evaluating a candidate portal anchor, verify at least:

* a valid portal frame can be built with that exact canonical anchor;
* all required frame and interior blocks can be placed under the existing replacement policy;
* the portal interior will contain portal blocks after activation;
* the anchor block is not occupied by a nonreplaceable obstruction;
* the player can occupy `Vec3.atBottomCenterOf(anchor)` without intersecting solid collision shapes;
* there is sufficient headroom for a standing player;
* appropriate floor support exists beneath the bottom portal interior, or the generator creates support according to the project’s intended placement policy;
* the player will not immediately fall into an unsafe cavity;
* the generated portal does not enclose an existing registered source anchor.

Do not move the arrival position outside the destination portal as a fallback.

Reject the anchor and continue searching.

## SourcePortalRecord

Revise the persisted route record so its semantics are explicit.

Conceptually, it should contain:

```java
record SourcePortalRecord(
    ResourceKey<Level> sourceDimension,
    BlockPos sourceAnchor,
    ResourceKey<Level> destinationDimension,
    BlockPos destinationAnchor
) {}
```

Adapt this to the project’s existing record structure as appropriate.

The important points are:

* `sourceAnchor` is the canonical anchor of the physical source portal;
* `destinationAnchor` is the canonical anchor of the physical destination portal;
* neither value is an approximate coordinate;
* neither value is a separate landing spot;
* arrival is derived from `destinationAnchor`.

Do not store both the destination anchor and a separately calculated destination arrival coordinate.

The dimension plus source anchor should remain the logical identity of a source portal record.

If `UUID portalId` is still present and is not required for external references or routing, remove it as part of this cleanup.

Do not add migration code for existing registry data. Existing development registry files will be deleted and recreated from scratch.

## Naming cleanup

Review the affected code for obsolete terminology.

Prefer:

```text
portal anchor
source anchor
destination anchor
candidate anchor
requested anchor
generated anchor
tryGeneratePortalAtAnchor
computeAnchorBlock
findCandidateAnchors
destinationAnchor
```

Avoid using `landing` for the destination coordinate, because the destination is now defined by the actual portal.

Avoid using `origin` when the value means canonical anchor, unless there is a clearly distinct remaining concept that genuinely requires the term.

## Tests

Add or update focused unit and game tests covering at least the following.

### Anchor calculation

1. Odd-width portal interiors select the single center block.
2. Even-width portal interiors apply the negative-coordinate tie rule.
3. X-axis interiors choose the more-negative X block on a tie.
4. Z-axis interiors choose the more-negative Z block on a tie.
5. The anchor is always in the lowest interior row.
6. Anchor calculation is independent of entry side.
7. Anchor calculation is independent of portal-facing direction.
8. Anchor calculation is independent of frame traversal order.

### Portal identity

9. A registered source portal is found when its stored anchor is enclosed by the entered portal.
10. A rebuilt frame still identifies the same source portal when it encloses the stored anchor.
11. A frame that no longer encloses the stored anchor is treated as a different source portal.
12. Multiple enclosed registered anchors follow the project’s explicit deterministic or rejection policy.

### Destination generation

13. A generated portal’s canonical anchor exactly equals the requested candidate anchor.
14. Both supported portal orientations preserve the requested anchor.
15. An unsuitable orientation is rejected rather than shifting the anchor.
16. An unsuitable candidate is rejected and another candidate can be tried.
17. Generation never reports success with a different canonical anchor.

### Route persistence and arrival

18. The outbound source record stores the generated destination portal’s canonical anchor.
19. The reverse source record stores the original source portal’s canonical anchor.
20. First travel arrives at `Vec3.atBottomCenterOf(destinationAnchor)`.
21. Subsequent travel arrives at the exact same `Vec3`.
22. Reverse travel arrives at `Vec3.atBottomCenterOf(originalSourceAnchor)`.
23. No separate persisted landing coordinate exists that can disagree with the destination anchor.

### Failure and rollback

24. If generated-anchor validation fails, all generated blocks are restored.
25. If an existing registered anchor is found inside the generated portal, all generated blocks are restored.
26. If registry insertion fails, all generated blocks are restored and no partial records remain.
27. A failed founding attempt leaves neither an orphaned destination portal nor a half-created route.
28. The player is not teleported when founding fails.

### Compatibility

29. Existing vanilla Nether portals continue to use vanilla behavior.
30. The implementation remains server-side-only.
31. Shared logic remains in `common`.
32. Fabric and NeoForge builds and tests both pass.

## README and documentation

Update the README and code documentation to describe the new model:

* A portal is identified by a canonical anchor in the bottom row of its interior.
* A route maps one portal anchor to another portal anchor.
* Destination search finds candidate portal anchors.
* The destination portal must be generated successfully before the route can be registered.
* The generator must place the portal with its canonical anchor exactly at the requested candidate block.
* Player arrival is always derived from the destination anchor and is inside the destination portal.
* There is no separate persistent landing-position concept.

Do not add migration logic for old development registries.

Keep the implementation loader-neutral in `common`, preserve the server-side-only design, and run the complete build and test suite for both Fabric and NeoForge.

## Outcome

- Replaced the previous mixed destination model with a strict anchor-to-anchor route model.
- Removed persistent landing-position semantics and the old reservation object.
- Canonical portal identity now uses the bottom-center interior anchor with negative-coordinate tie-breaking.
- Destination search now yields exact candidate portal anchors, and portal generation succeeds only when the generated anchor exactly matches the requested anchor.
- Founding travel now generates the destination portal before route registration and rolls back changed blocks if any later validation or registry write fails.
- Persistent source-portal records now store `sourceDimension`, `sourceAnchor`, `destinationDimension`, and `destinationAnchor`, with no UUID and no migration path for older development data.
- Updated unit tests cover anchor selection, anchor-based layout generation, and registry serialization/matching for the new route model.
- Verified with `./gradlew :common:test`, `./gradlew build`, `./gradlew :fabric:runServer`, and `./gradlew :neoforge:runServer`.
- Remaining limitation: teleportation still stays within the same dimension as the origin portal in the current implementation.
