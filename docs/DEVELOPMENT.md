# Development Guide

This document is intended for contributors and maintainers of **Forever World Portals**. It describes how to build, run, test, and work on the project.

Gameplay concepts and implementation details are documented separately in `ARCHITECTURE.md`.

---

# Prerequisites

* Java 25
* Git
* A supported Gradle environment
* The `just` command runner (recommended)

---

# Building

To perform a clean build:

```sh
./gradlew clean build
```

To compile without producing release artifacts:

```sh
just compile
```

Compile only the shared module:

```sh
just compile-common
```

Create distributable jars:

```sh
just jar
```

---

# Running

Dedicated server development is the primary workflow.

Launch a Fabric development server:

```sh
just run-fabric-server
```

Launch a NeoForge development server:

```sh
just run-neoforge-server
```

Client launch recipes are also available:

```sh
just run-fabric
just run-neoforge
```

The project is designed to function as a server-side mod whenever practical. Client launches are primarily intended for testing optional client-side enhancements.

---

# Testing

Run automated tests:

```sh
just test
```

When making gameplay changes, verify both Fabric and NeoForge builds whenever practical.

Do not assume behavior is identical across loaders without testing.

---

# Configuration Development

The runtime configuration file is:

```text
config/fwportals.properties
```

Default values are provided by:

```text
fwportals-default.properties
```

The bundled defaults serve two purposes:

* they are copied into the configuration directory when a world is first initialized
* they provide default values for any configuration keys omitted from an existing user configuration

When adding configuration options:

* add the new setting to `fwportals-default.properties`
* document user-visible settings in the README
* choose sensible defaults that preserve existing behavior whenever possible

---

# Data Pack Development

Attunements are defined through data packs.

Resource location:

```text
data/<namespace>/forever_world_portals/attunements.json
```

The built-in definitions are located under the common module resources.

When extending the format:

* preserve backward compatibility when practical
* validate data during reload
* fail fast on invalid definitions

---

# Repository Structure

The project is organized into shared logic and platform-specific entry points.

Typical responsibilities are:

* **common** — shared gameplay logic, persistence, configuration, and platform-independent implementation
* **client** — optional client-side enhancements such as rendering and visual behavior
* **fabric** — Fabric entry points and platform integration
* **neoforge** — NeoForge entry points and platform integration

Keep platform-specific code as small as practical.

---

# Development Philosophy

Forever World Portals follows a server-side-first architecture.

Whenever practical:

* reuse existing Minecraft mechanics instead of replacing them
* prefer shared code over loader-specific implementations
* keep loader entry points thin
* preserve compatibility with vanilla clients
* avoid introducing client-side networking or custom content unless required by the feature

See `AGENTS.md` for the project's complete development conventions.

---

# Documentation

Documentation is written for different audiences.

* `README.md` is user-facing.
* `ARCHITECTURE.md` describes implementation and design decisions.
* `docs/prompts/` contains archived implementation prompts.
* `AGENTS.md` defines repository-wide development conventions.

When implementing new features, update documentation appropriate to the intended audience rather than duplicating information across documents.

---

# Release Checklist

Before publishing a release:

* verify successful Fabric build
* verify successful NeoForge build
* verify dedicated server startup
* verify optional client module startup
* review configuration defaults
* update user-facing documentation if behavior has changed
* update architecture or development documentation if implementation has changed
* review known limitations
