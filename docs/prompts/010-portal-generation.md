# Forever World Portals — Pass 1 (Diamond Portal Activation)

This is **Pass 1** of the implementation.

Build on the existing project scaffold created during Pass 0.

Do **not** regenerate the project structure.

Preserve the existing architecture, singleton service, configuration system, build structure, and multi-loader organization.

The goal of this pass is to implement **diamond-block Forever World portals** while **not** implementing any teleportation or destination selection.

---

# Objectives

Implement the ability to:

* detect a valid diamond-block portal frame
* activate it using flint and steel
* fill the portal interior with vanilla Nether portal blocks
* distinguish it from an ordinary Nether portal
* recognize the portal later when an entity enters it

Do **not** implement travel yet.

This pass is solely about portal construction, activation, and identification.

---

# Vanilla-first implementation

Before writing code, inspect the mapped vanilla Minecraft implementation for:

* Nether portal lighting
* portal frame validation
* portal shape detection
* portal block placement
* portal maintenance after frame changes

Prefer adapting or reusing vanilla logic where practical.

Avoid reimplementing Nether portal geometry from scratch unless required by API limitations.

The resulting behavior should match vanilla Nether portals wherever possible, except that the frame material is diamond blocks instead of obsidian.

---

# Portal geometry

Support the same geometry rules as vanilla Nether portals.

Requirements:

* vertical rectangle
* X or Z axis
* same minimum interior dimensions as vanilla
* same maximum dimensions as vanilla
* complete diamond-block frame
* air-filled interior before activation

Do not support non-rectangular frames.

Do not support horizontal portals.

If vanilla geometry rules change in future Minecraft versions, prefer inheriting those rules naturally rather than duplicating constants.

---

# Activation

A valid frame should activate using:

* flint and steel

When activated:

* fill the interior using vanilla Nether portal blocks
* preserve vanilla portal appearance
* preserve vanilla portal sounds
* preserve vanilla portal particles

Do **not** introduce:

* custom portal blocks
* custom textures
* custom colors
* custom particles
* custom sounds

The mod should remain completely server-side.

---

# Ordinary Nether portals

Normal obsidian Nether portals must continue behaving exactly as vanilla.

The mod must not interfere with:

* portal construction
* portal lighting
* portal maintenance
* Nether travel

unless the portal has a valid diamond-block frame.

---

# Forever World portal recognition

Implement reusable logic that can determine whether a portal block belongs to a valid Forever World portal.

This should **not** rely solely on the individual portal block being entered.

Instead, determine the surrounding frame and identify the portal from that.

Create a reusable frame detector suitable for later passes.

The detector should return information such as:

* frame axis
* frame dimensions
* frame block positions
* interior portal positions
* a stable frame anchor position suitable for future persistence

No persistence is required yet.

---

# Portal lifecycle

Handle frame changes gracefully.

If a diamond frame is broken:

* the portal should collapse using vanilla behavior where practical

No registry updates are necessary because persistence has not yet been implemented.

---

# Entity handling

Detect when entities enter a Forever World portal.

For this pass:

* recognize the portal
* recognize the entering entity
* log that entry occurred

Do **not** teleport anything.

Do **not** suppress vanilla portal cooldown logic yet.

Do **not** implement inventory changes.

Do **not** modify player state.

This detection should simply prove that later travel hooks can be attached cleanly.

---

# Configuration

Expand the existing configuration.

Add only the settings needed for this pass.

At minimum:

```properties
frameBlock=minecraft:diamond_block
activationItem=minecraft:flint_and_steel
```

Use the existing typed configuration system.

Validate configuration values.

Continue creating sensible defaults when the configuration file does not exist.

Do not add speculative configuration for later portal behavior.

---

# Architecture

Continue following the architecture established in Pass 0.

Gameplay behavior should continue flowing through the central singleton service.

Complex logic should live in focused helper classes rather than one enormous singleton.

Suggested classes include:

* PortalFrameDetector
* PortalActivationService

Use names that fit the existing project conventions.

Avoid introducing abstractions that are only needed for future implementation passes.

---

# Logging

Use the project's normal logger.

Log useful events such as:

* successful portal activation
* invalid activation attempts
* entity entered Forever World portal

Avoid noisy logging.

---

# Explicitly out of scope

Do **not** implement:

* teleportation
* destination selection
* safe landing
* portal pairing
* persistent portal registry
* UUIDs
* world save data
* inventory handling
* commands
* advancement gating
* Ancient City activation
* Nether Star requirements
* Echo Shards
* custom blocks
* custom items
* client-side rendering
* networking

Remain focused on activation and recognition only.

---

# Testing

Add loader-independent unit tests where practical.

At minimum verify:

* valid diamond frame detection
* invalid frame rejection
* canonical frame anchor calculation
* ordinary obsidian portals remain unaffected

If the project already supports game tests, add focused game tests for:

* activating a valid diamond portal
* rejecting an invalid frame
* confirming that obsidian portals continue working normally

---

# Build verification

Run the appropriate build tasks.

Verify:

* Fabric builds successfully
* NeoForge builds successfully
* Fabric development server launches
* NeoForge development server launches

Manually verify:

* build a diamond frame
* light it with flint and steel
* portal activates
* obsidian portals continue functioning normally
* entering a diamond portal produces the expected log output
* no teleportation occurs

Fix compilation or runtime issues rather than documenting them.

---

# Documentation

Update the README.

Document:

* how to construct a diamond Forever World portal
* current implementation status
* known limitations
* that teleportation is intentionally **not** implemented yet

---

# Deliverables

At the end, provide:

1. A concise summary of the implementation.
2. A list of files added or modified.
3. Build and launch results.
4. Manual testing performed.
5. Any Minecraft API limitations discovered.
6. Any design recommendations for Pass 2.

Keep this pass intentionally narrow.

The successful outcome is:

* diamond portal frames activate correctly
* ordinary Nether portals continue working
* the mod can reliably recognize a Forever World portal
* the project is ready for persistence and teleportation in Pass 2.
