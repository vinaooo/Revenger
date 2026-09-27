# Test battery follow-ups — 2026-09-25

Handoff for a new session. Source: the full test battery run on `develop` @ `9b0217d`, after PRs #100–#107.

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- Every change includes tests (see `CLAUDE.md`). Never name games, consoles or brands anywhere.
- Before each PR, run `./gradlew check -PskipAssetStaging`. It covers detekt, detektMain, debug and release unit tests, and lint, and it must pass.
- Run `claude-usage` between items (see the global `CLAUDE.md`).
- `connectedDebugAndroidTest` installs and then uninstalls the **debug** app (`<applicationId>.debug`, since item 1). The release install and its saves are no longer touched.
- ⚠️ `./gradlew clean` deletes `app/src/main/jniLibs`. The core is downloaded again on the next full build.

## Baseline (2026-09-25)

| Suite | Result |
|---|---|
| `detekt` / `detektMain` | 0 / 0 issues |
| `testDebugUnitTest` / `testReleaseUnitTest` | 1202 / 1202 passed each (140 classes) |
| Kover (debug) | 68.3% lines, 52.2% branches, 72.6% methods |
| `lintDebug` | 2 Fatal, 1 Error, 183 warnings |
| `assembleDebug` (full staging) | OK |
| `connectedDebugAndroidTest` | 16 / 16 passed (4 classes) |
| `detektTest` (for information only) | 2234 findings, mostly naming style |

## TODO (recommended order)

### [x] 1. `feat/debug-application-id-suffix`: stop instrumented tests from clobbering installed games — PR #108
- In `app/build.gradle`, give the debug build type `applicationIdSuffix ".debug"`, so debug and release installs can live side by side.
- Check everything that reads the package name or `applicationId`:
  - the `generateConfigId`/`getResolvedCore` logic
  - `LogSaver`'s `contains("debug")` check
  - `DebugOverlayController.isDebugBuild`
  - `FileProvider` authorities in `AndroidManifest.xml`, if any
- Tests:
  - unit tests for any code that parses the package name
  - run `connectedDebugAndroidTest`; the release install must survive it

### [x] 2. `fix/core-variables-default-layout`: lint Fatal `MissingDefaultResource` — PR #109
- `core_variables.xml` only exists in `res/layout-land/` and `res/layout-port/`. Inflating it with an undefined orientation crashes `CoreVariablesFragment`.
- Move one variant (probably portrait) to `res/layout/`.
- Test: a Robolectric test that inflates `CoreVariablesFragment` under a configuration with `ORIENTATION_UNDEFINED`.

### [x] 3. `test/real-config-assets`: validate the real config JSON files — PR #110
- Today `AppConfig_test`, `ConfigSources_test` and `DefaultSettingsRepository_test` all use stubbed assets.
- Add a test that reads the real `app/src/main/assets/default_settings.json` and checks that:
  - every profile parses
  - every profile has the required keys
  - `platform` ids are unique
- The same test should read `app/src/main/assets/config/config.json` and check that:
  - it has the required keys (`default_settings`, `platform`, `name`, `rom`, `target_abi`)
  - when `default_settings: true`, `platform` resolves to a profile
- Refer to values by config key only; never hard-code the configured title, ROM or platform.

### [x] 4. `test/save-load-slot-fragments`: cover the save/load screens — PRs #111, #112, #113
Current coverage:
- `ui/retromenu3/SaveSlotsFragment`: 3.8%
- `ExitSaveGridFragment`: 6.6%
- `LoadSlotsFragment`: 19.4%

Follow the pattern in `tests/ManageSavesFragment_test.kt`, which reaches 69.5%:
- Robolectric
- a mocked `SaveStateManager` injected by reflection into `SaveStateGridFragment.saveStateManager`
- `SaveStateManager.clearInstance()` in setup and teardown

It's probably easiest as one PR per fragment. Avoid the process-kill paths (see the note in `ExitFragment_test`).

### [x] 5. `chore/kover-coverage-floor`: stop coverage from silently dropping — PR #114 (floor 72% lines / 56% branches)
- Kover is report-only today; see the comment in `app/build.gradle` near `kover {`.
- Add a `koverVerify` rule with a floor just under the current baseline (for example 67% lines and 51% branches), and hook it into `check`.
- Update the `CLAUDE.md` commands section.
- Verify: temporarily raise the floor above the actual coverage and confirm `check` fails, then set it back.

### [x] 6. `test/emulator-smoke-instrumented`: end-to-end test with the real core — PR #115
- Today no test loads the real core; the 16 instrumented tests only cover lifecycle and rotation. `retroview/RetroView` has 18.6% coverage.
- Add an `androidTest` that:
  - launches `GameActivity` with the packaged ROM and core
  - waits for frames to render
  - saves state, loads it back, and checks the state bytes round-trip
- Needs a device. Do item 1 first.

### [x] 7. Optional, lower priority — PRs #116–#118
- [x] (PR #116) Review the 7 lint `StaticFieldLeak` warnings: `SaveStateManager`, `ScreenshotCaptureUtil`, `AdvancedPerformanceProfiler`/`DebugOverlayController`, `GameActivityViewModel` (L136, L143), `InputViewModel`.
- [x] (PR #117) Delete the stray scratch files `test_check.kt` and `CheckShader.kt` in the repo root. Nothing compiles them.
- [x] (PR #118) Give test sources their own detekt profile (relax `FunctionNaming`, `MagicNumber`, `InvalidPackageDeclaration` and `ClassNaming`) if `detektTest` should ever be gated.
- [x] (PR #118) Add screenshot tests (for example Roborazzi) for the RetroMenu3 pixel UI.
- [x] (PR #118) Add `pytest` tests for the pure helpers in `icons/scripts/utils.py` (only `py_compile` runs today).

## Known lint false positives (no change needed)
- `MissingPermission` on `Build.getSerial()` in `utils/LogSaver.kt`: the `SecurityException` is caught.
- `UnspecifiedRegisterReceiverFlag` in `views/GameActivity.kt` (`registerPipReceiver`): `RECEIVER_NOT_EXPORTED` is passed on API 33+, and `minSdk` is 30.
