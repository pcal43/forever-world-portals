# Pass 3: Generated Return Portals

Status: Completed
Date: 2026-07-11

## Prompt

Before making any changes, read and follow the repository's `AGENTS.md`.

# Pass 3: Generated Return Portals

Implement automatic destination-side return-portal generation and persistent bidirectional portal linkage for Forever World Portals.

Archive this prompt under:

```text
docs/prompts/030-generated-return-portals.md
```

## Context

The mod already supports:

* Fabric and NeoForge
* diamond-framed Forever World portal activation
* recognition of Forever World portals
* teleportation to a distant Overworld destination
* vanilla Nether portal blocks inside the custom diamond frame
* ordinary obsidian Nether portals remaining unaffected

Inspect the current implementation before changing anything. Reuse and extend the existing architecture rather than replacing working systems unnecessarily.

The repository was originally bootstrapped from another mod, but Forever World Portals is now standalone.

Follow the existing architectural principles:

* one central singleton service
* thin Fabric and NeoForge entry points
* shared/common implementation
* server-side-first behavior
* vanilla-client compatibility
* reuse vanilla mechanics where practical
* avoid speculative abstractions
* implement this as a focused vertical slice

## Goal

When a player uses an unlinked Forever World portal for the first time, the mod should generate a corresponding diamond-framed return portal near the selected destination.

The two portals should then be registered as a persistent, bidirectionally linked pair.

Subsequent travel through either portal should take the player directly to its linked partner.

Automatic return-portal generation should be the default behavior.

## Configuration

Add a return-portal mode configuration setting.

Use an enum-like value rather than a Boolean so other behaviors can be added cleanly.

For example:

```properties
returnPortalMode=GENERATE
```

Define these conceptual modes:

* `GENERATE`

  * Automatically generate and link a destination-side return portal.
  * This is the default.
* `REQUIRE_PLAYER_BUILD`

  * Reserved for a future pass.
  * The player must construct a qualifying portal near the destination.
* `NONE`

  * Reserved for a future pass.
  * No return portal is created or linked automatically.

For this implementation pass, fully implement only `GENERATE`.

The other values may be parsed and represented if that fits the existing configuration architecture, but do not build speculative gameplay systems for them.

If a nonimplemented mode is selected, fail safely and clearly rather than silently behaving as `GENERATE`.

Use naming and configuration conventions already established in the repository.

## Founding traversal behavior

For an unlinked source portal in `GENERATE` mode:

1. Recognize the source Forever World portal and determine its canonical identity.
2. Select or reuse the destination chosen by the existing teleportation system.
3. Find a suitable nearby position for the destination portal.
4. Generate a valid diamond-framed Forever World portal at that position.
5. Determine the destination portal's canonical identity.
6. Persist a bidirectional link between the source and destination portals.
7. Teleport the player through the source portal to a safe arrival position beside or within the destination portal.
8. On future traversals, use the stored partner portal rather than selecting a new destination.

The operation should behave transactionally where practical.

Do not leave the world or registry in a partially linked state when portal placement fails.

## Destination portal structure

The generated destination portal should satisfy the same structural rules as a player-created Forever World portal.

In particular:

* use a diamond-block frame
* use vanilla Nether portal blocks for the portal interior
* use a supported portal orientation
* be recognizable by the existing Forever World portal-detection logic
* have a stable canonical frame anchor
* not be treated as an ordinary Nether portal

Prefer calling or extracting shared portal-construction and validation logic rather than duplicating frame rules.

Do not introduce a custom block.

Do not require a client-side mod.

## Portal placement

Reuse vanilla Nether portal placement concepts or implementation where practical, but do not force the code into vanilla abstractions that assume obsidian frames, Nether coordinate scaling, or ordinary Nether portal linkage.

Portal placement should:

* search near the already selected destination
* avoid placing the frame inside solid terrain
* provide adequate space for the entire frame and portal interior
* place the portal on stable ground where possible
* provide a safe place for the player to stand
* avoid obvious hazards such as lava, fire, deep unsupported drops, or immediate suffocation
* avoid generating an excessive number of chunks
* keep the search bounded and configurable where appropriate

A minimal constructed foundation is acceptable when no natural placement is available nearby.

Keep terrain modification conservative. Do not build a large platform or clear a large volume unnecessarily.

Use the existing destination or safe-landing code where appropriate. Refactor only when it creates a clear shared responsibility.

## Portal identity and persistence

The persistent portal registry should be authoritative.

Portal identity should come from the canonical frame anchor, not from an individual portal-interior block.

Persist enough information to identify both members of a link across server restarts.

A linked portal record will likely need information equivalent to:

* dimension
* canonical frame-anchor position
* frame orientation or portal axis, if required
* linked partner identity
* any existing destination/frontier metadata already tracked by the mod

Use the repository's existing persistent-state architecture if one already exists.

Do not create a second competing persistence mechanism.

Links must survive:

* server restart
* chunk unload and reload
* players leaving and rejoining

Do not store authoritative linkage only in block entities or portal blocks. Vanilla portal blocks are ephemeral and have no suitable custom block entity.

## Existing linked portals

When a player enters a portal that already has a registered partner:

* do not rerun destination selection
* do not apply held-item attunement again
* do not generate another return portal
* resolve the partner portal from the registry
* verify enough of the destination structure to travel safely
* teleport the player to the linked portal

Do not add automatic repair or regeneration of destroyed linked portals in this pass unless the current implementation already requires a small amount of validation to avoid crashes.

If the registered partner is missing or invalid, fail safely and log a useful diagnostic. Do not silently create a new frontier.

## Arrival behavior

Place the player safely relative to the destination portal.

Account for:

* portal orientation
* player bounding box
* nearby solid blocks
* immediate bounce-back into the portal
* existing portal cooldown behavior

Reuse vanilla portal arrival and relative-position logic where practical.

At minimum, the player should not:

* suffocate in the frame or terrain
* spawn over an unsupported drop
* immediately teleport back before having a chance to move
* appear at an unrelated location because of Nether portal search behavior

Preserve player rotation and relative portal positioning if that can be done cleanly with vanilla logic. Otherwise, prioritize safety and deterministic behavior.

## Failure behavior

Portal generation can fail because no suitable position is found or because block placement cannot complete.

When it fails:

* do not teleport the player
* do not create a completed link in persistent state
* avoid leaving a partially constructed destination portal
* leave the source portal available for a later retry
* emit a useful server log message
* provide player-facing feedback using an existing message mechanism, if the mod already has one

Do not crash the server.

If cleanup of partially placed blocks is necessary, track only blocks placed by this operation and restore them conservatively.

## Chunk generation and loading

Keep destination portal placement bounded.

Do not scan or generate a long continuous corridor of chunks between source and destination.

Load or generate only the limited candidate area needed for destination validation and portal placement.

Release temporary chunk tickets after the operation completes or fails.

Use loader-neutral common code where possible, with thin loader-specific hooks only where Minecraft APIs require them.

## Concurrency and duplicate activation

Guard against two players founding the same source portal simultaneously.

The implementation should not:

* generate two destination portals for one source
* assign two different partners
* create conflicting registry entries
* teleport one player before the link is committed while another operation overwrites it

Use a simple per-source in-progress guard or another mechanism consistent with the current architecture.

Avoid building a broad asynchronous job framework unless the current code already uses one.

Minecraft world mutations should remain on the server thread unless there is a well-established repository mechanism that safely does otherwise.

## Scope exclusions

Do not implement these features in this pass:

* inventory stripping
* carrying only one attunement item
* player-built return-portal matching
* rerolling destinations
* portal previews
* client-side portal recoloring
* portal repair or automatic regeneration
* commands for managing links
* unlinking or deleting frontiers
* arbitrary portal networks
* custom dimensions
* custom blocks

Held-item attunement and the existing destination-selection system should continue to behave as they currently do for the first founding traversal.

## Tests

Add focused tests where the current project structure supports them.

At minimum, cover pure or mostly pure logic such as:

* canonical portal identity
* bidirectional link creation
* partner lookup
* prevention of replacing an existing link
* serialization and deserialization of linked portal records
* configuration parsing for `returnPortalMode`
* deterministic placement-candidate calculations, if extracted into testable logic

If full integration testing of Minecraft world generation is impractical, keep the placement code modular enough to inspect and test its decision logic separately.

## Documentation

Update the README or other relevant documentation to explain:

* automatic return portals are generated by default
* the generated portal uses a diamond frame
* the source and destination become a persistent linked pair
* subsequent travel is bidirectional
* the `returnPortalMode` configuration setting
* only `GENERATE` is fully implemented in this pass, if the other modes are exposed

Do not claim unsupported behavior.

## Implementation quality

Prefer a small number of clear domain types and methods over large monolithic event handlers.

Likely responsibilities include:

* source-portal traversal coordination
* destination portal placement
* portal structure construction
* persistent portal registry
* bidirectional link establishment
* linked-partner resolution

Use the existing singleton service as the main orchestration point unless repository inspection reveals a better already-established structure.

Avoid:

* duplicating portal validation rules
* loader-specific copies of gameplay logic
* depending on client code
* treating every portal-interior block as a separate portal
* silently falling back to vanilla Nether portal behavior
* broad refactoring unrelated to this pass

## Validation

Before finishing:

1. Build all supported loader targets.
2. Run available tests.
3. Verify ordinary obsidian Nether portals still behave normally.
4. Verify lighting a diamond portal alone does not generate a destination portal.
5. Verify the destination portal is generated only when founding travel is committed.
6. Verify one destination portal is generated for one source portal.
7. Verify both portals resolve to each other after generation.
8. Verify subsequent travel does not select a new destination.
9. Verify linkage survives a server restart.
10. Verify a generation failure does not teleport the player or persist a completed link.
11. Verify vanilla clients require no custom assets or networking support.

## Completion summary

When finished, provide a concise implementation summary containing:

* files added or changed
* configuration changes
* persistent-state format changes
* portal placement approach
* linkage and failure behavior
* tests added and their results
* build results for Fabric and NeoForge
* remaining limitations or follow-up work

Do not create a Git commit unless explicitly requested.

## Outcome

- Implemented default `GENERATE` return-portal behavior with generated diamond-frame destination portals, persistent bidirectional partner resolution, and reuse of stored links on later traversals.
- Extended the existing persistent registry rather than replacing it: destination records can now remain reserved-only or become materialized physical partner portals with canonical anchors and frame metadata.
- Added bounded portal placement/construction helpers, portal-arrival resolution near generated partners, and a per-source in-progress guard to prevent duplicate founding work.
- Added configuration parsing for `returnPortalMode` and tests for config parsing, portal layout geometry, destination anchor lookup, and duplicate-link rejection.
- Ran `./gradlew test build --console=plain`, `./gradlew :fabric:runServer --console=plain`, and `./gradlew :neoforge:runServer --console=plain`.
- Deferred player-built return portals, `REQUIRE_PLAYER_BUILD`, `NONE`, inventory stripping, and full in-world integration/game-test coverage to later passes.
