# Forever World Portals

Forever World Portals is currently in Pass 3. Diamond-block portal frames can be detected, activated with flint and steel, filled with vanilla Nether portal blocks, recognized when entities enter them, and used to found a persistent bidirectional portal pair. Generated return portals are implemented by default. Inventory stripping is still intentionally not implemented.

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

- the frame is normalized and looked up in persistent portal registry data
- if no registry record exists yet, the mod chooses a distant same-dimension destination
- the destination is validated for safe landing and nearby portal placement
- a diamond-framed return portal is generated near the reserved destination
- an origin portal record and a linked destination portal record are stored persistently
- the player is teleported to the generated partner portal

Later entries through either linked portal reuse the stored partner instead of selecting a new destination.

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
- Diamond-block Forever World portal activation, recognition, persistent linkage, first-entry teleportation, and generated return portals are implemented.
- Ordinary obsidian Nether portals are left to vanilla behavior.
- Entering a new Forever World portal permanently creates a linked source/destination pair in world saved data.
- The generated partner portal uses the same diamond-frame Forever World portal rules and can be used for return travel.
- Teleportation currently stays within the same dimension as the origin portal.
- Inventory stripping, player-built return-portal matching, commands, custom content, and other later gameplay systems are not implemented yet.

## Known Limitations

- Only players are teleported in Pass 3. Non-player entities are recognized but not transported.
- `REQUIRE_PLAYER_BUILD` and `NONE` are intentionally not implemented yet and fail clearly if selected.
- Inventory stripping and player-built portal matching are deferred to later passes.
- In-world portal founding and restart persistence were not exercised through automated game tests in this pass; verification focused on build/test coverage plus Fabric and NeoForge dedicated-server startup.
