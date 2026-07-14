# Forever World Portals

Forever World Portals adds player-built portals that permanently connect your 
world to distant regions of the Overworld. This makes it easy to explore new 
biomes or to return to the early-game experience without starting a new world 
or walking thousands of blocks.

Over time, your world becomes a collection of settlements connected by a 
growing network of permanent portals, making it easy to revisit old homes, 
discover newly generated content, and continue the same adventure for years 
instead of constantly starting over.

## Features

* Teleport to distant lands in ungenerated chunks.
* Attune portals to specific biomes by offering items.
* Highly configurable.
* Vanilla-friendly, works with vanilla clients.
* Supports both Fabric and NeoForge.

## Usage

### Building a Portal

Build a normal Nether portal frame, but use **diamond blocks** instead of obsidian.

Light the portal with Flint and Steel.

The first time a player enters the portal, Forever World Portals searches for a suitable destination and permanently links the portal to it. Future trips through that portal always return to the same location.

### Portal Attunements

Before a newly created portal is used for the first time, you may throw certain items into it to influence where it will lead.

The default configuration includes attunements for several biome types:

* Sunflower → Sunflower Plains
* Allium → Flower Forest
* Pale Oak Sapling → Pale Garden

If no offering is made, the portal uses the server's default destination.

Attunements can be customized using Minecraft data packs.

### Return Travel

By default, a ruined portal will generate at your destination. In order to get back home, you'll have to rebuild it using diamond blocks.

But you can also change the configuration to generate a fully-functional return portal or no portal whatsoever.

## Configuration

The first time the mod runs, it creates:

```text
config/fwportals.properties
```

Most servers can use the default configuration without modification.

Configuration options allow server operators to control:

* portal activation requirements
* whether players must have empty inventories
* destination portal generation
* destination search behavior
* logging
* optional client portal color

## Optional Client Module

Forever World Portals works with completely vanilla clients.  But if you
do have the mod installed on the client (or are playing single-player),
you will get some enhanced effects on the portals.

## Disclaimers

* Forever World Portals is published under the MIT License.

* You're free to include this mod in modpacks provided appropriate attribution 
is given and that modpack consumers download it from modrinth or curseforge
(e.g., in an mrpack).

* A significant portion of this mod was developed using AI coding tools.

## Questions?

If you have questions, suggestions, or bug reports, please join the Discord server:

https://discord.pcal.net
