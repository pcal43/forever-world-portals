# AGENTS.md

This document describes the development conventions for **Forever World Portals**. These instructions apply to all implementation work unless explicitly overridden by the user.

---

# General philosophy

Prefer a clean, maintainable architecture over the smallest possible implementation.

This project is expected to evolve over many implementation passes. Avoid solutions that solve only the current task if they make future development more difficult.

When multiple reasonable approaches exist:

* prefer readability over cleverness
* prefer vanilla Minecraft behavior over custom implementations
* prefer small focused classes over large procedural methods
* prefer shared/common code over loader-specific implementations
* avoid speculative abstractions that are not yet needed

Prefer removing code over adding code when both satisfy the requirements.

Do not perform broad refactoring unless requested or clearly necessary for the task at hand.

---

# Architecture

Gameplay behavior should be coordinated through a single central singleton service.

Loader entry points should remain thin and primarily perform:

* loader initialization
* event registration
* delegation into common code

Complex algorithms should live in focused helper classes owned or coordinated by the singleton.

Avoid introducing dependency injection frameworks.

Avoid unnecessary global state outside the singleton.

---

# Server-side-first design

Forever World Portals is intended to remain server-side-only whenever practical.

Prefer implementations that work with completely vanilla clients.

Do not introduce client-side code unless the user explicitly requests it.

Avoid custom blocks, items, rendering, networking, textures, models, sounds, or GUIs unless they become necessary.

---

# Vanilla-first implementation

Whenever Minecraft already provides logic that accomplishes a task, prefer adapting or reusing that implementation rather than creating a parallel implementation.

Examples include:

* portal validation
* portal lighting
* portal maintenance
* safe teleport positioning
* block placement
* persistence mechanisms

Inspect the mapped Minecraft implementation before creating custom algorithms.

---

# Incremental implementation

Implement functionality in small vertical slices.

Each implementation pass should:

* build successfully
* launch successfully
* leave the project in a working state

Avoid partially implemented systems spanning multiple passes.

Do not implement future roadmap items unless required by the current task.

---

# Documentation

Write documentation for its intended audience.

## README.md

`README.md` is the project's public documentation. It is intended for:

* players
* server administrators
* prospective users evaluating the mod

Keep it focused on:

* what the mod does
* features
* installation
* configuration
* compatibility
* usage
* user-visible behavior
* troubleshooting

Do **not** include implementation details unless they are directly relevant to users.

Examples of material that does **not** belong in `README.md`:

* architecture
* module organization
* class structure
* mixin implementation
* registry internals
* synchronization details
* platform-specific implementation
* design rationale
* historical implementation notes

## Internal documentation

Implementation documentation belongs under `docs/`.

Only create or update internal documentation when it captures information that will help future contributors understand or maintain the code.

Avoid documenting code that is already obvious from the implementation.

## Updating documentation

When completing an implementation pass:

* update `README.md` only if user-visible behavior has changed
* otherwise update the appropriate document under `docs/` when necessary
* avoid duplicating information across multiple documents
* prefer explaining **why** a design exists rather than restating what the code already does

---

# Implementation prompt archive

For substantial implementation passes, preserve the task prompt in the repository before modifying production code.

Store implementation prompts under:

`docs/prompts/`

Use sequential filenames with a three-digit pass number and a concise descriptive slug, for example:

* `000-project-scaffold.md`
* `010-diamond-portal-activation.md`
* `020-portal-persistence.md`

When the user provides a pass number or filename, use it. Otherwise, inspect the existing prompt files and select the next available number in increments of 10.

Each prompt file should use this format:

```markdown
# Pass N: Descriptive title

Status: In progress
Date: YYYY-MM-DD

## Prompt

[The complete task prompt, preserving its substantive wording and requirements.]
```

Rules:

1. Create the prompt file before making implementation changes.

2. Preserve the complete substantive task prompt. Do not replace it with a summary.

3. Minor formatting changes for readable Markdown are acceptable, but do not silently alter requirements.

4. Do not overwrite or revise the archived prompt after implementation has begun.

5. When follow-up instructions materially change the requested implementation, preserve them in a new prompt file or in a clearly dated `Amendments` section. Do not rewrite history.

6. Do not archive trivial questions, exploratory discussions, small corrections, or requests that do not result in a substantive code change.

7. Include the prompt file in the implementation diff.

8. At the end of the task, change `Status: In progress` to one of:

   * `Completed`
   * `Partially completed`
   * `Blocked`

9. Append a concise outcome section:

```markdown
## Outcome

- Brief summary of what was implemented.
- Important deviations or implementation decisions.
- Tests and builds run.
- Known limitations or deferred work.
```

10. Do not create Git commits unless the user explicitly requests them.
11. In the final task report, identify the archived prompt file.

---

# Code quality

Write production-quality code.

Avoid:

* TODOs without explanation
* placeholder implementations
* commented-out code
* dead code
* unnecessary abstraction
* premature optimization

Keep methods reasonably small.

Choose descriptive names.

Prefer removing obsolete code over preserving compatibility layers unless explicitly requested.

Avoid creating helper methods that are only called once unless they significantly improve readability.

When implementing a change, remove obsolete code, comments, configuration, documentation, and resources that are no longer needed.

Add comments only where they improve understanding of non-obvious behavior.

---

# Testing

When practical:

* add unit tests for loader-independent logic
* add game tests for Minecraft behavior
* verify both Fabric and NeoForge builds

Do not claim tests passed unless they were actually executed.

---

# Final reports

At the end of each implementation pass, include:

1. Summary of the implementation.
2. Files added or modified.
3. Build/test results.
4. Important implementation decisions.
5. Known limitations.
6. Recommendations for the next implementation pass.
