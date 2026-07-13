# Forever World Portals

Forever World Portals currently supports diamond-frame portal activation, anchor-based persistent routes, generated return portals, and reloadable data-driven attunement definitions. Inventory stripping is still intentionally not implemented.

- Mod ID: `fwportals`
- Java package: `net.pcal.fwportals`
- Supported Minecraft version: `26.2`
- Supported loaders: Fabric and NeoForge
- Java version: `25`
- Intended deployment: server-side only

## Build

```sh
./gradlew clean build
```

## Development Launch

```sh
just run-fabric-server
just run-neoforge-server
```

Client launch recipes are also present to match the Quicksort template:

```sh
just run-fabric
just run-neoforge
```

## Useful `just` Recipes

- `just clean`
- `just compile`
- `just compile-common`
- `just test`
- `just jar`
- `just run-fabric-server`
- `just run-neoforge-server`

## Diamond Portal Construction

Build a standard vertical Nether-portal rectangle, but use `minecraft:diamond_block` for the full frame instead of obsidian.

- Supported geometry matches vanilla Nether portals
- The interior must be empty before activation
- Light the interior with `minecraft:flint_and_steel`
- The portal uses vanilla Nether portal blocks, sounds, particles, and visuals after activation
- Lighting a portal alone does not generate a destination

## First-Entry Behavior

The first time a player enters a valid diamond-frame portal:

- the complete portal is detected and assigned one canonical anchor block in the lowest interior row
- if no registered portal anchor is enclosed by that physical portal, the mod walks a deterministic square spiral of distant search anchors
- entering an unresolved portal displays a server-driven action-bar destination-search message before the search begins
- at each eligible spiral anchor, the mod uses Minecraft's built-in worldgen biome search for the current default biome target
- the mod attempts to generate a destination portal whose canonical anchor is exactly the requested candidate block
- after generation succeeds, two independent portal routes are stored persistently:
  - original portal anchor -> generated destination anchor
  - generated destination anchor -> original portal anchor
- the player is teleported to `Vec3.atBottomCenterOf(destinationAnchor)`

Later entries through any physical Forever World portal that still encloses the same stored portal anchor reuse the same exact destination anchor without rerolling anything.

## Attunements

Attunements are loaded from server data packs at:

```text
data/<namespace>/forever_world_portals/attunements.json
```

The built-in file lives at:

```text
common/src/main/resources/data/forever_world_portals/forever_world_portals/attunements.json
```

Definitions are merged by logical attunement ID in pack-priority order. A higher-priority definition replaces the entire lower-priority definition with the same logical ID. Definitions with other IDs remain in place.

The built-in file includes a required reserved `default` definition. After merge, there must be exactly one effective `default` entry, it must not specify an `item`, and all non-`default` entries must specify one.

Current built-in mappings:

- `default` -> `minecraft:cherry_grove`
- `minecraft:sunflower` -> `minecraft:sunflower_plains`
- `minecraft:allium` -> `minecraft:flower_forest`
- `minecraft:pale_oak_sapling` -> `minecraft:pale_garden`

Current scope:

- item-to-target mappings plus color and optional vanilla particle metadata are loaded and validated from data packs
- biome targets only
- `minecraft:overworld` only
- attunement is applied by throwing one recognized item into an unresolved portal before first use
- the most recently accepted offering replaces any previous portal attunement
- accepted offerings are consumed immediately, trigger a brief multi-tick portal-wide magical particle surge plus a confirmation sound, and send an action-bar message to the throwing player when identified
- unrecognized items are ignored without feedback
- established portals cannot be re-attuned
- portal founding uses the stored portal attunement when present and otherwise falls back to the effective data-driven `default` target

## Portal Anchor Identity

The registry stores anchor-to-anchor routes, not linked portal-pair objects and not separate landing coordinates.

- Every physical Forever World portal has one canonical `anchorBlock`
- The anchor is inside the portal interior, in the lowest interior row, as close as possible to the horizontal center
- Even-width portals break ties toward the more-negative world coordinate along the portal axis
- A rebuilt frame keeps the same identity if it still encloses that stored portal anchor
- Moving the frame so it no longer encloses that anchor creates a new portal
- Teleportation always derives arrival from the stored destination anchor and does not use a separate persisted landing position
- By default, players must carry absolutely nothing to enter a Forever World portal; this can be disabled with `requireEmptyInventory=false`

## Configuration

The mod writes and loads `config/fwportals.properties`.

- Purpose: configure Forever World portal activation, logging, destination selection, and return-portal behavior
- Current settings: `requireEmptyInventory`, `logLevel`, `portalFrameBlock`, `returnPortalMode`, `spiralSearchSpacing`, `maxSpiralSearchPositions`, `maxBiomeSearches`, `maxPortalPlacementAttemptsPerBiome`, `minGeneratedTerrainDistanceBlocks`, and `client.portalColor`
- Initial creation: if `fwportals.properties` does not exist, the mod copies the bundled `fwportals-default.properties` template verbatim
- Defaults on startup: the bundled `fwportals-default.properties` resource is always loaded as the default-value source, and any keys present in the user file override it
- Partial user configs: omitted keys continue using the defaults bundled with the installed mod version
- Existing user files are never automatically overwritten or rewritten just to add newly introduced settings
- `requireEmptyInventory=true` prevents portal activation unless the player's main inventory, hotbar, armor slots, and offhand are all empty
- `spiralSearchSpacing` controls square-spiral search-center spacing
- The biome-search radius is derived from spiral spacing as `ceil(spacing / sqrt(2))`
- `maxSpiralSearchPositions` limits cheap spiral-center inspection across one destination search
- `maxBiomeSearches` independently limits expensive biome-locator calls across one destination search
- `maxPortalPlacementAttemptsPerBiome` resets for each eligible biome result and limits concrete generated-layout evaluations
- `minGeneratedTerrainDistanceBlocks` remains the independent minimum distance from pre-existing generated terrain
- `returnPortalMode` accepts `NONE`, `RUINED`, or `COMPLETE` and defaults to `RUINED`
- `client.portalColor` sets the fixed RGB tint used by the optional client module for valid Forever World portals and defaults to `#4CAF50`

`returnPortalMode` currently supports these values:

- `NONE`: no destination portal is generated and no reverse portal is registered
- `RUINED`: a cobbled-deepslate placeholder frame plus buried crying-obsidian footing is generated and registered
- `COMPLETE`: a complete diamond Forever World portal plus buried crying-obsidian footing is generated and registered

## Status

- The mod is intended to remain server-side-only so vanilla clients can connect.
- Clients with the optional mod installed render valid Forever World portals with a fixed configurable green tint while ordinary Nether portals remain vanilla purple.
- Clients with the optional mod installed also suppress the vanilla portal screen-warp effect while the local player is standing in a valid Forever World portal and carrying items.
- Diamond-block Forever World portal activation, anchor-based route identity, first-entry teleportation, generated return portals, thrown-item portal attunement, and data-pack-driven attunement loading are implemented.
- Ordinary obsidian Nether portals are left to vanilla behavior.
- Entering a new Forever World portal permanently creates an anchor-to-anchor route in world saved data.
- Generated return travel works by creating a second independent source portal route from the generated destination anchor back to the original source anchor.
- Teleportation currently supports only `minecraft:overworld` destination targets.
- Destination selection uses a stored thrown-item portal attunement when present and otherwise uses the effective data-pack `default` target for new portals.
- Inventory stripping, player-built return-portal matching, commands, custom content, and other later gameplay systems are not implemented yet.

## Known Limitations

- Only players are teleported in Pass 4. Non-player entities are recognized but not transported.
- `REQUIRE_PLAYER_BUILD` and `NONE` are intentionally not implemented yet and fail clearly if selected.
- Attunement reload fails fast on invalid item IDs, biome IDs, duplicate effective input items, empty biome lists, or unsupported destination dimensions.
- Inventory stripping and player-built portal matching are deferred to later passes.
- This pass keeps the current development-only assumption that older experimental registry data will be discarded rather than migrated.
- In-world portal founding and restart persistence were not exercised through automated game tests in this pass; verification focused on build/test coverage plus Fabric and NeoForge dedicated-server startup.
