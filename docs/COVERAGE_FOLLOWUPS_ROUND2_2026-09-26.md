# Test and coverage follow-ups, round 2 — 2026-09-26

Source: `./gradlew koverXmlReportDebug -PskipAssetStaging` on `develop` @ `2aef786` (after PR #147). Written after every item in `COVERAGE_FOLLOWUPS_2026-09-26.md`, `TEST_BATTERY_FOLLOWUPS_2026-09-2{5,6}.md` and `GAMEACTIVITY_DECOMPOSITION_2026-09-26.md` was done.

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- **Ask before on-device runs** (items 11–12).
- Every change includes tests (see `CLAUDE.md`). Never name games, platforms or brands anywhere.
- Before each PR:
  - run `./gradlew check -PskipAssetStaging`
  - put the before/after coverage numbers for the touched files in the PR
- Run `claude-usage` between items.
- **When a test finds a real bug:** stop, report it, and fix it in its own PR with a regression test (as #143 was split from #144), unless the user says to fold it in.
- Mark each item `[x]` here with its PR number when it's done.

## Estimate

About **10–12 PRs** for the core plan (items 1–10, with items 3 and 4 possibly split in two), **+2** optional device items (11–12), and **0–2** bug-fix PRs if the new tests find something. Expected result: Kotlin unit coverage from 87.4% / 68.6% to roughly **90–91% lines / 74–76% branches**.

## Baseline

| Code | Lines | Branches |
|---|---|---|
| Kotlin (app, unit tests) | 87.4% (8470/9686) | 68.6% (2426/3539) |
| Python (icons/scripts) | 98.1% (742/756) | 93.1% (216/232) |
| Shell (*.sh) | 100.0% (11/11) | – |
| **All code** | **88.2% (9223/10453)** | 70.1% (2642/3771) |
| Kotlin (instrumented tests, not in All code) | 42.7% (5178/12130) | 20.4% (843/4129) |

The Kover floor is 87 / 68.

### Where the 1,216 missed Kotlin lines are

| File | Lines covered | Lines missed | Branches missed | Unit-testable? |
|---|---|---|---|---|
| `views/GameActivity` | 0% | 195 | 34 | device only |
| `viewmodels/GameActivityViewModel` | 68.4% | 94 | 53 | yes |
| `retroview/RetroView` | 18.6% | 83 | 28 | device only |
| `ui/splash/CRTBootView` | 69.3% | 43 | 6 | mostly device |
| `gamepad/GamePad` | 55.6% | 36 | 29 | yes |
| `utils/RetroViewUtils` | 0% | 32 | 22 | device only |
| `input/KeyEventRouter` | 70.9% | 30 | 18 | yes |
| `controllers/PipQuickSaveExecutor` | 16.7% | 30 | 7 | yes, needs a seam |
| `input/MotionEventRouter` | 79.8% | 24 | 28 | yes |
| `performance/ProfilingSessionController` | 47.8% | 24 | 11 | yes |
| `privacy/EnhancedPrivacyManager` | 0% | 24 | 12 | yes |
| `gamepad/GamePadConfig` | 82.3% | 22 | 0 | yes |
| `ui/retromenu3/RetroEditText` | 68.6% | 22 | 14 | `onDraw` only |
| `utils/ViewUtils` | 31.2% | 22 | 2 | yes |
| `ui/retromenu3/MenuSystem` | 89.7% | 21 | **73** | yes |
| `controllers/PipController` | 83.6% | 19 | 44 | yes |
| `utils/FontUtils` | 54.8% | 19 | 17 | yes |
| `utils/OrientationManager` | 41.9% | 18 | 11 | yes (Robolectric) |
| `ui/retromenu3/SaveSlotsFragment` | 94.6% | 14 | **59** | yes |

## TODO (recommended order)

### [ ] 1. `test/pip-quick-save-executor`: the PiP Quick Save path
- **Why first:** it writes the player's save. A bug here loses progress, and it's at 16.7%.
- **What's missed:** the whole body of the coroutine (lines and 7 branches of `execute`):
  - the save written to the last used slot, or slot 1 when there is none
  - an empty slot named "Slot N", while a used slot keeps its name
  - `recordSave` called
  - the abort when no frame arrives within `FRAME_TIMEOUT_MS`
  - the `catch` when `serializeState()` throws
  - `finishPipTask` always posted
- **Seam needed:** the save runs on a raw `Thread` and reaches the `SessionSlotTracker`, `SaveStateManager` and `ScreenshotCaptureUtil` singletons. Add constructor parameters with production defaults: a background runner (`(Runnable) -> Unit`, default `Thread(it).start()`) and the save collaborators. Tests then run it synchronously with fakes. Keep the public constructor used by `PipController` working.
- **Check while there:** `romName` comes from `getString(R.string.name)`. `CLAUDE.md` says components query `AppConfig`, never resources. Confirm whether this should be `AppConfig`'s `name`. If it's a real inconsistency, fix it in its own PR.

### [ ] 2. `test/input-event-routers`: `KeyEventRouter` and `MotionEventRouter`
- **Why:** controller input players use constantly. 46 branches missed between them.
- **`KeyEventRouter`:** `interceptButtonB` (15 lines, 7 branches) and `fireDpadCallback` (9 lines).
- **`MotionEventRouter`:**
  - `fireTriggerCallback` (15 lines)
  - `checkSingleTrigger` (9 branches)
  - `computeDirectionTrigger` (8 branches)
  - `isAnyAxisOutOfDeadzone` (6 branches)
- Test the deadzone edges exactly (just inside, on, just outside the threshold) and each direction/trigger combination.
- Split into two PRs if the diff gets large.

### [ ] 3. `test/game-activity-viewmodel-remaining`: `GameActivityViewModel`
- **Why:** the largest block of real logic left (94 lines, 53 branches).
- **What's missed:**
  - `onBackToMainMenu` (17 lines, 8 branches)
  - `onMenuEvent` (12 / 11)
  - `preserveState` (10 branches)
  - `initializeControllers` (8 / 6)
  - `setupRetroView` and its lambda (14 / 10)
  - `onCleared` (16 lines)
  - `setConfigOrientation`
  - `onAboutBackToMainMenu`
  - the two `init` lambdas
- Probably two PRs: **3a** menu events and back-to-main (`onMenuEvent`, `onBackToMainMenu`, `onAboutBackToMainMenu`); **3b** lifecycle and setup (`preserveState`, `initializeControllers`, `setupRetroView`, `onCleared`, `setConfigOrientation`).
- Reuse the fixtures in the existing `GameActivityViewModel_*_test.kt` files. Add a new file per topic rather than growing one.

### [ ] 4. `test/gamepad-events`: `GamePad` and `GamePadConfig`
- **`GamePad`:**
  - `handleButtonEvent` (18 lines, 12 branches)
  - `handleDirectionEvent` (12 / 7)
  - `eventHandler`
  - `hasExternalPhysicalController` (4 branches), which decides whether the on-screen pad shows
- **`GamePadConfig`:** 22 lines missed, no branches. Likely the button/layout definitions for configurations the current tests don't build.

### [ ] 5. `test/menu-system-branches`: `MenuSystem`
- 90% of lines but **73 branches missed**, the most in the project. Mostly the "not ready / null fragment / wrong state" guards.
- List the uncovered branches from the Kover HTML first, then add one test per guard. Don't chase branches that are only Kotlin null-safety bytecode.

### [ ] 6. `test/save-slots-fragment-branches`: `SaveSlotsFragment` and the other fragments
- **`SaveSlotsFragment`** (59 branches missed):
  - `hideDialog` (11)
  - `performConfirm` (8)
  - the naming dialog show/hide (6 + 6)
  - `updateDialogSelection` (6)
  - `performNavigateUp` / `performNavigateDown` (4 + 4)
  - `performSave`
- **Also:**
  - `ExitFragment.performAutoSaveAndExit` and `performConfirm`
  - `CoreVariablesFragment.setupViews`, `handleItemClick` and `updateSelectionVisualInternal`
  - `RetroKeyboard.handleDpadEvent`
- Use the shared `tests/MenuFragmentHost.kt`. If a UI change is involved, rerun the Roborazzi goldens; there shouldn't be one.

### [ ] 7. `test/pip-controller-branches`: `PipController`
- 19 lines and **44 branches** missed: the guard chains (API level, PiP disabled, already in PiP, no frame yet) and the mode-change paths.
- Could merge into item 1 if that PR stays small.

### [ ] 8. `test/privacy-and-profiling`: `EnhancedPrivacyManager` and `ProfilingSessionController`
- **`EnhancedPrivacyManager`** (0%):
  - `hasStoragePermissions` (per API level, 4 branches)
  - `hasBasicPermissions`
  - `initializePrivacyControls`
  - Use Robolectric `@Config(sdk = [...])` for the API branches.
  - First check whether it's still used anywhere. If it's dead code, propose removing it instead of testing it.
- **`ProfilingSessionController`:** `collectPerformanceData` (14 lines, 7 branches), `startStandardProfiling`, and the monitoring loop's runnable. Drive it with Robolectric's main looper.

### [ ] 9. `test/small-utils`: `ViewUtils`, `FontUtils`, `OrientationManager`, `EventQueue`
- **`ViewUtils.animateMenuView`** (10 lines).
- **`FontUtils`:**
  - `applyArcadeFont`
  - `applySelectedFont`
  - `applyTextCapitalization`
  - `getCapitalizedString`
  - the selected/unselected colors
- **`OrientationManager`:** `forceConfigurationBeforeSetContent` (11 / 6) and `applyConfigOrientation` (7 / 5), under Robolectric.
- **`EventQueue`:** `shouldDebounce` (5 branches) and `getStats`.
- **Also:** `MenuLayoutConfig.applyDialogVerticalPosition` / `applyVerticalProportions` and `LogSaver.getInputMethodInfo`.

### [ ] 10. `chore/raise-kover-floor-4`: raise the floor
- Run `./gradlew coverageAll -PskipAssetStaging` and raise `kover { verify { rule } }` in `app/build.gradle` to the new measured values, rounded down.
- Update the floor numbers in `CLAUDE.md`.
- Fill in the Result section below.

### [ ] 11. (optional, device) `test/device-activity-coverage`: more instrumented coverage
- Covers the parts only a device can run: `GameActivity` (195 lines), `RetroView` (83), `RetroViewUtils` (32), `CRTBootView` (43).
- **Ideas:**
  - pause/resume and the SRAM flush on focus loss
  - the shader switch through the menu
  - fast-forward on/off
  - rotate with the menu closed
  - the full splash → game path, timed
- Run `./gradlew createDebugAndroidTestCoverageReport -PinstrumentedCoverage`, then `coverageAll`, and put the instrumented row (now 42.7% / 20.4%) before and after in the PR.

### [ ] 12. (optional, device) The key-up check from `KEYUP_INPUT_SIDE_EFFECTS_2026-09-27.md`
- On a device with a physical controller: does a held button auto-repeat `KEY_DOWN`?
- Record the answer in that doc. Only if the fade fix turns out to be needed, open its PR following that doc's checklist.

## Out of scope

- `RetroEditText.onDraw` (canvas drawing). The Roborazzi screenshots cover how it looks; branch-testing the draw calls adds little.
- Kotlin null-safety and `when`-exhaustiveness branches that no input can reach. Don't write tests just to satisfy the counter.
- Lowering the Kover floor, ever.

## Result

*(fill in when item 10 is done)*
