# Bundled Config Defaults

Status: Completed
Date: 2026-07-12

## Prompt

Please refactor Forever World Portals configuration defaults so that they are defined declaratively in a bundled properties resource rather than being duplicated or buried as literal values in Java code.

First inspect the current repository, particularly:

* the existing configuration loading and parsing code
* the loader-specific startup/bootstrap code
* existing configuration tests
* `README.md`
* the prompts under `docs/prompts`

Preserve the current configuration location, key names, behavior, and loader compatibility unless this prompt explicitly changes them.

## Goal

Add a bundled resource named:

```text
fwportals-default.properties
```

Place it in the common module’s resources so that it is packaged in both Fabric and NeoForge jars.

This resource must serve two related purposes:

1. When the user configuration file does not exist, copy the bundled resource verbatim to the normal user/world configuration location as the initial `fwportals.properties`.
2. On every startup, load the bundled resource as the source of default property values. Overlay the user’s existing `fwportals.properties` values on top of those defaults. Therefore, a key omitted from an existing user configuration uses the value from `fwportals-default.properties`.

Conceptually:

```text
effective properties = bundled default properties + user property overrides
```

The bundled defaults must be loaded independently of whether the user configuration file already exists.

## Required behavior

### Bundled default resource

Create:

```text
common/src/main/resources/fwportals-default.properties
```

Populate it with all currently supported configuration keys and their current default values.

Retain useful comments documenting the settings and valid values. This file should be suitable for copying directly into the user configuration directory without further generation or rewriting.

The bundled file should be the canonical source for raw default property values. Do not keep a second manually maintained set of equivalent default strings in Java.

### First startup

When `fwportals.properties` does not exist:

* create its parent directories if necessary
* copy `fwportals-default.properties` to it
* preserve the bundled resource’s comments, ordering, spacing, and general formatting
* do not reconstruct the file using `Properties.store()`
* do not prepend a generated timestamp
* then load the effective configuration normally

Use a stream/resource-copy operation so the initial user file is a literal copy of the bundled template.

Never overwrite or rewrite an existing user configuration merely because defaults have changed or new keys have been added.

### Existing configurations and new settings

When an existing `fwportals.properties` omits a setting:

* use the value from the bundled default resource
* do not add the missing setting to the existing user file automatically
* do not rewrite the existing file
* do not emit a warning merely because the key is absent

This allows newer versions of the mod to add defaulted settings without modifying users’ files.

### Parsing and validation

Continue parsing effective property values into the existing strongly typed configuration representation.

Java code should still own:

* conversion from strings into booleans, integers, enums, resource locations, blocks, items, colors, and other typed values
* range checks
* cross-setting validation
* clear error reporting
* semantic constraints

However, ordinary default values must come from the bundled properties file rather than literals passed to calls such as `getProperty(key, "defaultValue")`.

Avoid APIs or helper methods that silently introduce a second Java-defined default. Once the effective property set has been assembled, every expected setting should be present because the bundled default resource defines it.

Treat a missing required key in the bundled default resource as an internal packaging/programming error. Fail startup with a clear message identifying the missing default key rather than silently inventing a fallback in Java.

Preserve the current behavior for malformed or invalid user-provided values unless there is a compelling implementation reason not to. In particular, do not silently replace an explicitly supplied but invalid value with the default unless that is already the established behavior.

### Resource loading

Load the resource through a classloader-safe mechanism that works from:

* a development environment
* a packaged Fabric jar
* a packaged NeoForge jar

Use try-with-resources and provide clear errors if the bundled resource is unexpectedly absent or unreadable.

Do not rely on the resource being accessible as an ordinary filesystem `Path` inside the jar.

### Structure

Prefer a clean separation along these lines:

* load and validate bundled default properties
* ensure the user config file exists, copying the bundled bytes when necessary
* load user properties when present
* overlay user properties onto defaults
* parse the resulting effective properties into the typed configuration object

It is acceptable to use separate streams for copying and parsing the bundled resource.

Keep the implementation straightforward. Do not introduce a general-purpose configuration framework or new dependency for this change.

## Tests

Add or update focused automated tests covering at least:

1. When no user config exists, the bundled default resource is copied to the expected location.
2. The copied file corresponds to the bundled template and retains its comments rather than being serialized through `Properties.store()`.
3. A completely populated user config overrides bundled defaults.
4. A partial user config uses bundled values for omitted keys.
5. An existing partial user config is not rewritten or augmented.
6. An invalid explicit user value continues to produce the expected validation failure or existing error behavior.
7. A missing expected key in the bundled default properties produces a clear internal configuration error.
8. The bundled resource contains every configuration key expected by the parser.

Design the configuration-loading code so tests can use temporary directories and, where useful, injected/default-resource streams without requiring a running Minecraft server.

## Documentation

Update `README.md` to explain:

* the user-facing configuration file remains `fwportals.properties`
* its initial contents are copied from the bundled `fwportals-default.properties`
* settings omitted from an existing user file use the defaults bundled with the installed mod version
* existing user configuration files are never automatically overwritten merely to add new settings

Do not expose `fwportals-default.properties` as a second user-editable configuration file. It is an internal bundled template and default-value source.

## Compatibility and scope

* Preserve all current config keys and current effective defaults.
* Preserve the existing configuration file location.
* Preserve Fabric and NeoForge support.
* Keep the implementation in common code wherever practical.
* Do not make unrelated gameplay changes.
* Do not add migration machinery beyond treating missing keys as defaulted.
* Do not rewrite existing user config files.
* Run the relevant tests and `./gradlew clean build`.
* Resolve any failures introduced by this refactor.
* Summarize the files changed and the resulting startup/configuration behavior.
