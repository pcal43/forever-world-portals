# Registry Refactor: Source Portal Identity

Status: Completed
Date: 2026-07-11

## Prompt

Before making any changes, read and follow the repository's `AGENTS.md`.

# Registry Refactor: Source Portal Identity

Archive this prompt under:

```text
docs/prompts/025-source-portal-identity.md
```

## Purpose

Review the current portal registry implementation and update it to match the architecture described below.

Do **not** assume the current implementation already matches this design.

The existing implementation may contain concepts such as:

* linked portal pairs
* partner portal IDs
* endpoint objects
* exact frame-anchor identity
* destination portal lookup
* portal linkage
* temporary development structures

Inspect the current implementation first, understand its current model, then adapt it with the smallest clean set of changes necessary.

Preserve existing gameplay wherever practical.

---

# Core model

The registry stores **source portals**.

A source portal represents a persistent portal identity together with the exact destination reached when that portal is used.

Conceptually:

```text
Source Portal
        →
Exact Destination
```

The registry does **not** represent permanently linked portal pairs.

A physical diamond portal frame is simply one manifestation of a source portal.

---

# Source portal identity

Each registered source portal should conceptually contain:

```text
portalId
dimension
originBlock
destinationDimension
destinationPosition
```

Use names and Minecraft types appropriate to the existing codebase.

A stable generated identifier (for example a UUID) is preferred.

The important concept is:

> A source portal is identified by a single hidden origin block.

There is **no identity radius**.

The origin block alone defines the persistent identity.

---

# Computing the origin block

Every valid Forever World portal defines exactly one origin block.

Compute it as follows:

1. Detect the complete valid portal.
2. Determine the geometric center of the portal interior.
3. Select the **interior block** nearest that center.
4. If multiple interior blocks are equally near, break ties toward negative coordinates.

This origin block must always be one of the portal's interior blocks.

Do **not** use:

* the lower-left frame block
* the first portal block encountered
* the canonical frame anchor

The origin block should be as close as practical to the visual center of the portal.

The purpose is to make rebuilt portals behave intuitively.

---

# Portal matching

Whenever a player enters a valid Forever World portal:

1. Detect the complete portal.
2. Iterate through registered source portals in deterministic order.
3. Consider only source portals in the same dimension.
4. Determine whether the entered portal encloses the registered portal's stored origin block.

If:

* exactly one source portal is enclosed → use that source portal
* none are enclosed → treat this as an unregistered portal and follow the existing frontier-founding behavior
* multiple are enclosed → use the first deterministic match and emit a warning to the server log

The matching rule is therefore:

> A physical portal represents a registered source portal if and only if its frame encloses that source portal's stored origin block.

Do **not** compare distances.

Do **not** compute nearest portals.

Do **not** use frame anchors for portal identity.

The registry iteration order must be deterministic.

---

# Rebuilding portals

The stored origin block never changes automatically.

Destroying and rebuilding a portal should continue to resolve to the same source portal whenever the rebuilt frame still encloses the stored origin block.

If the rebuilt portal no longer encloses that origin block, it naturally becomes a new source portal.

This gives players a simple and intuitive way to create a new portal simply by moving the frame.

---

# Multiple physical portals

Multiple physical Forever World portals may intentionally enclose the same origin block.

This is expected behavior.

Every such portal should resolve to the same source portal and therefore the same destination.

Do **not** attempt to designate one portal as primary.

The registry stores source portals, not physical portal structures.

---

# Destination semantics

Each source portal stores one exact destination position.

Teleportation should use that stored destination directly.

Do **not** search for:

* a linked portal
* a partner endpoint
* the nearest portal
* any destination-side portal

Teleportation must not depend on a portal existing at the destination.

Destroying a generated return portal must not invalidate the source portal.

---

# Return portals

Automatic return-portal generation is a separate gameplay feature layered on top of the registry.

When enabled, founding travel conceptually creates two independent source portals:

```text
Source Portal A
        →
Destination B

Source Portal B
        →
Destination A
```

These source portals do **not** need to reference one another.

Do **not** require:

* partner IDs
* linked endpoints
* destination portal lookup
* portal pair traversal

Bidirectional travel exists simply because two independent source portals happen to point back toward one another.

---

# Remove pair assumptions

Inspect the current implementation for concepts such as:

* linked endpoints
* partner portals
* portal pair traversal
* endpoint lookup
* destination portal IDs

Replace or refactor these where they conflict with this architecture.

---

# Creating a source portal

When founding a new frontier:

1. Detect the complete portal.
2. Compute its origin block.
3. Ensure no registered source portal already has its origin block enclosed by this portal.
4. Reserve creation if necessary to avoid concurrent duplication.
5. Determine the destination.
6. Persist the completed source portal.

---

# Registry API

Shape the registry around source portals.

Conceptually useful operations include:

```text
findSourcePortal(...)
createSourcePortal(...)
removeSourcePortal(...)
```

Avoid APIs centered around:

```text
linkPortal(...)
resolvePartner(...)
getLinkedPortal(...)
```

Teleportation should obtain an exact destination directly from the matched source portal.

---

# Persistence

This feature is still under active development.

**Do not implement any code to migrate or read older versions of the portal registry.**

Assume the portal registry format is being defined for the first time. It is acceptable to redesign the persistent NBT structure in whatever way best supports this architecture.

I will delete any existing saved registry data and start from a fresh world or fresh registry before testing this implementation.

Favor a clean, maintainable persistent data model over backward compatibility with previous development iterations.

Do not add:

* migration code
* registry version upgrades
* legacy NBT readers
* compatibility layers for previous registry formats

The completion summary should briefly describe:

* the final persistent NBT structure
* important design decisions
* assumptions made about future compatibility

---

# Tests

Add or update tests covering:

* origin block selection
* tie-breaking toward negative coordinates
* rebuilt portals still enclosing the stored origin block
* rebuilt portals that no longer enclose the origin becoming new source portals
* multiple physical portals enclosing the same origin block
* serialization
* destination lookup without requiring a destination portal

---

# Documentation

Update documentation to explain:

* the registry stores source portals
* every source portal has one hidden origin block
* the origin block is chosen near the visual center of the portal
* rebuilding a portal that still encloses that origin preserves its identity
* moving the frame away creates a new source portal
* multiple nearby portals may intentionally represent the same source portal
* destination portals are optional
* automatic return travel is implemented by creating a second independent source portal

Avoid describing the registry as permanently linking two portal structures.

---

# Validation

Before finishing:

1. Build Fabric.
2. Build NeoForge.
3. Run available tests.
4. Verify ordinary Nether portals remain unaffected.
5. Verify rebuilding a portal around the stored origin preserves portal identity.
6. Verify rebuilding the portal elsewhere creates a new source portal.
7. Verify multiple portals enclosing the same origin share one destination.
8. Verify destroying a destination portal does not break travel.
9. Verify teleportation never performs destination-side portal lookup.

---

# Completion summary

Provide a concise summary including:

* the previous registry model
* the final source portal model
* files changed
* the final persistent NBT structure
* tests added
* build results
* remaining follow-up work

Do not create a Git commit unless explicitly requested.

## Outcome

- Replaced the pair-based portal registry with a source-portal registry keyed by one hidden origin interior block and storing one exact destination coordinate per source portal.
- Removed partner-portal lookup from ordinary travel; registered portals now teleport directly to their stored destination, while generated return travel works by creating a second independent source portal.
- Added origin-block selection logic near the visual portal center with negative-coordinate tie-breaking, plus frame-encloses-origin matching for rebuilt or overlapping physical portals.
- Simplified persistence to a fresh `sourcePortals` list format with no legacy readers or migration logic, per the prompt's development-only assumption.
- Added focused tests for origin selection, rebuilt/enclosing behavior, matching, serialization, and direct destination resolution, and ran full build plus Fabric/NeoForge dedicated-server launch verification.
