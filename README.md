# Forever World Portals

Forever World Portals is currently in Pass 4. Diamond-block portal frames can be detected, activated with flint and steel, filled with vanilla Nether portal blocks, recognized when entities enter them, and used to register persistent portal routes. Each route maps one canonical portal anchor to another canonical portal anchor. Generated return portals are implemented by default. Inventory stripping is still intentionally not implemented.

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
- if no registered source portal anchor is enclosed by that physical portal, the mod chooses distant candidate destination anchors
- the mod attempts to generate a destination portal whose canonical anchor is exactly the requested candidate block
- after generation succeeds, two independent source-portal routes are stored persistently:
  - original source anchor -> generated destination anchor
  - generated destination anchor -> original source anchor
- the player is teleported to `Vec3.atBottomCenterOf(destinationAnchor)`

Later entries through any physical Forever World portal that still encloses the same stored source anchor reuse the same exact destination anchor without rerolling anything.

## Portal Anchor Identity

The registry stores anchor-to-anchor routes, not linked portal-pair objects and not separate landing coordinates.

- Every physical Forever World portal has one canonical `anchorBlock`
- The anchor is inside the portal interior, in the lowest interior row, as close as possible to the horizontal center
- Even-width portals break ties toward the more-negative world coordinate along the portal axis
- A rebuilt frame keeps the same identity if it still encloses that stored source anchor
- Moving the frame so it no longer encloses that anchor creates a new source portal
- Teleportation always derives arrival from the stored destination anchor and does not use a separate persisted landing position

## Configuration

The mod writes and loads `config/fwportals.properties`.

- Purpose: configure Forever World portal activation, logging, destination selection, and return-portal behavior
- Current settings: `enabled`, `logLevel`, `frameBlock`, `activationItem`, `returnPortalMode`, `minimumPortalSeparationBlocks`, and `destinationSearchAttempts`
- Behavior: defaults are documented and written automatically if the file does not exist

`returnPortalMode` currently supports these values:

- `GENERATE`: fully implemented; automatically generates and links the destination-side return portal
- `REQUIRE_PLAYER_BUILD`: parsed but not implemented yet
- `NONE`: parsed but not implemented yet

## Status

- The mod is intended to remain server-side-only so vanilla clients can connect.
- Diamond-block Forever World portal activation, anchor-based route identity, first-entry teleportation, and generated return portals are implemented.
- Ordinary obsidian Nether portals are left to vanilla behavior.
- Entering a new Forever World portal permanently creates an anchor-to-anchor route in world saved data.
- Generated return travel works by creating a second independent source portal route from the generated destination anchor back to the original source anchor.
- Teleportation currently stays within the same dimension as the origin portal.
- Inventory stripping, player-built return-portal matching, commands, custom content, and other later gameplay systems are not implemented yet.

## Known Limitations

- Only players are teleported in Pass 4. Non-player entities are recognized but not transported.
- `REQUIRE_PLAYER_BUILD` and `NONE` are intentionally not implemented yet and fail clearly if selected.
- Inventory stripping and player-built portal matching are deferred to later passes.
- This pass keeps the current development-only assumption that older experimental registry data will be discarded rather than migrated.
- In-world portal founding and restart persistence were not exercised through automated game tests in this pass; verification focused on build/test coverage plus Fabric and NeoForge dedicated-server startup.
