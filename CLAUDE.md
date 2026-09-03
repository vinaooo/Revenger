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

Tests — **unit test sources live in `tests/` at the repo root**, wired in via `sourceSets { test.java.srcDirs += "../tests" }`. Files are named `*_test.kt`; many test method names are in Portuguese. Instrumented tests are under `app/src/androidTest/`.

```bash
./gradlew testDebugUnitTest                                              # all unit tests (Robolectric/MockK/Mockito)
./gradlew testDebugUnitTest --tests "com.vinaooo.revenger.models.SaveSlotData_test"
./gradlew connectedDebugAndroidTest                                      # instrumented tests, needs a device
```

Static analysis: `./gradlew detekt` (config: `detekt.yml`; use `detektMain` for type‑resolution variants). Android lint has `abortOnError = false`.

App icons: `python3 icons/scripts/master_icon.py` (also runs automatically as the `generateIcons` Gradle task before every build) scrapes console/game art from SteamGridDB/IGDB. `./pick_icon.sh` opens an interactive HTML picker when the auto‑pick is bad. Needs Python + Pillow (a `venv/` is checked out).

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

- **`views/GameActivity`** + **`viewmodels/GameActivityViewModel`** — the emulator screen. Both files are very large; the ViewModel owns the `RetroView`, emulation lifecycle, save/load, PiP, and screenshot logic; the Activity handles input dispatch, PiP broadcast actions, and menu wiring.
- **`retroview/RetroView`** — wraps LibretroDroid's `GLRetroView` (core loading, serialize/deserialize state, viewport).
- **`input/ControllerInput`** (large) + **`gamepad/`** — physical gamepad handling and the RadialGamePad virtual touchscreen controls.
- **`managers/`** — `SaveStateManager` (multi‑slot saves + save‑states, `slotN.sav` / `slotN.state`, previews as `.webp`), `SessionSlotTracker`, `AudioRoutingManager`, `GameLifecycleObserver` (SRAM flush on focus loss).
- **`controllers/`** — `AudioController`, `ShaderController` (`disabled` / `sharp` / `crt` / `lcd` / `upscale1`), `SpeedController` (fast‑forward).

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
- Preserve public fragment/ViewModel contracts (backward compatibility is a stated rule). New manager/navigation classes need unit + integration tests.
- **Never name specific games, companies, consoles, or brands** in code, comments, commit messages, PR descriptions, docs, or test data — not even the one currently configured. Use neutral terms: "the game", "the ROM", "the platform", "the core", "an 8‑bit console". The configured title/ROM/platform values live only in `config.json` / `default_settings.json`; refer to them by config key (`name`, `rom`, `platform`), never quote the value.

## Reference docs

- `docs/ANALISE_PROJETO.md` — project deep‑dive (Portuguese)
- `docs/BUILD_DEPLOY_2026-08-27.md` — build/deploy notes and SDK setup troubleshooting
- `definitions/config.md`, `definitions/config_manual.md` — config key reference
- `definitions/Code.md` — architecture and maintenance guide
- `TODO.kt` — roadmap notes (used with the TODO Tree VS Code extension)
