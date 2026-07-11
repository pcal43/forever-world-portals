# Forever World Portals

Forever World Portals is currently in Pass 1. Diamond-block portal frames can now be detected, activated with flint and steel, filled with vanilla Nether portal blocks, and recognized again when entities enter them. Teleportation and destination selection are intentionally not implemented yet.

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

## Configuration

The mod writes and loads `config/fwportals.properties`.

- Purpose: configure Forever World portal activation and logging
- Current settings: `enabled`, `logLevel`, `frameBlock`, and `activationItem`
- Behavior: defaults are documented and written automatically if the file does not exist

## Status

- The mod is intended to remain server-side-only so vanilla clients can connect.
- Diamond-block Forever World portal activation and recognition are implemented.
- Ordinary obsidian Nether portals are left to vanilla behavior.
- Entering a Forever World portal logs the event for future travel hooks.
- Teleportation, destination selection, persistence, commands, custom content, and other gameplay systems are not implemented yet.

## Known Limitations

- Forever World portals deliberately do not teleport entities in Pass 1.
- There is no persistence or registry for portal frames yet.
- In-world activation was not automated in headless tests; runtime verification in this pass focused on build/test coverage plus loader startup with the new mixin wiring.
