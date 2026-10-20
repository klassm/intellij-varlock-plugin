# IntelliJ varlock Plugin

[![Build](https://github.com/klassm/intellij-varlock-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/klassm/intellij-varlock-plugin/actions/workflows/build.yml)

Run any IntelliJ run configuration wrapped in [`varlock run --`](https://varlock.dev), so environment variables are resolved and validated by varlock and piped output is redacted.

Each push to `main` builds the plugin and archives the installable zip as a workflow artifact (Actions → Build → Artifacts).

## How to use

1. **Install the plugin** — download the zip from the [latest build release](https://github.com/klassm/intellij-varlock-plugin/releases/latest/download/intellij-varlock-plugin-latest.zip) (or from the [Build workflow artifacts](https://github.com/klassm/intellij-varlock-plugin/actions/workflows/build.yml), or build locally with `./verify.sh build`), then: Settings → Plugins → ⚙ → *Install Plugin from Disk…* → select the zip → restart the IDE.
2. **Set up varlock** — make sure `varlock` is available (e.g. `brew install varlock`), or point to it explicitly under Settings → Tools → varlock.
3. **Opt in per run configuration** — open the run configuration (Run → Edit Configurations…), scroll to the **Before Launch** section, click **+**, and choose **varlock**. The entry's checkbox enables/disables it for that configuration.
4. **Run** — the process now starts as `varlock run -- <original command>`: varlock resolves and validates your environment and redacts sensitive output for the whole process tree.
5. **Fail-safe** — if varlock cannot be found, the launch aborts with a clear error by default. To run anyway (with a warning), untick *Fail launch when varlock is missing* under Settings → Tools → varlock.

## Features

- **Opt-in per run configuration**: add "varlock" to any run configuration's **Before Launch** list (native IDE UI — enable/disable per config, persisted by the platform).
- **Full command wrapping**: the launch becomes `varlock run -- <original command>` — varlock's validation and output redaction apply to the whole process tree.
- **Executable resolution**: configured path (Settings → Tools → varlock) → `varlock` on PATH (e.g. Homebrew installs) → `<project>/node_modules/.bin/varlock` → `npx --yes varlock`.
- **Launch safety**: if varlock cannot be found, launches abort with a clear error by default (configurable to warn-and-continue).
- **Idempotent**: commands already running under varlock (`__VARLOCK_RUN` env present) are not wrapped twice.

## Supported run configurations

| Config type | Wrapping | Mechanism |
|---|---|---|
| Node.js, npm | ✅ | JavaScript `nodeRunConfigurationExtension` EP → launch session rewrites the target command line |
| Maven goal, Gradle (client process) | ✅ | `com.intellij.runConfigurationExtension` → `patchCommandLine` (external-system runs) |
| Java Application, JUnit | ⚠️ env-only | `updateJavaParameters` cannot change the java executable; the resolved varlock environment is injected instead (no output redaction) |
| Python (incl. uv-managed interpreters), pytest | ⚠️ env-only | `pythonCommandLineTargetEnvironmentProvider` injects the resolved `varlock load` environment (no output redaction); requires a Python plugin |
| Other config types | ❌ | Each IDE family exposes its own extension point; no generic hook exists |

## Known limitations

- **Gradle**: wrapping covers the Gradle *client* process only; tasks execute in the daemon and do not inherit varlock's environment or redaction.
- **Java Application/JUnit on 2024.2**: the platform does not invoke the command-line patch hook for these configurations (verified against the 242 distribution); newer IDE versions may call it — check the checkbox behavior on your IDE.
- The JavaScript/Node.js integration requires an IDE that bundles those plugins (IntelliJ IDEA Ultimate, WebStorm, etc.); the plugin loads without them (all platform integrations are optional `<depends config-file>` entries) but offers only the hooks available in that IDE.

## Building

```bash
./verify.sh build     # buildPlugin
./verify.sh test      # unit tests
```

The plugin zip is produced at `build/distributions/intellij-varlock-plugin-<version>.zip`.

## Development notes

- Compile platform: IntelliJ IDEA Ultimate 2024.2 (`sinceBuild = 242`); set `VARLOCK_IDE_HOME` to a locally extracted IDE for sandboxed builds.
- `buildSearchableOptions` is disabled (requires the bundled JBR).
- Unit tests run on a standard toolchain JDK, not the IDE runtime.
