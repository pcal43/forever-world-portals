# Pass 60: Biome-Targeted Destination Search

Status: Completed
Date: 2026-07-11

## Prompt

Implement biome-targeted destination search on top of the existing spiral search infrastructure.

As with previous work, before making any code changes, create a new prompt file under `doc/prompts` containing this prompt. Follow the existing naming convention used by the project so the development history remains documented.

The spiral iterator is now responsible for producing candidate search-anchor coordinates. Do not change the spiral implementation.

Instead, update the destination search so that each eligible spiral anchor becomes the origin for Minecraft's built-in world-generation biome search.

For each spiral anchor:

1. Use Minecraft's world-generation biome search API (`BiomeSource.findBiomeHorizontal(...)` or the appropriate equivalent for Minecraft 26.2) to locate the nearest biome matching a predicate.
2. The biome search must use world-generation data only and must not generate chunks.
3. If no matching biome is found within the configured search radius, continue to the next spiral anchor.
4. If a matching biome is found, verify that the returned coordinate is at least the configured minimum distance `d` from every region file that existed when the destination search began.
5. If it is too close to any region in that frozen snapshot, reject it and continue with the next spiral anchor.
6. Otherwise, use the returned biome coordinate as the input to the existing landing-site and destination-portal placement logic.
7. Preserve the existing final validation that the actual destination portal anchor is still at least `d` blocks from every region in the frozen snapshot before generating the portal.

For this first implementation, hardcode the biome predicate so that it matches any of these biomes:

* `minecraft:sunflower_plains`
* `minecraft:flower_forest`
* `minecraft:pale_garden`

Use the predicate type expected by Minecraft's biome search API. Do not implement a custom biome-sampling search using `getNoiseBiome()`. The goal is to use Minecraft's built-in nearest-biome search.

Keep the biome predicate isolated so it can easily be replaced later.

Do not change the portal registry format, spiral iterator, frozen region snapshot behavior, or landing-site generation logic except where necessary to use the biome search result as the search origin.

Please inspect the actual Minecraft 26.2 sources available in this project and use the correct biome-search API for that version rather than relying on examples from older mappings.

When complete:

* Add or update tests covering the biome-targeted destination search.
* Ensure the project builds successfully on both Fabric and NeoForge.
* Update any developer documentation that is affected by this change.
* Include a brief summary of the implementation, any assumptions made about the Minecraft 26.2 biome-search API, and any limitations discovered during implementation.

## Outcome

- Added a built-in biome-targeted search step on top of the existing spiral anchor iterator.
- Used Minecraft 26.2 `ServerLevel.findClosestBiome3d(...)`, which delegates to `BiomeSource.findClosestBiome3d(...)` with worldgen sampler data and does not require chunk generation for the biome lookup itself.
- Reused `minimumGeneratedTerrainDistanceBlocks` as the biome-search radius because this prompt did not introduce a separate configurable biome radius.
- Kept the frozen region-file snapshot constraint in place for both transient spiral anchors and biome-search results, and preserved the final validation of the generated portal anchor before route registration.
- Updated tests for spiral-plus-biome selection behavior and kept the implementation loader-neutral in `common`.
- Verified with `./gradlew :common:compileJava`, `./gradlew :common:test`, and `./gradlew :fabric:build :neoforge:build`.
