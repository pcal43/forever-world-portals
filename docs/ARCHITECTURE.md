# Architecture

This document describes the internal architecture of **Forever World Portals**. It is intended for contributors and maintainers.

User-facing documentation is provided in `README.md`. Development workflows are documented in `DEVELOPMENT.md`.

---

# Design Goals

Forever World Portals is designed around a few core principles:

* Prefer server-side implementations whenever practical.
* Preserve compatibility with completely vanilla clients.
* Reuse Minecraft's existing mechanics instead of replacing them.
* Build on vanilla portal behavior rather than introducing custom portal blocks.
* Keep platform-specific code minimal by placing nearly all gameplay logic in shared code.

---

# Module Layout

The project is divided into several modules.

## common

Contains nearly all gameplay logic, including:

* portal discovery
* destination generation
* persistence
* configuration
* data pack loading
* teleportation
* attunements

This module should remain independent of Fabric- or NeoForge-specific APIs whenever practical.

## client

Contains optional client-side enhancements.

Current responsibilities include:

* rendering Forever World portals with a configurable tint
* suppressing the vanilla portal screen-warp effect when portal travel is blocked

The mod is fully functional without this module.

## fabric

Provides Fabric-specific initialization and platform integration.

## neoforge

Provides NeoForge-specific initialization and platform integration.

Loader modules should remain thin and primarily delegate into shared code.

---

# Server-Side Architecture

Gameplay is coordinated through a central service responsible for portal behavior.

Responsibilities include:

* portal activation
* portal discovery
* route creation
* teleportation
* persistence
* configuration access

Supporting classes implement focused responsibilities while the central service coordinates the overall workflow.

---

# Portal Identity

Forever World Portals identify portals by an **anchor block**, not by the frame itself.

Each valid portal has exactly one canonical anchor located within the portal interior.

The anchor:

* lies on the lowest interior row
* is as close as possible to the horizontal center
* breaks ties toward the more-negative world coordinate

Portal routes are stored as mappings between source and destination anchors.

This approach provides stable portal identity even if the frame is temporarily removed and rebuilt, provided the rebuilt frame still encloses the stored anchor.

---

# Route Persistence

Routes are stored as anchor-to-anchor mappings.

When a portal is first resolved:

1. the source anchor is identified
2. a destination is generated
3. a route from source to destination is stored
4. a reverse route from destination back to the source is also stored when a destination portal is generated

Subsequent travel always consults the stored route rather than repeating destination selection.

Arrival positions are derived from the destination anchor rather than stored separately.

---

# Portal Discovery

The first time an unresolved portal is entered:

1. candidate search locations are generated using a square spiral
2. Minecraft's built-in biome search is used to locate suitable terrain near each candidate
3. candidate portal locations are evaluated
4. the first successful destination is accepted and persisted

The search algorithm intentionally reuses Minecraft world generation facilities whenever practical rather than maintaining independent biome-location logic.

---

# Destination Generation

Portal generation uses configurable placement modes.

Depending on configuration, the destination may contain:

* no portal
* a broken portal frame
* a complete Forever World portal

Once established, destinations are permanent unless the world data is modified.

---

# Attunements

Portal destinations may be influenced through attunements.

Attunements are loaded from data packs and define mappings between offered items and destination biome targets.

Definitions are merged according to standard data pack priority rules.

Validation occurs during reload to ensure invalid definitions fail immediately rather than producing inconsistent runtime behavior.

---

# Configuration

Runtime configuration is loaded from:

```text
config/fwportals.properties
```

Default values are supplied by the bundled:

```text
fwportals-default.properties
```

The default configuration serves both as the template copied into new worlds and as the fallback value source for omitted settings.

Existing user configuration files are never rewritten automatically.

---

# Client Enhancements

The optional client module provides purely visual improvements.

Current features include:

* configurable portal coloration
* suppression of the vanilla portal screen distortion when portal travel is blocked

These enhancements do not affect gameplay and are not required for compatibility with a Forever World Portals server.

---

# Data-Driven Content

Attunements are defined through data packs rather than hardcoded Java logic.

This allows server operators and modpack authors to customize portal behavior without modifying the mod itself.

Current data-driven content includes:

* item-to-biome mappings
* portal colors
* optional particle definitions

---

# Design Philosophy

Whenever practical, the implementation prefers:

* existing Minecraft systems over custom implementations
* persistent identities over transient calculations
* data-driven behavior over hardcoded logic
* shared code over loader-specific implementations
* simple, maintainable designs over highly generalized abstractions

The project intentionally evolves in small, self-contained implementation passes so that the codebase remains functional throughout development.
