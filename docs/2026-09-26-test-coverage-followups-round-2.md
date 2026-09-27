# Test and coverage follow-ups, round 2 — 2026-09-26

Source: `./gradlew koverXmlReportDebug -PskipAssetStaging` on `develop` @ `2aef786` (after PR #147). Written after every item in `2026-09-26-kotlin-coverage-followups.md`, `2026-09-25-test-battery-followups.md`, `2026-09-26-test-battery-followups-icon-scripts.md` and `2026-09-26-gameactivity-decomposition-plan.md` was done.

**Revised 2026-09-27** after checking every claim against the code (see "What the first draft got wrong"). The plan below replaces the first draft's item list.

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- Every change includes tests (see `CLAUDE.md`). Never name games, platforms or brands anywhere.
- Before each PR:
  - run `./gradlew check -PskipAssetStaging`
  - put the before/after coverage numbers in the PR
- Run `claude-usage` between items.
- **When a test finds a real bug:** stop, report it, and fix it in its own PR with a regression test (as #143 was split from #144), unless the user says to fold it in.
- **Dead code gets deleted, not tested.** Before testing a method, check that something calls it.
- **Don't test impossible branches.** `minSdk` is 30, so an `SDK_INT < O` check can never be true: delete it.
- Mark each item `[x]` here with its PR number when it's done.

## Baseline

| Code | Lines | Branches |
|---|---|---|
| Kotlin (app, unit tests) | 87.4% (8470/9686) | 68.6% (2426/3539) |
| Python (icons/scripts) | 98.1% (742/756) | 93.1% (216/232) |
| Shell (*.sh) | 100.0% (11/11) | – |
| **All code** | **88.2% (9223/10453)** | 70.1% (2642/3771) |
| Kotlin (instrumented tests, not in All code) | 42.7% (5178/12130) | 20.4% (843/4129) |

The Kover floor is 87 / 68. The only Kover exclusion is generated ViewBinding code.

Expected result: roughly **91–92% lines / 75–78% branches**. Deleting dead code shrinks the total as well as adding covered lines.

## What the first draft got wrong

| First draft | Verdict | What the code shows |
|---|---|---|
| Test `EnhancedPrivacyManager` per API level | Wrong: delete | Empty placeholder bodies. Deleted in #150 with the permission plumbing that only fed it. |
| `GamePadConfig`'s 22 missed lines are unbuilt layouts | Wrong | They were getters Kotlin generated for constants nobody outside the class read. Made private in #150. |
| `RetroViewUtils` is device-only | Wrong | `RetroView` is mocked in about 10 unit tests already. Its preserve logic is unit-testable. |
| `MenuSystem`'s 73 branches are guards | Mostly noise | They're in `MenuManager`: each of 6 methods builds a log message from four `(fragment as? Fragment)?.isX == true` checks. One helper removes most of them (item 8). |
| PiP's `getString(R.string.name)` breaks the `AppConfig` rule | Not a bug | `R.string.name` is generated from config `name` (`resValue`); 6 places read it the same way. |
| PiP Quick Save needs a seam | Right | Done in item 2. The seam also exposed the slot-1 overwrite (item 2b). |
| Key and motion routers | Right, incomplete | `interceptButtonB` exists twice (`KeyEventRouter`, `GamePadButtonRouter`) with slightly different non-DOWN handling. |

Missed by the first draft: `SaveStateManager` error paths (28 branches, the code that writes saves), `ManageSavesFragment` (54) and `ExitSaveGridFragment` (45) with dialog code copied across three fragments, `SubmenuCoordinator` (20), and about 30 unused methods.

## TODO (in order)

### [x] 1. `chore/remove-dead-code-2`: delete code nothing calls — PR #150
- Privacy placeholders, permission plumbing, `applyConditionalFeatures`, about 25 unused helpers, `GamePadConfig` constants made private.
- Coverage 87.4% / 68.6% → 88.1% / 69.6% with no new tests.
- Kept on purpose: getters that tests use to observe state, and public contracts (`AppConfig`, callback interfaces, ViewModel methods, `MenuFragmentBase`).

### [x] 2. `test/pip-quick-save`: PiP Quick Save and `PipController` — PR #151
- `PipQuickSaveExecutor` gets seams (background runner, slot store, PiP frame, frame timeout) with production defaults, and tests for every path: slot choice, slot name, screenshot, timeout abort, `serializeState()` failure, the task always finished once.
- `PipController` takes the executor as an optional constructor parameter. Tests for both PiP action buttons, the stuck-overlay cleanup, gamepad hide/restore, and entry failures.
- The impossible `SDK_INT < O` checks and `@TargetApi(O)` in `PipController` and `PipParamsFactory` are deleted.

### [x] 2b. `fix/pip-quick-save-slot`: where PiP Quick Save writes — PR #152
Today it writes to the slot last used this session, or **slot 1** when none was used, overwriting whatever is there. The rule the user chose:
1. A slot saved or loaded since this launch → that slot. ("This launch" only: the tracker stays in memory, Save and Exit is unchanged.)
2. Otherwise → the first empty slot.
3. All slots full → the slot with the oldest save date.
4. A slot with no readable date (missing or corrupt `metadata.json`): use its `state.bin` file date and write that date into `metadata.json`, creating the file with the name the menu already shows ("Slot N") if it's missing. A tie goes to the lower slot number.
- **Compatible with older versions:** no change to folders, file names, or `metadata.json` keys and date format. Older versions ignore keys they don't know, so a downgraded APK still reads every save.
- `PipLastSlotScreenshotResolver` (the PiP still-frame) no longer falls back to slot 1 either. It shows the most recent save (the slot used last this session, else the newest save), not the Quick Save target: with every slot full the target is the *oldest* save, which would be the wrong picture of the game.
- Both decisions live in `managers/QuickSaveSlotPicker` (pure); `SaveStateManager.backfillMissingTimestamps()` writes the missing dates.
- Also: record the save in `SessionSlotTracker` only when `saveToSlot` returns true (today a failed write is still recorded).
- Tests: saves written the way older versions wrote them (no date, no metadata, corrupt date, a migrated single save), and the full-slots case.

### [x] 3a. `fix/save-slot-error-paths`: Manage Saves on damaged slots — PR #153
Found while starting item 3, each confirmed with a test:
- Rename on a save with a corrupt `metadata.json` threw an uncaught `JSONException` (an app crash from Manage Saves). A save with no `metadata.json` couldn't be renamed at all.
- Copy/Move from a save with corrupt metadata failed after the target was already overwritten: Move left the save in both slots and reported an error.
- Delete ignored `deleteRecursively()`'s result and always reported success, so Move could too.

Fix: rename and copy read a damaged file the way the menu shows it ("Slot N") and write a repaired one. A failed copy removes its partial target. Delete returns the real result. When Move can't delete the source, it reports failure and the save stays in both slots, so nothing is lost. The unreachable `SecurityException`/`JSONException` catches are gone.

### [x] 3. `test/save-state-manager-errors`: `SaveStateManager` — this PR
- The remaining error paths: `saveToSlot` (preview image, write failure), `loadFromSlot` (empty slot, read failure), the slot-number checks, `backfillMissingTimestamps` write failure, `SlotMetadataStore` leftovers (blank date, legacy-migration failure). Use a temp folder.
- Delete the methods only tests call: `updateScreenshot`, `hasAnySave`, `getFirstEmptySlot`, `getOccupiedSlotCount`.
- Also removed: the legacy migration's unreachable `SecurityException` catch.
- Left untested on purpose: `copySlot` when the source slot folder is a plain file (`listFiles()` returns null), which the app never creates.

### [ ] 4. `test/button-routers`: `KeyEventRouter`, `MotionEventRouter`, `GamePadButtonRouter`
- `interceptButtonB` (both copies), `fireDpadCallback`, `fireTriggerCallback`, `checkSingleTrigger`, `computeDirectionTrigger`, `isAnyAxisOutOfDeadzone`.
- Deadzone edges exactly (just inside, on, just outside). Pin both B-button variants as they are.

### [ ] 5. `test/game-activity-viewmodel-remaining`: `GameActivityViewModel`
- `onMenuEvent`, `onBackToMainMenu`, `onAboutBackToMainMenu`, `preserveState`, `initializeControllers`, `setupRetroView`, `onCleared`, `setConfigOrientation`. Split in two if the diff gets large. New test file per topic.

### [ ] 6. `test/gamepad-and-retroview-utils`: `GamePad` and `RetroViewUtils`
- `GamePad`: `handleButtonEvent`, `handleDirectionEvent`, `eventHandler`, `hasExternalPhysicalController` (decides whether the on-screen pad shows).
- `RetroViewUtils.preserveEmulatorState` (never persists frame speed 0).

### [ ] 7a. `refactor/shared-slot-dialogs`: one dialog helper for the save grids
- `SaveSlotsFragment`, `ExitSaveGridFragment` and `ManageSavesFragment` copy the naming, overwrite and selection dialogs (`updateDialogSelection` and `performNavigateUp` are identical). Pin current behavior with tests first, then extract a shared helper and test it once. Rerun the Roborazzi goldens; they must not change.

### [ ] 7. `test/save-grid-fragments`: what's left per fragment
- The fragment-specific parts of the three grids, plus `CoreVariablesFragment`, `ExitFragment` (`performAutoSaveAndExit`, `performConfirm`) and `RetroKeyboard`.

### [ ] 8. `test/menu-navigation`: `MenuManager` and friends
- First replace `MenuManager`'s repeated log-message checks with one helper, then test the real guards (no fragment, not added, no context).
- `SubmenuCoordinator` (restoring the main-menu selection after back), `NavigationController`, `MenuOpenHandler`.

### [ ] 9. `test/small-utils`
- `FontUtils`, `OrientationManager`, `EventQueue.shouldDebounce`, `ProfilingSessionController`, `TypefaceProvider`, `MenuLayoutConfig`, `GamePadLayoutAdjuster`, `LogSaver`.
- Delete the remaining impossible pre-API-30 checks: `RotationController`, `ScreenshotPreviewController`, `ScreenshotCaptureUtil`, `CroppedScreenshotStore`.

### [ ] 10. `chore/raise-kover-floor-4`
- Run `./gradlew coverageAll -PskipAssetStaging`, raise `kover { verify { rule } }` in `app/build.gradle` to the new values rounded down, update `CLAUDE.md`, fill in the Result section.

**Done when** items 1–10 are merged, `check` passes with the raised floor, and every file still below 80% is either in "Won't do" with its reason or device-only.

## Open decisions (not scheduled)

- **The temp state is written but never read.** Since `aabe1a4` (2025-10-03) the game writes a temp state each time the menu opens or it pauses, but `RetroViewUtils.restoreEmulatorState`, its only reader, has no caller. Either wire the restore back in (progress survives Android killing the app) or stop writing the temp state. Its own PR either way.
- **Unused public contracts.** Some `AppConfig`, callback-interface and ViewModel methods have no caller. They were kept in #150 because of the contract rule in `CLAUDE.md`.

## Won't do

- **Device items** (more instrumented coverage of `GameActivity`, `RetroView`, `CRTBootView`; the key-up check from `2026-09-27-keyup-input-side-effects.md`): skipped by the user's decision. They don't affect the enforced floor.
- **`RetroEditText.onDraw` and `CRTBootView`'s draw methods.** Canvas drawing, covered visually by the Roborazzi screenshots.
- **Bytecode-only branches** (null-safety and `when` checks no input can reach). Don't write tests to satisfy the counter.
- **About 150 lines and 220 branches spread over small files** with fewer than 8 missed lines each, unless an item above touches them anyway.
- **Python's 14 missed lines** (98.1%; no enforced floor).
- **Lowering the Kover floor**, ever.

## Result

*(fill in when item 10 is done)*
