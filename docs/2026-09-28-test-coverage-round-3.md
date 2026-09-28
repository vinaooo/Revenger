# Test and coverage follow-ups, round 3 — 2026-09-28

Source: `./gradlew coverageAll -PskipAssetStaging` on `develop` @ `8df220e` (after PR #170). Written after every item in `2026-09-26-test-coverage-followups-round-2.md` was done, including its dead-code follow-ups (#166–#170).

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- Every change includes tests (see `CLAUDE.md`). Never name games, platforms or brands anywhere.
- Before each PR:
  - run `./gradlew check -PskipAssetStaging`
  - put the before/after coverage numbers in the PR
- Run `claude-usage` between items.
- **When a test finds a real bug:** stop, report it, and fix it in its own PR with a regression test, unless the user says to fold it in.
- **Dead code gets deleted, not tested.** Before testing a method, check that something calls it. The reliable check is to rename it and compile (`./gradlew compileDebugKotlin -PskipAssetStaging`); grep misses calls without a receiver.
- **Don't test impossible branches**, and don't write tests just to satisfy the counter (null-safety or `when` branches no input can reach).
- Mark each item `[x]` here with its PR number when it's done.

## Baseline

| Code | Lines | Branches |
|---|---|---|
| Kotlin (app, unit tests) | 93.3% (8024/8597) | 80.3% (2349/2927) |
| Python (icons/scripts) | 98.1% (742/756) | 93.1% (216/232) |
| Shell (*.sh) | 100.0% (11/11) | – |
| **All code** | **93.7% (8777/9364)** | 81.2% (2565/3159) |

The Kover floor is 93 / 80.

Where the 573 missed Kotlin lines are:

| File | Missed lines | Missed branches | Why |
|---|---|---|---|
| `views/GameActivity.kt` | 189 of 189 | 34 of 34 | Assumed device-only until now (see below) |
| `retroview/RetroView.kt` | 102 of 102 | 28 of 28 | Builds the native `GLRetroView` (see below) |
| `ui/splash/CRTBootView.kt` | 43 | 6 | Canvas drawing: Won't do |
| `ui/retromenu3/RetroEditText.kt` | 22 | 14 | Canvas drawing: Won't do |
| 105 other files | 217 | 496 | Mostly branches, a few per file |

Expected result: roughly **96–97% lines / 84–86% branches**.

## What changed since round 2

Round 2 listed `GameActivity` and `RetroView` as device-only. Two spikes on 2026-09-28 showed both run under Robolectric:

- **`GameActivity`:** the test builds the activity with `Robolectric.buildActivity`, then fetches its `GameActivityViewModel` through `ViewModelProvider(activity)` before `create()`. That lets it set `retroViewFactory` to a mock, so `onCreate` never loads the native core. The mock `GLRetroView` needs `parent` = null, `findViewById` = null and real `FrameLayout.LayoutParams`. `GamePad.shouldShowGamePads` needs stubbing, because Robolectric's activity has no display during `onCreate`. One smoke test through create → destroy covered 126 of 189 lines.
- **`RetroView`:** `mockkConstructor(GLRetroView::class)` with the `frameSpeed`, `shader`, `onResume`, `onPause` and `onDestroy` setters stubbed; the real setters load the native library. The ROM comes from a `ContextWrapper` whose `AssetManager` is a mock. One test covered 64 of 102 lines.

## TODO (in order)

### [x] 1. `chore/remove-dead-code-3`: delete code nothing runs (#172)
Found while sampling the gaps; check each one with a rename and compile before deleting:
- `RetroMenu3ToggleController`: the delayed `Handler` block in the dismiss path only logs.
- `MenuViewManager.updateMenuState` (empty body), and `animateMenuIn` / `animateMenuOut` if nothing calls them.
- `RetroCardView`: the `expectedUnreachable` catch around `setBackgroundColor`.
- Anything else the same sweep finds among the 105 small files.

### [x] 2. `refactor/shader-type`: one shader table (#173)
Shader names are mapped three times: twice in `RetroView` (`applyShaderInRealtime` and `getShaderConfig`) and once in `ShaderController.getCurrentShaderDisplayName`. `utils/ShaderType` already holds display and config names but lacks the three upscale shaders. Add them, give `ShaderType` a lookup by config name and the matching `ShaderConfig`, and make all three places use it. Unknown names keep today's fallbacks: Sharp for rendering, "Unknown" for the display name.

### [x] 3. `test/retro-view`: `RetroView` (#174)
Using the spike's setup: ROM copy (and the missing-ROM error), the configured shader, the core variables (including malformed entries and values containing `=`), SRAM loading when the file exists, `dynamicShader`, the frame-rendered and FPS listeners, and pause/resume/destroy.

### [x] 4. `test/game-activity`: `GameActivity` (#175)
Using the spike's setup: the create → destroy lifecycle, input dispatch (`onKeyDown`, `onKeyUp`, `onGenericMotionEvent`, `dispatchTouchEvent`), the `PipHost` methods, `onConfigurationChanged`, saved-state handling, `onUserLeaveHint`, `onPictureInPictureModeChanged`, and `startShutdownAnimation`.

### [x] 5. `test/menu-fragment-branches`: the menu fragments (#178)
Missed branches in `ExitFragment`, `ProgressFragment`, `AboutFragment`, `RetroMenu3Fragment`, `MenuFragmentBase`, `SubmenuCoordinator`, `RetroKeyboard`, `CoreVariablesFragment`, `ExitSaveGridFragment`, `ManageSavesFragment` and `SettingsMenuFragment`.

### [x] 6. `test/controllers-and-utils`: the rest (#179)
`PipController`, `GamePadLayoutAdjuster`, `ScreenshotGeometry`, `ScreenshotCaptureUtil`, `ShaderController`, `RotationController`, `LogSaver`, `PipConfigRepository`, `SplashActivity`, `NavigationEventProcessor`, `KeyboardInputAdapter`, `FloatingMenuButtonController`, `GlowAnimationController`, `MenuAnimationController`, `SaveLoadOrchestrator`, `MenuCloseHandler`, `SubmenuFragmentDismisser`.

### [ ] 7. `chore/raise-kover-floor-5`
Run `./gradlew coverageAll -PskipAssetStaging`, raise `kover { verify { rule } }` in `app/build.gradle` to the new values rounded down, update `CLAUDE.md`, fill in the Result section.

**Done when** items 1–7 are merged, `check` passes with the raised floor, and every file still below 80% is in "Won't do" with its reason.

## Won't do

- **`CRTBootView` and `RetroEditText` drawing.** Canvas drawing, covered visually by the Roborazzi screenshots.
- **Bytecode-only branches** (null-safety and `when` checks no input can reach).
- **Instrumented coverage.** It stays reporting-only and needs a device; ask before any on-device run.

## Result

_Filled in by item 7._
