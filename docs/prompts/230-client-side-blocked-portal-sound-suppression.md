# Pass 230: Client-Side Blocked Portal Sound Suppression

Status: Completed
Date: 2026-07-13

## Prompt

can you also suppress the nether portal teleporting sounds?

## Outcome

- Added a client-only `LocalPlayer` mixin that suppresses the vanilla local portal trigger sound when the same blocked-entry predicate is true: the local player is touching a valid Forever World portal and their inventory is not empty.
- Reused `ForeverWorldPortalsClient.shouldSuppressPortalEffect(LocalPlayer)` so the sound and visual suppression paths share one heuristic and one portal-detection path.
- Left all server-side logic, teleportation behavior, and ordinary Nether portal sounds unchanged.
