# Player Message Translation Keys With Fallbacks

Status: Completed
Date: 2026-07-12

## Prompt

Actually, we're going to take a slightly different approach. I rolled back your change. Instead, pleae do this: Please refactor the player-facing text in Forever World Portals to use Minecraft translation keys with English fallbacks.

Before making changes:

- Inspect the existing code.
- Preserve the current architecture.
- Save a copy of this prompt in `doc/prompts` using the existing numbering convention.

# Goal

Remove hardcoded player-visible strings from the Java code while keeping the mod fully server-side and loader-independent.

Do **not** add any localization library or translation framework.

Do **not** implement server-side translation.

The goal is simply to establish stable translation keys now so that proper localization can be added later.

# Requirements

Whenever the mod currently creates text using:

```java
Component.literal(...)
```

## Outcome

- Replaced player-facing literal text construction with `Component.translatableWithFallback(...)` so message keys are stable while vanilla clients still receive English fallback text.
- Kept the refactor in the common feedback/message layer and existing call sites; no loader-specific logic or translation framework was added.
- Added `common/src/main/resources/assets/fwportals/lang/en_us.json` so client installs of the mod can also resolve the same keys normally.
- Ran `./gradlew --no-daemon :common:test --console=plain` and `./gradlew --no-daemon :fabric:build :neoforge:build --console=plain`.
- No manual in-game verification was run in this pass.
