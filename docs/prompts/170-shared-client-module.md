# Shared Client Module

Status: Completed
Date: 2026-07-12

## Prompt

Please add a new shared client-only Gradle module to the Forever World Portals project.

The purpose of this change is purely architectural. Do not implement any actual client-side portal enhancements yet. Create only the project structure, initialization paths, and placeholder classes needed for future client-specific features.

## Goals

The mod must continue to work with vanilla clients.

The optional client-side code must never be loaded or referenced on a dedicated server.

The intended module structure is:

```text
common
client
fabric
neoforge
```

Dependency direction should be:

```text
client   -> common
fabric   -> common + client
neoforge -> common + client
```

There must be no dependency from `common` to `client`.

## General requirements

1. Add a new Gradle module named `client`.
2. Configure it consistently with the existing project conventions.
3. Use Java 25 everywhere, matching the rest of the Minecraft 26.2 project.
4. The `client` module should contain loader-neutral client-only code.
5. Do not publish `client` as a separate user-facing mod artifact.
6. Continue producing the existing Fabric and NeoForge mod artifacts, with the shared client module included as appropriate.
7. Do not add any real client-side behavior yet.
8. Clean up any obsolete build configuration made unnecessary by this change.
9. Preserve all existing server-side functionality.

## Client module

Create a client-only package, following the project’s existing package naming conventions, for example:

```text
net.pcal.fwportals.client
```

Add a singleton client service such as:

```java
public final class ForeverWorldPortalsClient {
    private static final ForeverWorldPortalsClient INSTANCE =
            new ForeverWorldPortalsClient();

    private ForeverWorldPortalsClient() {
    }

    public static ForeverWorldPortalsClient getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        // Placeholder for future loader-neutral client initialization.
    }
}
```

Use the project’s established naming style if there is already a better corresponding name.

The class should remain a placeholder. Do not register renderers, particles, HUD elements, packets, key bindings, or other client functionality in this pass.

## Strict classloading boundary

The dedicated server must never resolve a class from the new `client` module.

Enforce the following:

* Nothing in `common` may import or reference classes from `client`.
* Do not add a common-side `isClient()` branch that names a client class.
* Do not use reflection from common code to load the client service.
* Do not expose the client singleton through a common interface or common service locator.
* Only loader-specific client initialization paths may call the client singleton.

It is acceptable for client classes to be physically present in the final loader JAR, provided they are never loaded on a dedicated server.

## Fabric initialization

Ensure Fabric has a distinct client entry point implementing the appropriate Fabric client initializer interface.

That client entry point should call:

```java
ForeverWorldPortalsClient.getInstance().initialize();
```

The normal Fabric server/common initializer must not reference the client service.

Update `fabric.mod.json` or equivalent metadata so that the normal and client entry points are declared separately.

Use Fabric Loom’s client source-set support if that is already compatible with the project’s structure. Follow the conventions already used by this repository rather than introducing unnecessary complexity.

## NeoForge initialization

Add a NeoForge client-only initialization path.

Use an approach that guarantees the class is only loaded on the physical client, such as a client-only event subscriber or another NeoForge-supported client lifecycle mechanism.

The client-only NeoForge class should call:

```java
ForeverWorldPortalsClient.getInstance().initialize();
```

The main NeoForge mod entry point must not directly import or reference the shared client service.

Prefer a visibly client-restricted class rather than a common entry point containing a distribution check.

## Client mixins

Give the new `client` module its own mixin configuration for future client-only mixins, for example:

```text
fwportals.client.mixins.json
```

It should:

* use Java 25 compatibility;
* use a client-only mixin package;
* contain no actual mixins yet;
* keep client mixins separate from common mixins;
* be wired into both loader artifacts correctly.

Do not add placeholder mixin classes solely to make the file non-empty unless the tooling requires one.

Any future client mixins must go under the client-specific section of the mixin configuration, not the general common mixin list.

## Build configuration

Update:

* `settings.gradle` or `settings.gradle.kts`;
* root build configuration;
* module build files;
* dependency declarations;
* source-set configuration;
* resource processing;
* Fabric metadata;
* NeoForge metadata or mixin registration;
* IDE run configurations, if generated by the existing build.

Use the project’s existing Gradle DSL and conventions.

Avoid duplicating shared configuration if the root build already centralizes it.

Make sure Java toolchains, compilation targets, and mixin compatibility remain Java 25.

## Verification

Run the relevant build and tests.

At minimum, verify:

1. The full multi-loader build succeeds.
2. The Fabric artifact builds.
3. The NeoForge artifact builds.
4. Existing tests still pass.
5. Dedicated-server run configurations start without client-classloading errors, if those run configurations are available.
6. Client run configurations invoke the new placeholder initialization path.
7. `common` has no compile-time dependency on `client`.

Add a small unit or structural test only if there is an existing straightforward convention for doing so. Do not introduce a large new testing framework for this change.

## Scope restrictions

Do not implement any actual client-facing feature in this pass.

Specifically, do not add:

* portal rendering changes;
* colored portal textures;
* particles;
* anchor highlighting;
* debug overlays;
* key bindings;
* client commands;
* networking protocols;
* client capability negotiation;
* HUD elements;
* configuration UI.

This pass should establish only the safe project boundary and initialization scaffolding.

Before making changes, inspect the existing Gradle structure, loader entry points, mixin configuration, and the prompt history under `doc/prompts` so the implementation remains consistent with prior architectural decisions.

When finished, summarize:

* files added or changed;
* the resulting module dependency graph;
* how Fabric client initialization is isolated;
* how NeoForge client initialization is isolated;
* how dedicated-server client classloading is prevented;
* build and test commands run and their results.

## Outcome

- Added a new shared `client` module with a placeholder `ForeverWorldPortalsClient` singleton and a separate empty `fwportals.client.mixins.json`.
- Wired Fabric to use a distinct `ClientModInitializer`, and NeoForge to use a client-only `@EventBusSubscriber`, while keeping the normal server/common initializers free of client-service references.
- Updated the loader build scripts to include `client` sources and resources alongside `common`, without adding any dependency from `common` back to `client`.
- Verified common tests, both loader builds, and both dedicated-server run configurations.
- Did not run graphical client launches in this environment.
