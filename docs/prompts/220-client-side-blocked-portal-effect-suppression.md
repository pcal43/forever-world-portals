# Pass 220: Client-Side Blocked Portal Effect Suppression

Status: Completed
Date: 2026-07-13

## Prompt

Implement client-side suppression of the vanilla Nether portal visual effect when a player is standing in a Forever World portal while carrying items.

Before making changes, carefully inspect the existing implementation, including:

* the current client mixins used for Forever World portal rendering
* the portal/frame detection helpers
* the server-side empty-inventory logic
* the client initialization code for both Fabric and NeoForge
* the current separation between common and client-only code

## Goal

When the client has the Forever World Portals mod installed, suppress the vanilla "wavy" Nether portal screen effect whenever:

1. the local player is standing in a valid Forever World portal, and
2. the local player's inventory is not empty.

This is purely a cosmetic client-side enhancement.

Do **not** add any networking, custom packets, server/client synchronization, or protocol negotiation.

Do **not** modify any server-side logic.

## Important

This implementation is intentionally heuristic.

It is acceptable that the client suppresses the portal effect even if the connected server has disabled `requireEmptyInventory`.

We will address server-authoritative synchronization in a future version. Do not attempt to solve that problem in this change.

## Implementation

Inspect the Minecraft 1.21.x client source and mappings to determine where the vanilla portal visual intensity/progress is computed and supplied to rendering.

Prefer a narrow mixin that causes the effective portal visual progress to be zero while this predicate is true:

```
local player is touching a Forever World portal
&&
local player inventory is not empty
```

Avoid cancelling large rendering methods if a smaller interception point exists.

The implementation should:

* affect only the local player
* leave Nether portals completely unchanged
* leave all other screen effects unchanged
* avoid modifying the player's portal timer or teleport logic
* avoid broad redirects that are likely to conflict with other rendering mods

If Minecraft renders multiple portal effects from separate code paths, first look for a shared progress value that can be intercepted. Only use multiple mixins if there is no cleaner solution.

## Portal detection

Reuse the existing Forever World portal detection logic wherever practical.

Do not duplicate complicated portal-identification code solely for this feature.

If a small helper extraction would reduce duplication between the portal-color code and this feature, make that refactor.

## Inventory check

Reuse existing utility methods if available.

Otherwise implement a small helper that returns true if the player is carrying any items.

Do not duplicate inventory traversal logic throughout the client.

## Loader structure

Keep common logic in the shared module whenever possible.

Limit loader-specific code to client registration and mixin declarations.

Follow the same architectural style used for the recently added client-side portal coloring.

## Testing

Verify:

* standing in a Forever World portal with items suppresses the portal animation
* standing in a Forever World portal with an empty inventory behaves normally
* ordinary Nether portals remain completely vanilla
* single-player still works
* dedicated server with optional client mod still works
* both Fabric and NeoForge builds pass

## Cleanup

Remove any dead code introduced during development.

Document the implementation with concise comments where appropriate.

Add a new prompt/history document under `docs/prompts` describing this feature, following the project's existing convention.

At the end, summarize:

* which client class(es) were mixed into
* why those injection points were chosen
* any helper refactors that were performed
* build/test results

## Outcome

- Added a shared-client heuristic that suppresses the vanilla portal screen effect for the local player when they are touching a valid Forever World portal and their inventory is not empty.
- Mixed into `GameRenderer` to zero only the portal-effect intensity reads used by the client-side screen-warp rendering path, leaving portal timers, sounds, and teleportation flow unchanged.
- Reused the existing client-side Forever World portal detection helper and the shared `PortalInventoryAccess` utility instead of duplicating portal or inventory logic.
- Added no networking or server synchronization; the suppression is intentionally client-local and heuristic.
- Verified compilation with `./gradlew --no-daemon :client:compileJava :fabric:compileJava :neoforge:compileJava --console=plain`.
- Verified the full build with `./gradlew --no-daemon clean build --console=plain`.
