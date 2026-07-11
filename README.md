# Forever World Portals

Forever World Portals is currently in Pass 3. Diamond-block portal frames can be detected, activated with flint and steel, filled with vanilla Nether portal blocks, recognized when entities enter them, and used to register persistent source portals. Each source portal stores one hidden origin block plus one exact destination coordinate. Generated return portals are implemented by default. Inventory stripping is still intentionally not implemented.

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

- the complete portal is detected and assigned one hidden origin block near the visual center of its interior
- if no registered source portal is enclosed by that physical portal, the mod chooses a distant same-dimension destination
- the destination is validated for safe landing and nearby portal placement
- a diamond-framed return portal is generated near the reserved destination
- two independent source portal records are stored persistently when return generation succeeds
- the player is teleported to the generated return area

Later entries through any physical Forever World portal that still encloses the same stored origin block reuse the same exact destination without selecting a new one.

## Source Portal Identity

The registry stores source portals, not linked portal pairs.

- Each source portal has one hidden `originBlock` chosen from the portal interior near the visual center
- A rebuilt frame keeps the same identity if it still encloses that stored origin block
- Moving the frame away so it no longer encloses that block creates a new source portal
- Multiple physical portals may intentionally enclose the same origin block and therefore share one destination
- Teleportation uses the stored destination coordinate directly and does not require a destination portal

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
- Diamond-block Forever World portal activation, source-portal identity, first-entry teleportation, and generated return portals are implemented.
- Ordinary obsidian Nether portals are left to vanilla behavior.
- Entering a new Forever World portal permanently creates a source portal record with one exact destination in world saved data.
- Generated return travel works by creating a second independent source portal near the generated destination portal.
- Teleportation currently stays within the same dimension as the origin portal.
- Inventory stripping, player-built return-portal matching, commands, custom content, and other later gameplay systems are not implemented yet.

## Known Limitations

- Only players are teleported in Pass 3. Non-player entities are recognized but not transported.
- `REQUIRE_PLAYER_BUILD` and `NONE` are intentionally not implemented yet and fail clearly if selected.
- Inventory stripping and player-built portal matching are deferred to later passes.
- This pass keeps the current development-only assumption that older experimental registry data will be discarded rather than migrated.
- In-world portal founding and restart persistence were not exercised through automated game tests in this pass; verification focused on build/test coverage plus Fabric and NeoForge dedicated-server startup.
