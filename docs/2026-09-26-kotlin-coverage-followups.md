# Kotlin coverage follow-ups — 2026-09-26

Source: `./gradlew coverageAll -PskipAssetStaging` on `develop` @ `6c4c0b9` (after PR #127).

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- Every change includes tests (see `CLAUDE.md`). Never name games, consoles or brands anywhere.
- Before each PR:
  - run `./gradlew check -PskipAssetStaging`
  - run `./gradlew coverageAll -PskipAssetStaging` and put the before/after numbers in the PR
- Run `claude-usage` between items.

## Baseline

| Code | Lines | Branches |
|---|---|---|
| Kotlin (app, unit tests) | 74.0% (7249/9800) | 58.5% (2077/3553) |
| Python (icons/scripts) | 98.1% (742/756) | 93.1% (216/232) |
| Shell (*.sh) | 100.0% (11/11) | – |
| **All code** | **75.7% (8002/10567)** | 60.6% (2293/3785) |

The Kover floor is 73 / 57.

### Where the 2,551 missed Kotlin lines are
1. **Screens with no unit tests** (~470 lines): `views/GameActivity` 265 (0%), `ui/splash/CRTBootView` 159 (0%), `views/SplashActivity` 46 (0%). The two activities run in the instrumented tests, which Kover doesn't measure.
2. **Wrappers around native, GL or device APIs** (~530 lines): `utils/LogSaver` 136 (6%), `performance/DebugOverlayController` 89 (16%), `utils/ScreenshotCaptureUtil` 87 (17%), `retroview/RetroView` 83 (19%), `utils/RetroViewUtils` 33 (0%), `utils/OrientationManager` 31 (0%).
3. **Menu fragments and navigation, partly tested** (~600 lines):
   - `RetroMenu3Fragment` 107 missed (51%)
   - `ManageSavesFragment` 92 (70%)
   - `ExitFragment` 68 (64%)
   - `NavigationEventProcessor` 65 (68%)
   - `FragmentNavigationAdapter` 61 (55%)
   - `ProgressFragment` 49 (75%)
   - `AboutFragment` 39 (75%)
   - `SettingsMenuFragment` 37 (79%)
   - `MenuActionHandler` 31 (52%)
4. **Dead code** (~95 lines): see item 1.
5. **The rest** (~30%): error branches, logging and small edge cases spread over many files.

## TODO (recommended order)

### [x] 1. (PR #128) `chore/remove-dead-code`: delete code nothing calls
- **`utils/LibRetroDownloader`:**
  - Left over from commit `cf32746`, which replaced the old download plugin.
  - The `prepareCore` Gradle task does the same work inline (`app/build.gradle`: buildbot URL, HEAD check, download and extract), and nothing runs the class: no `JavaExec` task, no Kotlin call site.
  - It is still compiled into the APK.
  - Only `tests/LibRetroDownloader_test.kt` references it.
- **`ui/effects/`** (`BackgroundEffect` + `BackgroundEffectFactory`, `NoEffect`, `ScanlineEffect`):
  - Background effects for the old RetroMenu2 pause screen (`retromenu2_background_effect`). They were left behind when the menu became RetroMenu3.
  - Nothing outside `ui/effects/` references them, and no config key exists.
  - Only `tests/BackgroundEffect_test.kt` references them.
  - Not to be confused with `ShaderController`, which is the live in-game shader.
- Delete the classes and their tests.
- **Guard:** a unit test that scans `app/src/main` and asserts the deleted class names don't come back. Optional; decide in the PR.
- Estimated gain: about +0.5 points of lines.
- **Check:** `prepareCore` still downloads a core. Run a real `./gradlew assembleDebug` on a clean `jniLibs`.

### [x] 2. (PRs #129, #130) `test/menu-fragments-robolectric`: menu fragments and navigation
- **2a done (PR #129):** `NavigationEventProcessor` 98%, `FragmentNavigationAdapter` 99%, `MenuActionHandler` 100%. Kotlin 76.1% / 60.2%.
- **2b done (PR #130):** the six fragments are now at 92–98% lines, using the shared `tests/MenuFragmentHost.kt`. Kotlin 79.9% / 63.8%; all code 81.2%.
  - Unit tests now get `maxHeapSize = '2g'`: the 512m default ran out of memory.
- For item 4: `MenuActionHandler.executeSaveLog` runs `LogSaver.saveCompleteLog` on the main thread (`lifecycleScope.launch` uses the Main dispatcher), despite its comment saying "background thread".
- Robolectric tests, using the `ScreenshotHostActivity` pattern with a stubbed `GameActivityViewModel` from `tests/RetroMenu3Screenshot_test.kt`:
  - `RetroMenu3Fragment`, `ManageSavesFragment`, `ExitFragment`, `ProgressFragment`, `AboutFragment`, `SettingsMenuFragment`
  - `NavigationEventProcessor`: debounce/rate limit, invalid events
  - `FragmentNavigationAdapter`
  - `MenuActionHandler`
- Look at the Kover HTML report per file first; target the uncovered branches, not the lines that are already green.
- Split this into two PRs (fragments / navigation) if it gets large.
- Estimated gain: +4–5 points of lines.

### [x] 3. (PR #131) `test/rotation-recreator-and-animations`
- Done: `MenuRotationRecreator` 100% lines, `AnimationOptimizer` 99%. Kotlin 81.9% / 64.6%; all code 83.1%.
- Also removed `AnimationOptimizer`'s dead pooled single-view path. It threw "Already in the pool!" on cancel.
- `controllers/MenuRotationRecreator` (36%): the teardown/rebuild chain with fake fragments.
- `utils/AnimationOptimizer` (0%, used by `MenuViewManager` through `ViewUtils.animateMenuViewsBatchOptimized`): with a Robolectric paused looper and ShadowValueAnimator.
- Estimated gain: +1.5–2 points.

### [x] 4. (PR #132) `refactor/extract-testable-device-logic`
- Done: `LogReport`, `DebugOverlayText` and the `PixelCopier` seam. Kotlin 84.9% / 66.8%; all code 85.9%.
- Bugs fixed on the way:
  - the overlay gate read a missing resource, so the overlay never showed
  - the ROM status looked in raw resources
  - keyboards were counted as gamepads
  - `SAVE_LOG` ran on the main thread
- Pull pure logic out of `LogSaver` (report formatting, file naming, rotation), `DebugOverlayController` (the metrics text and thresholds) and `ScreenshotCaptureUtil` (crop and scale math) into small classes, then test them. This follows the pattern already used for PiP and rotation (`PipSnapshotSelector`, `RotationMenuStateResolver`, ...).
- Leave the actual framework calls (PixelCopy, logcat, GL) thin.
- Estimated gain: +2–3 points.

### [x] 5. (PR #133) `feat/instrumented-coverage-report`
- Done: run `createDebugAndroidTestCoverageReport -PinstrumentedCoverage`. The instrumented row shows 30.5% lines (GameActivity 63%, RetroView 71%).
- Also fixed: `connected*` tasks now run `cleanupRom`.
- The AVD's `pixel_9a` skin is missing from the SDK. Start it with `-skin 1080x2424`.
- Turn on `enableAndroidTestCoverage` for debug. Then run `createDebugAndroidTestCoverageReport` on the AVD (`Pixel_9a_Android_16`), and add its JaCoCo XML to `coverageAll` as a separate row: "Kotlin (instrumented)".
- This is reporting only; the Kover floor stays unit-test based.
- It shows what `GameActivity`, `SplashActivity` and `RetroView` really get from the 20 instrumented tests.

### [x] 6. (PR #134, pending merge) `chore/raise-kover-floor-2`
- Done: the floor is now 84/66 (measured 84.92 / 66.75).
- After items 1–4, raise the floor to about 1 point below the new numbers.

## Out of scope
- `ui/splash/CRTBootView` (boot animation, 159 lines) and `retroview/RetroView` (native core wrapper). They are covered at best by instrumented tests (item 5), not unit tests.
