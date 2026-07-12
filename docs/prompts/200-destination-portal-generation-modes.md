# Destination Portal Generation Modes

Status: Completed
Date: 2026-07-12

## Prompt

Please add configurable destination-portal generation modes to Forever World Portals.

## Configuration

Add a new server-prefixed configuration key:

```properties
server.destinationPortalMode=broken
```

Represent this as an enum with these values:

```java
NONE
BROKEN
COMPLETE
```

Configuration parsing should be case-insensitive. Document the accepted property values as:

```text
none
broken
complete
```

Use `broken` as the default.

Invalid values should produce a clear warning and fall back to `broken`.

Do not implement this as multiple booleans. These are mutually exclusive destination-generation policies.

## Mode behavior

### `NONE`

After a new source portal finds a destination:

* Teleport the player to the selected arrival location.
* Do not generate any destination portal structure.
* Do not register a destination/source portal at the arrival location.
* The original source portal should still store its outbound destination as usual.
* There is no automatic return route.

The landing area must still be safe and suitable for the player.

### `BROKEN`

Generate a broken destination portal at the selected destination.

The broken portal should:

* Use the same dimensions, position, orientation, anchor conventions, and placement validation as a complete generated Forever World Portal.
* Have its visible frame made from a normal, blastable placeholder block.
* For now, use `cobbled_deepslate` for the visible broken frame.
* Contain no active portal blocks.
* Not be immediately usable.

The player must repair the visible frame by replacing it with diamond blocks and then light it normally before it can be used.

Register this destination portal immediately, even though it is broken. It must be linked back to the original source portal.

When the player repairs and lights a diamond frame enclosing the registered destination portal's origin block, the existing registered portal must be recognized and activated. It must not be treated as a new unrelated portal or assigned a new destination.

### `COMPLETE`

Generate a complete Forever World Portal at the destination.

It should:

* Use the normal diamond-block frame.
* Generate active portal blocks.
* Be registered immediately.
* Be linked back to the original source portal.
* Be ready for immediate return travel.

## Destination subfoundation

For both `BROKEN` and `COMPLETE`, place a hidden four-block crying-obsidian subfoundation immediately beneath the bottom row of the generated portal frame.

This should be a `4 × 1` line:

* one crying obsidian block directly beneath each of the four bottom frame blocks;
* aligned with the portal frame's horizontal axis;
* one block below the bottom frame row;
* therefore normally underground or beneath the structure's floor surface.

This is a physical recovery marker. If the visible frame is destroyed, the surviving crying-obsidian line should show the player exactly where and in which orientation the portal frame must be rebuilt.

Do not use a `2 × 2` square. Do not place the marker around the portal anchor. It is specifically a four-block linear footing directly beneath the portal's four bottom frame blocks.

The crying obsidian is not part of portal-frame validation. Players should not need to expose, replace, or interact with it to activate the portal.

Do not generate this subfoundation in `NONE` mode because no destination portal location is being established.

## Placement and terrain handling

Portal placement must account for both:

* the visible portal structure; and
* the buried crying-obsidian subfoundation.

Do not choose a placement where the subfoundation would overwrite protected or inappropriate blocks according to the project's existing placement rules.

Generate the full structure before finalizing the destination position stored by the source portal. The stored outbound destination must remain the portal arrival/anchor position inside the generated destination frame, using the project's current destination-anchor conventions.

The destination portal structure must still be entirely valid and safely accessible after terrain clearing and generation.

## Registry semantics

Preserve the current portal identity model:

* A registered portal is identified by its frame enclosing a specific origin `BlockPos`.
* Portal matching should iterate registered portals and determine whether a registered origin block lies within the entered frame.
* Do not reintroduce region-based identity.
* Do not add portal UUIDs if they are not otherwise required by the current registry.
* Do not add migration logic for old registry data. Existing development-world registry files can be deleted.

For `BROKEN` and `COMPLETE`, register the generated destination portal using the origin block enclosed by the generated frame.

For `BROKEN`, registration must not depend on the placeholder frame continuing to exist. If the placeholder blocks are destroyed, the registered destination portal should remain in persistent state so that rebuilding the diamond frame in the correct location restores the link.

For `NONE`, do not register a portal at the destination.

## Logging

Add useful logs that include:

* selected destination portal mode;
* generated destination portal position and orientation;
* whether a broken or complete destination portal was generated;
* whether the destination portal was registered;
* placement of the crying-obsidian subfoundation;
* clear warnings when generation fails.

Avoid noisy per-block logging unless debug logging is enabled.

## Documentation and tests

Update the configuration documentation and example properties file.

Add or update tests for at least:

1. Default mode is `BROKEN`.
2. Case-insensitive enum parsing.
3. Invalid values fall back to `BROKEN`.
4. `NONE` generates and registers no destination portal.
5. `BROKEN` generates a cobbled-deepslate frame with no portal blocks.
6. `BROKEN` registers the destination portal and links it back to the source.
7. A repaired broken portal matches the existing registered portal rather than creating a new one.
8. `COMPLETE` generates a diamond frame with active portal blocks.
9. `COMPLETE` registers and links the destination portal.
10. `BROKEN` and `COMPLETE` generate exactly four crying-obsidian blocks in a straight line beneath the bottom frame row.
11. `NONE` generates no crying obsidian.
12. The implementation works for both portal orientations.
13. Registry persistence survives a server restart.

Keep loader-specific entry points thin and place shared behavior in common code wherever possible. Preserve compatibility with both Fabric and NeoForge.
