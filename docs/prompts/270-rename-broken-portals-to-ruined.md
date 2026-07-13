Rename the `BROKEN` portal state to `RUINED` throughout the project.

Requirements:

- Rename the enum constant, identifiers, methods, variables, and any other symbols from `BROKEN` to `RUINED`.
- Update all comments, Javadocs, log messages, configuration keys, documentation, and user-facing text to consistently use "ruined" instead of "broken".
- Rename any serialized names, codecs, registry IDs, NBT values, configuration values, and other persisted strings from `"broken"` to `"ruined"`. Do not preserve backward compatibility; this is an unreleased mod and breaking compatibility is acceptable.
- Preserve all existing behavior; this is a terminology refactor only.
- Update any tests affected by the rename.
- Remove any dead code that becomes unnecessary as a result of the refactor.

The goal is for "ruined portal" to be the only terminology used throughout the codebase and documentation.
