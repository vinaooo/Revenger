# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Revenger is an Android app that packages a single retro game into a self‑contained APK: ROM + LibRetro core + full configuration, so "install the APK" is the only step to play. It is a GPLv3 fork of [Ludere](https://github.com/tytydraco/Ludere). Single Gradle module `:app`, package `com.vinaooo.revenger`, Kotlin, view‑based UI (no Compose, no Material components).

## Commands

Build/run (Android SDK expected at `~/Android/Sdk`; set in `local.properties`):

```bash
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease        # release APK, signed with revenger.jks (creds are in app/build.gradle)
./gradlew installDebug           # build + install on connected device
./gradlew clean                  # also deletes app/src/main/jniLibs (downloaded cores)
```

Debug builds install as `<applicationId>.debug`, and their launcher icon carries a red "DEBUG" banner (`app/src/debug/res/`). A debug install and a release install of the same game live side by side, so `installDebug` and `connectedDebugAndroidTest` never touch the release app or its saves.

Tests — **unit test sources live in `tests/` at the repo root**, wired in via `sourceSets { test.java.srcDirs += "../tests" }`. Files are named `*_test.kt`; many test method names are in Portuguese. Instrumented tests are under `app/src/androidTest/`.

```bash
./gradlew testDebugUnitTest                                              # all unit tests (Robolectric/MockK/Mockito)
./gradlew testDebugUnitTest -PskipAssetStaging                           # same, without a staged ROM/core/icons (clean checkout / CI)
./gradlew testDebugUnitTest --tests "com.vinaooo.revenger.models.SaveSlotData_test"
./gradlew connectedDebugAndroidTest                                      # instrumented tests, needs a device
```

`connectedDebugAndroidTest` includes `EmulatorSmokeTest`, which runs the real packaged core and ROM (no mocks): first frame, then state save/load round trips. It needs a full build with the ROM and core staged, so don't pass `-PskipAssetStaging`.

**Every change must include tests.** Adding a class or method → add tests for it. Fixing a bug → add a regression test that fails without the fix. Changing existing behavior → update the tests that cover it. If the code you're touching has no test yet, write one as part of the change instead of deferring it — see `definitions/Code.md` for testing conventions and patterns (mocking Android framework classes, resetting stateful singletons between tests, etc.).

Coverage: `./gradlew koverHtmlReportDebug -PskipAssetStaging` writes the report to `app/build/reports/kover/htmlDebug/`. `koverVerifyDebug` runs as part of `./gradlew check` and fails it if debug coverage drops below the floor in `app/build.gradle` (`kover { … verify { rule … } }`: 72% lines, 56% branches). When coverage grows, raise the floor; never lower it to get a change through.

Static analysis: `./gradlew detekt detektMain detektTest` (config: `detekt.yml`, `maxIssues: 0`). `detektMain` is the type‑resolution run (catches `NullableToStringCall`, `UnsafeCallOnNullableType`, `VarCouldBeVal`, …). `detektTest` checks `tests/` and `app/src/androidTest/` with `detekt-test.yml` layered on top: it relaxes only rules whose findings are idiomatic in tests (backtick names, fixture numbers, `!!`, stub overrides; each has its reason in that file). All three must stay at 0 issues and all run as part of `./gradlew check`. Android lint has `abortOnError = false`.

Screenshot tests: `tests/RetroMenu3Screenshot_test.kt` renders the RetroMenu3 screens with Roborazzi (Robolectric native graphics) and compares them against the goldens in `tests/screenshots/`. Every unit-test run verifies them (`roborazzi.test.verify=true` in `gradle.properties`), so `check` fails on any pixel change. After an intended UI change, run `./gradlew recordRoborazziDebug -PskipAssetStaging`, look at the new PNGs, and commit them with the change. Diff images from a failed run land under `app/build/outputs/roborazzi/`. Keep config-driven content (game name, platform profile values) out of the captures: stub it through the ViewModel, as that test does. The goldens are recorded on Linux; native graphics renders slightly differently on macOS and Windows, so verify and record them on Linux.

App icons: `python3 icons/scripts/master_icon.py` (also runs automatically as the `generateIcons` Gradle task before every build) scrapes console/game art from SteamGridDB/IGDB. `./pick_icon.sh` opens an interactive HTML picker when the auto‑pick is bad. Needs Python + Pillow (`venv/` is local and gitignored, not checked in). Runtime dependencies are declared in `icons/requirements.txt`. The repo `venv/` is broken (its `bin/python` and libs are different Python versions); use the system `python3` or a fresh venv.

Script tests: `./gradlew testScripts` runs the pytest suite in `icons/tests/` (the icon scripts, and ruff with the pinned `ruff.toml`). It is part of `./gradlew check` and runs with `-PskipAssetStaging` too. On first use `setupScriptsVenv` creates `build/scripts-venv` from `icons/requirements.txt` + `icons/requirements-dev.txt` (needs `python3` with `venv`, and network for pip); it is rebuilt only when those files change. Script tests must never read the real `icons/.env`, write to `app/src/main/res` or `icons/.cache`, or touch the network: use temp dirs and monkeypatch.

## Git workflow

**Never commit directly to `develop`.** Every change — a bug fix, a refactor, a docs update, anything — is made on its own branch created from `develop` (branch off the latest `develop`, not off `master` or another feature branch). `develop` only advances by merging those branches in through a pull request; it is never the branch you commit to directly.

- Before making any change, create a new branch from `develop` (e.g. `fix/<short-description>`, `feat/<short-description>`, `docs/<short-description>`).
- When the change is ready, open the pull request against `develop` (never against `master`).
- If you find yourself already on `develop` with uncommitted or committed work, stop and move it: create the branch from the current commit, then reset `develop` back to `origin/develop` before continuing.

## The config system (read before changing build or runtime behavior)

`app/src/main/assets/config/config.json` is the single source of truth for *what game this APK is*. It holds only: `default_settings`, `platform`, `name`, `rom`, `target_abi` (plus optional `core` when manual).

- `default_settings: true` → gameplay settings come from a profile in `app/src/main/assets/default_settings.json`, resolved by `platform` id (falling back to ROM file extension). The Gradle build fails early if `default_settings` is true but `platform` is empty.
- `default_settings: false` → gameplay settings come from `app/src/main/assets/config/config_manual.json`.

At runtime, `AppConfig` (a facade, exposed as `RevengerApplication.appConfig`) merges the base config with either the resolved profile or the manual config, and **every component queries `AppConfig`, never resources directly**. `menu_mode` is a comma string parsed by `AppConfig` (e.g. `combo,gamepad,back,fab=bottom-right`).

`build.gradle` derives `applicationId` dynamically: `com.vinaooo.revenger.<name>_<resolvedCore>` (sanitized). So the package name changes with the configured game/core.

### Build task chain (in `app/build.gradle`, all before `preBuild`)

`generateIcons` → `prepareRom` → `prepareCore` → `preBuild`

- **`prepareRom`**: ROMs are gitignored and live in `roms_backup/` at the repo root. This task moves the active ROM (`rom` in config) into `app/src/main/assets/rom/` for packaging and moves any other ROM back out. `cleanupRom` runs `finalizedBy` every `assemble*/bundle*/install*` to move it back to `roms_backup/`.
- **`prepareCore`**: downloads the LibRetro core `.so` from `buildbot.libretro.com/nightly` for the target ABI(s) into `app/src/main/jniLibs/<abi>/libcore.so`. Does a HEAD check first, skips ABIs that 404, skips download if `jniLibs/<abi>` is already populated. `target_abi: all` fetches x86, x86_64, armeabi-v7a, arm64-v8a.

Gradle configuration cache is disabled because of `prepareCore`.

## Runtime architecture

Launch flow: `views/SplashActivity` (CRT boot animation, `ui/splash/CRTBootView`) → `views/GameActivity`. `RevengerApplication.onCreate` initializes `DefaultSettingsRepository`, `PipConfigRepository`, and the `AppConfig` singleton.

- **`views/GameActivity`** + **`viewmodels/GameActivityViewModel`** — the emulator screen. Both files are very large; the ViewModel owns the `RetroView`, emulation lifecycle, and screenshot logic, and delegates save/load/reset orchestration to `viewmodels/menu/SaveLoadOrchestrator`. PiP orchestration is delegated to `controllers/PipController` (entering/exiting PiP, the PiP overlay still-frame, the Quick Save / Save and Exit PiP actions and their broadcast receiver); `GameActivity` implements the narrow `controllers/PipHost` interface so the controller can be unit-tested without a real Activity. `controllers/PipParamsFactory` and `controllers/PipQuickSaveExecutor` split out of `PipController` to stay under detekt's function-count threshold; `utils/PipSnapshotSelector`, `utils/PipAspectRatioResolver`, and `utils/PipLastSlotScreenshotResolver` hold the pure snapshot-selection/aspect-ratio/last-slot-thumbnail math. Rotation orchestration (the auto-rotate `BroadcastReceiver`, orientation reapply, and post-rotation menu recreation) is delegated to `controllers/RotationController`, which composes `controllers/MenuRotationRecreator` for the multi-step fragment-teardown-and-rebuild chain; `views/menu/RotationFragmentFactory` and `views/menu/RotationMenuStateResolver` (see below) hold the pure fragment-instantiation and state-resolution decisions that chain uses. The Activity also handles input dispatch and menu wiring.
- **`retroview/RetroView`** — wraps LibretroDroid's `GLRetroView` (core loading, serialize/deserialize state, viewport).
- **`input/ControllerInput`** (large) + **`gamepad/`** — physical gamepad handling and the RadialGamePad virtual touchscreen controls; `gamepad/GamePadLayoutAdjuster` repositions the pads on rotation.
- **`managers/`** — `SaveStateManager` (multi‑slot saves + save‑states, `slotN.sav` / `slotN.state`, previews as `.webp`), `SessionSlotTracker`, `AudioRoutingManager`, `GameLifecycleObserver` (SRAM flush on focus loss).
- **`controllers/`** — `AudioController`, `ShaderController` (`disabled` / `sharp` / `crt` / `lcd` / `upscale1`), `SpeedController` (fast‑forward), `FloatingMenuButtonController` (floating menu-button visibility), `PipController` (Picture-in-Picture orchestration, see above), `RotationController` + `MenuRotationRecreator` (rotation/auto-rotate orchestration, see above).
- **`viewmodels/menu/SaveLoadOrchestrator`** — save/load/reset orchestration extracted from `GameActivityViewModel`, keyed off the emulator's frame-speed state.
- **`views/menu/RotationMenuStateResolver`** — pure decision of which `MenuState` a rotation-triggered menu recreation should rebuild, extracted from `GameActivity.onConfigurationChanged`. `views/menu/RotationFragmentFactory` is the matching pure factory that instantiates the fragment for that state.

### RetroMenu3 — the in‑game menu (`ui/retromenu3/`)

Command Pattern + State Machine. Navigable by touch D‑pad, physical gamepad, and keyboard simultaneously.

- `RetroMenu3Fragment` (UI) ↔ `MenuViewModel` (`StateFlow` is the canonical UI state carrier)
- `MenuSystem` orchestrates; `MenuActionHandler` (actions), `SubmenuCoordinator` (submenus), `MenuViewManager` (view updates)
- Navigation is split: `navigation/NavigationStateManager` **owns** state (current menu, selected index, nav stack); `navigation/NavigationEventProcessor` turns raw input into validated events (debounce/rate‑limit) and delegates mutations to the state manager. UI must not mutate navigation state directly.
- Submenu fragments: `SettingsMenuFragment`, `SaveSlotsFragment`, `SaveStateGridFragment`, `ManageSavesFragment`, `ExitFragment`, `AboutFragment`, etc.

## Conventions (from `definitions/Code.md`)

- **No Material Design / Material You / Material Components** — deliberate and enforced. UI uses the custom `RetroCardView` with pixel/arcade styling. Do not reintroduce `com.google.android.material`.
- **No `print` / `println`** — use `android.util.Log` or the project's `MenuLogger`.
- PascalCase classes, camelCase members, KDoc on public APIs. Prefer composition and small focused managers. Keep immutable state objects where practical.
- Preserve public fragment/ViewModel contracts (backward compatibility is a stated rule). **Every change must include tests** — new classes get tests, bug fixes get a regression test, behavior changes get their existing tests updated. New manager/navigation classes specifically need both unit and integration tests.
- **Never name specific games, companies, consoles, or brands** in code, comments, commit messages, PR descriptions, docs, or test data — not even the one currently configured. Use neutral terms: "the game", "the ROM", "the platform", "the core", "an 8‑bit console". The configured title/ROM/platform values live only in `config.json` / `default_settings.json`; refer to them by config key (`name`, `rom`, `platform`), never quote the value.

## Reference docs

- `docs/ANALISE_PROJETO.md` — project deep‑dive (Portuguese)
- `docs/BUILD_DEPLOY_2026-08-27.md` — build/deploy notes and SDK setup troubleshooting
- `definitions/config.md`, `definitions/config_manual.md` — config key reference
- `definitions/Code.md` — architecture and maintenance guide
- `TODO.kt` — roadmap notes (used with the TODO Tree VS Code extension)
