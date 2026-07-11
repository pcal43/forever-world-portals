# Forever World Portals

Forever World Portals is currently a Pass 0 placeholder scaffold. This repository establishes the multi-loader build, shared singleton service, typed configuration loading, and server lifecycle wiring, but it does not implement any portal mechanics yet.

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

## Configuration

The mod writes and loads `config/fwportals.properties`.

- Purpose: placeholder typed configuration for the shared service
- Current settings: `enabled` and `logLevel`
- Behavior: defaults are documented and written automatically if the file does not exist

## Status

- The mod is intended to remain server-side-only so vanilla clients can connect.
- Pass 0 does not implement portal detection, activation, teleportation, persistence, commands, blocks, items, or any other gameplay features.
