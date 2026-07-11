Build a placeholder/scaffolding Minecraft mod named **Forever World Portals**.

This is **Pass 0** only. The goal is to establish a clean, compiling, runnable multi-loader project and its core architecture before implementing any portal mechanics.

## Authoritative template

Before writing code, inspect the existing project at:

`~/dev/quicksort`

Use Quicksort as the authoritative template for both:

* build and project structure
* application architecture

Do not modify `~/dev/quicksort`.

Follow Quicksort's existing conventions for:

* Gradle structure
* shared/common code organization
* Fabric support
* NeoForge support
* Minecraft version
* Java version
* mappings
* loader versions
* dependency and version management
* run configurations
* resource processing
* logging
* configuration loading
* package organization
* formatting and coding style
* test setup
* publishing metadata
* artifact naming
* the repository `Justfile`

Do not merely copy files and rename strings blindly. Inspect how Quicksort works, preserve the useful patterns, and remove all Quicksort-specific functionality and metadata.

## Mod identity

Use the following identifiers consistently:

* Display name: `Forever World Portals`
* Mod slug / mod ID: `fwportals`
* Java base package: `net.pcal.fwportals`
* Configuration filename: `fwportals.properties`

Use `fwportals` consistently anywhere an identifier or filename is required, including as applicable:

* Fabric mod ID
* NeoForge mod ID
* artifact names
* generated JAR names
* configuration filenames
* resource directories
* mixin filenames and identifiers, if mixins are required later
* logging identifiers
* publishing metadata

Do not use `foreverworldportals` or another expanded identifier in places where the slug is expected.

## Central singleton service

Follow the central-service architecture used by Quicksort.

Create one mod-level singleton service as the main coordination point for the mod, conceptually similar to:

```java
ForeverWorldPortals.getInstance()
```

Match Quicksort's actual singleton initialization and access pattern where practical rather than inventing an unrelated design.

The central service should initially own or expose:

* the logger
* the typed configuration
* initialization state
* server lifecycle handling needed by the placeholder mod

Loader-specific entry points must remain thin. They should:

* perform loader-specific registration
* initialize or connect the shared singleton service
* provide only the narrow loader-specific adapters required by common code
* delegate common behavior to the singleton

Do not create separate Fabric and NeoForge implementations of the central service.

Do not introduce a dependency-injection framework.

Do not design speculative abstractions for portal detection, portal persistence, destination selection, teleportation, or inventory handling yet.

## Configuration

Establish the configuration mechanism during this pass, following the conventions used by Quicksort.

Use:

`fwportals.properties`

Create a typed configuration model rather than allowing gameplay code to read raw property strings.

The placeholder configuration should contain only a small number of benign settings sufficient to prove that configuration loading works. For example:

```properties
enabled=true
```

A similarly simple placeholder property is acceptable if it fits Quicksort's conventions better.

Requirements:

* create the file with documented defaults if it does not exist
* load it during the appropriate initialization lifecycle
* validate parsed values
* fall back safely to defaults when a value is malformed
* log configuration errors clearly
* avoid configuration reload support for now
* keep the parser isolated from the rest of the mod through a typed configuration object

Do not add speculative portal-related configuration fields yet.

## Justfile

Copy and adapt the repository-level `Justfile` from `~/dev/quicksort`.

Preserve Quicksort's useful workflow and naming conventions where they remain applicable.

Update it for the new project so that:

* all project names and paths refer to Forever World Portals
* all artifact and mod identifiers use `fwportals`
* Fabric and NeoForge build tasks work
* development client and server launch tasks work, where present in Quicksort
* formatting, cleaning, testing, and publishing-related recipes are adapted appropriately
* no recipe refers to Quicksort-specific packages, artifacts, directories, or functionality

Do not add speculative portal-specific recipes.

Run the relevant Justfile recipes and correct them if they do not work.

## Placeholder behavior

The mod should perform only enough runtime behavior to prove that both loaders initialize correctly.

At minimum:

* load the singleton service
* load `fwportals.properties`
* register appropriate server lifecycle hooks
* log a concise initialization message
* log when a development server starts, if that matches Quicksort's lifecycle pattern
* cleanly release any server-scoped references when the server stops

Do not add a command during this pass.

Do not add player-facing chat messages.

Do not add gameplay behavior.

## Explicitly out of scope

Do not implement any of the following:

* portal frame detection
* portal activation or lighting
* Nether portal interception
* teleportation
* destination selection
* safe landing selection
* portal generation
* portal pairing
* persistent portal records
* inventory handling
* commands
* custom blocks
* custom items
* entities
* menus or screens
* textures or models
* sounds
* dimensions
* biomes
* client-side rendering
* advancement or progression mechanics

Do not add empty placeholder interfaces or classes for these future systems unless they are strictly required by the current architecture.

## Server-side-only constraint

Preserve the intended server-side-only design.

Vanilla clients should be able to connect to a server running the mod.

Do not add client-required content or networking.

If loader metadata needs to declare the mod as server-side-compatible or optional on clients, configure it appropriately using the same conventions as Quicksort.

A development client module may still be present where required by the build template, but the mod itself must not require installation on connecting clients.

## Metadata and cleanup

Ensure that all copied or adapted files are fully converted to the new project.

Check for and remove:

* Quicksort package names
* Quicksort mod IDs
* Quicksort display names
* Quicksort descriptions
* Quicksort repository links
* Quicksort artifact names
* Quicksort configuration names
* copied gameplay functionality
* stale mixin configuration
* stale access wideners or access transformers
* unused dependencies
* unused resources
* comments that describe Quicksort rather than this project

Use an appropriate placeholder description for Forever World Portals, but do not claim that portal functionality is already implemented.

## Build and verification

Produce a project that compiles for both Fabric and NeoForge.

Run the relevant commands from the adapted `Justfile`, as well as the underlying Gradle tasks where useful.

At minimum verify:

* clean build succeeds
* common tests succeed, if the template has tests
* Fabric build succeeds
* NeoForge build succeeds
* Fabric development server launches successfully
* NeoForge development server launches successfully
* configuration is created or loaded correctly
* startup logs confirm singleton initialization
* vanilla clients are not declared as requiring the mod

Do not leave placeholder API calls that fail compilation.

Fix build, resource-processing, metadata, and launch errors rather than merely documenting them.

## Documentation

Create a concise README covering:

* project name and current placeholder status
* mod ID `fwportals`
* Java package `net.pcal.fwportals`
* supported Minecraft version
* supported loaders
* Java version
* how to build
* how to launch Fabric and NeoForge development servers
* useful `just` recipes
* location and purpose of `fwportals.properties`
* server-side-only intention
* the fact that no portal mechanics are implemented in Pass 0

## Final report

At the end, provide:

1. A concise description of the resulting project architecture.
2. A list of files added or materially adapted.
3. A description of which Quicksort patterns were retained.
4. A description of how the `Justfile` was adapted.
5. Build, test, and development-server launch results.
6. Any differences that were necessary between Fabric and NeoForge.
7. Any unresolved build or loader limitations.

Keep this pass narrowly focused on establishing a clean foundation. Do not begin implementing portal behavior.
