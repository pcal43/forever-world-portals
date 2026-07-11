# Forever World Portals

Forever World Portals is currently in Pass 2. Diamond-block portal frames can be detected, activated with flint and steel, filled with vanilla Nether portal blocks, recognized when entities enter them, and used for first-entry frontier reservation plus long-distance teleportation. Return-portal generation and inventory stripping are intentionally not implemented yet.

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

## First-Entry Behavior

The first time a player enters a valid diamond-frame portal:

- the frame is normalized and looked up in persistent portal registry data
- if no registry record exists yet, the mod chooses a distant same-dimension destination
- the destination is validated for safe landing
- an origin portal record and a reserved destination record are stored persistently
- the player is teleported to the reserved frontier location

Later entries through the same origin portal reuse the same reserved destination instead of rolling a new one.

## Configuration

The mod writes and loads `config/fwportals.properties`.

- Purpose: configure Forever World portal activation, logging, and first-entry destination selection
- Current settings: `enabled`, `logLevel`, `frameBlock`, `activationItem`, `minimumPortalSeparationBlocks`, and `destinationSearchAttempts`
- Behavior: defaults are documented and written automatically if the file does not exist

## Status

- The mod is intended to remain server-side-only so vanilla clients can connect.
- Diamond-block Forever World portal activation, recognition, persistent linkage, and first-entry teleportation are implemented.
- Ordinary obsidian Nether portals are left to vanilla behavior.
- Entering a new Forever World portal permanently reserves a frontier destination in world saved data.
- Teleportation currently sends players to a distant safe landing point in the same dimension as the origin portal.
- Return portals, inventory stripping, commands, custom content, and other later gameplay systems are not implemented yet.

## Known Limitations

- Only players are teleported in Pass 2. Non-player entities are recognized but not transported.
- Reserved destinations are created on first entry, but no physical return portal is generated there yet.
- Inventory stripping, paired return travel, and portal persistence beyond the registry records are deferred to later passes.
- In-world portal use was not exercised through an automated game test in this pass; verification focused on build/test coverage plus Fabric and NeoForge dedicated-server startup.
