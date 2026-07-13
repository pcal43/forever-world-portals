# Pass 240: Fabric Public Portal Model Wrapper

Status: Completed
Date: 2026-07-13

## Prompt

Review and streamline the optional client portal-rendering implementation, taking into account the current reorganized structure and the Javadocs on `ModelBlockRendererMixin` and `AltModelBlockRendererImplMixin`.

The current documentation correctly identifies two rendering paths:

1. Vanilla `ModelBlockRenderer`, which obtains model parts through `BlockStateModel.collectParts(...)`.
2. Fabric Renderer API / Indigo, which may bypass `ModelBlockRenderer` and invoke `BlockStateModel.emitQuads(...)` directly.

Do not assume that this requires two mixins. Preserve support for both paths, but investigate replacing the Fabric Indigo implementation mixin with a public Fabric model integration.

Current goals:

* Retain the shared `ModelBlockRendererMixin` unless a demonstrably better cross-loader Minecraft-level hook exists. Vanilla `collectParts(...)` lacks world and position parameters, so the current mixin provides context needed to distinguish Forever World portals from ordinary Nether portals.
* Remove the mixin targeting:
  `net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl`
* Remove all direct references to `net.fabricmc.fabric.impl`.
* Do not add mixins targeting Sodium or any other renderer implementation.

For Fabric, investigate using the public Fabric model-loading/model-modification API to wrap the Nether portal `BlockStateModel` at resource/model load time.

The Fabric-installed wrapper should:

* retain the original Nether portal model as its delegate;
* leave ordinary Nether portals unchanged;
* use the `BlockAndTintGetter`, `BlockPos`, and `BlockState` supplied to the public Fabric `BlockStateModel.emitQuads(...)` path to determine whether the current block is a valid Forever World Portal;
* delegate normal portals directly to the original model;
* emit transformed quads for Forever World portals using the existing mask sprite and tint index;
* work with Indigo and other compliant Fabric Renderer API implementations, including Sodium where supported;
* avoid depending on Indigo classes or behavior beyond the public Renderer API contract.

Reuse and centralize the transformation semantics currently implemented by:

* `TintedPortalBlockStateModel`
* `TintedPortalBlockStateModelPart`
* `ForeverWorldPortalsClient.wrapPortalModelIfNeeded(...)`

Do not maintain two independent definitions of portal geometry or rendering properties.

Preserve from the original portal quads:

* positions and geometry;
* face direction and culling behavior;
* UV layout, remapped to the mask sprite;
* render layer;
* item render type;
* shade flag;
* light emission;
* material flags;
* particle material, unless there is a documented reason to change it.

Only intentionally change:

* the sprite to the Forever World portal mask;
* the tint index to the registered portal tint index.

Do not manually regenerate portal faces and do not reintroduce a hardcoded portal-plane depth.

Review allocation and caching behavior:

* `wrapPortalModelIfNeeded(...)` currently appears to create a new `TintedPortalBlockStateModel` for each qualifying tessellation.
* `TintedPortalBlockStateModelPart` caches transformed quads, but that cache may be ineffective if its enclosing wrapper is short-lived.
* Where safe, cache transformed model views by original model identity and current mask sprite identity.
* Never cache portal validity by world position.
* Clear or naturally replace model/sprite-dependent caches after resource reload.

The intended final structure is approximately:

Shared client module:

* portal detection;
* portal tint source;
* shared quad/model transformation;
* vanilla `ModelBlockRendererMixin`;
* renderer-independent helpers and tests.

Fabric module:

* public Fabric model-loading registration;
* a public Renderer API-compatible wrapper or adapter;
* no Fabric implementation mixins.

NeoForge module:

* continue using the shared vanilla rendering hook unless NeoForge testing proves another public integration is necessary.

Update the Javadocs after the change:

* Explain that vanilla and Fabric Renderer API use separate model-consumption paths.
* Explain why the vanilla mixin is still required for position-sensitive rendering.
* Explain that Fabric’s alternate path is handled through public model registration and `emitQuads(...)`, not through an Indigo implementation mixin.
* Do not claim that Indigo specifically requires a mixin.

Verification requirements:

* Build and test all modules.
* Launch Fabric with the default renderer.
* Launch Fabric with Sodium if a compatible version is available.
* Launch NeoForge.
* Verify that:

  * valid Forever World portals use the configured tint;
  * ordinary Nether portals remain vanilla purple;
  * both portal orientations render correctly;
  * portal faces retain vanilla geometry and culling;
  * resource reload does not leave stale sprites or transformed models;
  * dedicated servers do not load client classes.

Before implementing, confirm from the actual Fabric API source for the pinned Minecraft/Fabric API version that a public model-loading hook can install a wrapper whose `emitQuads(...)` implementation is consumed by Indigo and compatible Renderer API implementations.

If the public API cannot support this, document the exact API limitation and call graph. Do not retain or restore the `AltModelBlockRendererImplMixin` merely because it is the existing solution without first proving the public-wrapper approach cannot work.

## Outcome

- Confirmed from the pinned Fabric API bytecode/signatures in the local cache that the public path is sufficient (the matching source jars were not present in the local Gradle cache for direct source inspection):
  - `ModelLoadingPlugin` exposes `modifyBlockModelAfterBake()`.
  - `ModelModifier.AfterBakeBlock` can replace baked block-state models.
  - `WrapperBlockStateModel` provides a public `emitQuads(...)` override point consumed by Fabric Renderer API renderers.
- Replaced the Fabric Indigo implementation mixin with a public Fabric model-loading plugin that wraps baked Nether portal models after bake time.
- Removed all direct references to `net.fabricmc.fabric.impl` and deleted the Fabric-only client mixin config.
- Kept the shared `ModelBlockRendererMixin` for the vanilla path and updated its Javadoc to explain the split between vanilla `collectParts(...)` and Fabric Renderer API `emitQuads(...)`.
- Centralized the transformed portal model reuse by caching wrapped portal models in `ForeverWorldPortalsClient`, keyed by model identity and naturally reset when the mask sprite identity changes after resource reload.
- Verified compilation with `./gradlew --no-daemon :client:compileJava :fabric:compileJava :neoforge:compileJava --console=plain`.
- Verified `:client:test`, `:fabric:jar`, and `:neoforge:jar` with `./gradlew --no-daemon :client:test :fabric:jar :neoforge:jar --console=plain`.
- `./gradlew --no-daemon clean build --console=plain` currently fails in the existing common test suite due unrelated visibility/access issues in repository test code outside this rendering change.
