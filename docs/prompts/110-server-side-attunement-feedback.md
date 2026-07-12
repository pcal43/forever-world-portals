# Pass 110: Server-Side Attunement Feedback

Status: Completed
Date: 2026-07-12

## Prompt

Please implement clear server-side-only feedback for portal attunement and destination resolution.

First inspect the current implementation and the existing prompts in `doc/prompts` so this fits the current portal identity, unresolved-portal registry state, datapack-driven attunement definitions, and thrown-item interaction. Preserve the existing architecture wherever practical.

Save a copy of this implementation prompt in `doc/prompts`, following the existing filename and numbering convention.

# Goal

Improve player feedback when:

1. a recognized attunement item is accepted by an unresolved Forever World Portal;
2. an existing attunement is replaced;
3. the portal begins resolving its destination on first entry.

The implementation must remain server-side only and must work with unmodified vanilla clients.

Do not add custom blocks, custom particles, custom sounds, custom rendering, persistent display entities, or client-side code.

# Accepted offering feedback

When a recognized attunement item enters an unresolved Forever World Portal and is accepted:

1. Consume exactly one item from the `ItemEntity`, preserving the current behavior for stacks.
2. Persist the selected attunement immediately, exactly as the current implementation does.
3. Spawn a compact vanilla particle effect at or near the consumed item position.
4. Play a vanilla confirmation sound at the portal.
5. Send an action-bar message to the player who threw the item when that player can be identified.

Use a restrained effect rather than a large explosion of particles.

A reasonable default effect is:

* a small burst of `REVERSE_PORTAL` particles;
* optionally a small number of item particles representing the consumed item, if this is straightforward with the current Minecraft API;
* one vanilla charging or completion sound, preferably something like the respawn-anchor charge sound or end-portal-frame fill sound.

Choose the specific sound and particle APIs that are stable and appropriate for the current target Minecraft version.

Do not delay persistence or consumption in order to animate the item. Avoid introducing a ticking animation, suction state machine, scheduled callback, or temporary persistent entity.

It is acceptable to move the item entity to the portal center immediately before consuming it if that produces noticeably better feedback without complicating the logic, but this is optional.

# Action-bar message

When the owner or thrower of the item can be identified as an online player, send that player an action-bar message:

```text
Portal attuned to Cherry Grove
```

Use the same message whether this is the first attunement or a replacement. Do not include the previous attunement.

Do not broadcast this message globally.

If the throwing player cannot be identified, still perform the particle and sound feedback and accept the item normally.

The message should use the attunement’s logical ID converted into a readable display name:

* `cherry_grove` -> `Cherry Grove`
* `old_growth_taiga` -> `Old Growth Taiga`

If the logical ID is namespaced, use only its path component for this automatic display name.

Implement this formatting in a small reusable helper rather than embedding it directly in the interaction handler.

Do not add a new required JSON field solely for the display name.

# Destination-resolution feedback

When a player first enters an unresolved portal and the stored attunement is about to be used for destination selection, send that player an action-bar message:

```text
Seeking a Cherry Grove frontier...
```

For an unattuned portal using the required `default` attunement entry, use:

```text
Seeking a new frontier...
```

Send this message when destination resolution actually begins, not every tick while the player remains inside the portal.

Do not change the biome search, spiral search, distance constraints, destination registry behavior, or teleportation flow.

# Unrecognized items

When an item is not recognized as an attunement offering:

* do not consume it;
* do not modify portal state;
* do not play rejection sounds;
* do not spawn rejection particles;
* do not send a message.

The absence of consumption should remain the indication that the portal did not accept the item.

# Replacement behavior

Throwing a second recognized attunement item into the same unresolved portal must continue to replace the stored attunement immediately.

The replacement should produce the same:

* particle burst;
* confirmation sound;
* action-bar message.

Do not add separate replacement-specific behavior or messages.

# Resolved portals

A portal whose destination has already been resolved must not accept or consume attunement items.

Preserve the current resolved-portal behavior.

Do not emit acceptance feedback for resolved portals.

# Server-side and loader requirements

Keep all feedback server-authoritative and compatible with vanilla clients.

Reuse common code wherever possible. Keep Fabric- and NeoForge-specific entry points thin.

Use existing abstractions for:

* portal detection;
* portal identity;
* unresolved portal state;
* attunement registry lookup;
* destination resolution.

Do not duplicate portal matching or registry logic merely to add feedback.

# Suggested structure

Prefer small focused helpers or services, for example:

* a helper that converts an attunement ID into a readable name;
* a helper that emits the accepted-offering particles and sound;
* a helper that sends action-bar messages.

Do not introduce a large new subsystem.

# Testing

Add or update tests where practical for:

1. formatting logical IDs into readable names;
2. using `Seeking a new frontier...` for the default attunement;
3. using the readable attunement name for non-default destination resolution;
4. ensuring unrecognized items remain unconsumed;
5. ensuring resolved portals do not accept offerings;
6. preserving replacement behavior;
7. consuming exactly one item from a recognized stack.

For particle and sound calls that are difficult to unit-test directly, keep the surrounding decision logic testable and verify through existing integration or game-test facilities if available.

# Documentation

Update relevant documentation to describe:

* accepted offerings produce particles, sound, and an action-bar message;
* replacing an unresolved portal’s attunement produces the same feedback;
* entering an unresolved portal displays the destination-search message;
* unrecognized items are ignored without feedback;
* all feedback remains server-side only.

# Validation

Run the project’s normal formatting, tests, and build checks for all supported loaders.

At the end, summarize:

* the files changed;
* the exact particle and sound selected;
* how the throwing player is identified;
* where the two action-bar messages are emitted;
* any target-version API limitations encountered.
