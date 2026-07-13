# Pass 260: Restore Generated Terrain Distance Test Factory

Status: Completed
Date: 2026-07-13

## Prompt

The build for GeneratedTerrainDistanceConstraintTest.java is failing because of a missing GeneratedTerrainDistanceConstraint.ofRegions method.  It must have been deleted sometime in our recent reworks but I'm not sure what happened to it.  Could you examin it and figureout the right fix?

## Outcome

- Restored `GeneratedTerrainDistanceConstraint.ofRegions(...)` as the direct-construction path for deterministic tests and internal callers in the `common.portal` package.
- Kept `snapshot(...)` as the runtime filesystem-based construction path for scanning generated region files.
- Verified that the missing method was not removed from an older implementation; the class was introduced during the recent cleanup pass without the direct factory, while existing tests already depended on it.
- Ran `./gradlew --no-daemon :common:test --tests net.pcal.fwportals.common.portal.GeneratedTerrainDistanceConstraintTest --tests net.pcal.fwportals.common.portal.PortalDestinationSelectorTest --console=plain`.
- The targeted common tests passed successfully.
- No broader API or behavior changes were made to generated-terrain distance checking.
