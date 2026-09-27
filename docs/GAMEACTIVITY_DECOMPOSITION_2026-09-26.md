# Next big job: shrink and test the emulator screen — 2026-09-26

Source: a read-only survey of `views/GameActivity.kt` and the startup screens, made after the coverage follow-ups (`docs/COVERAGE_FOLLOWUPS_2026-09-26.md`, PRs #128–#134) were done.

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- Every change includes tests (`CLAUDE.md`). A bug fix gets a regression test, and a mutation check shows the test fails without the fix.
- **Never name games, consoles, cores or brands** anywhere: code, comments, test data, commits, PRs, this doc. The Gradle build log prints the ROM file name; never copy it.
- Before each PR:
  - run `./gradlew check -PskipAssetStaging`
  - run `./gradlew coverageAll -PskipAssetStaging` and put the before/after numbers in the PR
- Run `claude-usage` between items.
- **Ask the user before any on-device run.** For the AVD:
  - Start it with `emulator -avd Pixel_9a_Android_16 -skin 1080x2424 -no-snapshot-save -no-audio -no-boot-anim`. The AVD's `pixel_9a` skin is missing from the SDK, and without `-skin` it fails with "unknown skin name".
  - Instrumented coverage: `./gradlew createDebugAndroidTestCoverageReport -PinstrumentedCoverage` (a full build, without `-PskipAssetStaging`).

## Baseline (develop after #133; #134 raises the floor to 84/66)

| Code | Lines | Branches |
|---|---|---|
| Kotlin, unit tests | 84.9% (8200/9656) | 66.8% (2349/3519) |
| Kotlin, instrumented (reporting only) | 30.5% | 12.0% |
| All code (Kotlin + Python + shell) | 85.9% | 68.4% |

| File | Size | Unit | Instrumented |
|---|---|---|---|
| `views/GameActivity` | 648 lines | 0% | 63% |
| `views/SplashActivity` | 148 lines | 0% | 0% |
| `ui/splash/CRTBootView` | 417 lines | 0% | 9% |

## What `GameActivity` still does (survey)

PiP (`PipController` + `PipHost`), rotation (`RotationController`), the floating button (`FloatingMenuButtonController`) and save/load (`SaveLoadOrchestrator`) are already out. What is left inline:

1. **`onCreate`, in four steps** (`initializeCoreServices`, `initializeViewsControllersAndInput`, `setupRetroViewAndObservers`, `finishGamePadAndMenuSetup`):
   - audio focus
   - orientation
   - screenshot context
   - SDK-36 features: privacy, profiler, debug overlay
   - system bars
   - view lookups
   - load-preview overlay callback
   - gamepad alignment validation
   - immersive insets listener
   - RetroView and lifecycle observer
   - gamepads
   - menu wiring
   - Every step logs timing with `Log.e("STARTUP_TIMING", …)`: error level for non-errors.
2. **Input-device listener** (`registerInputListener`):
   - three identical callbacks, all calling `viewModel.updateGamePadVisibility(...)`
   - **never unregistered** (see item 1)
3. **System back** (`OnBackPressedCallback`), a three-way decision:
   - menu open → `NavigateBack(SYSTEM_BACK)`
   - `shouldHandleBackButton()` → `OpenMenu(SYSTEM_BACK)`
   - otherwise → the default back (the callback disables itself)
4. **System bars appearance** (`configureSystemBarsForTheme`): light icons in the dark theme.
5. **Key and motion routing** (`onKeyDown`, `onKeyUp`, `onGenericMotionEvent`, `dispatchTouchEvent`): frame-time recording, button fade, PiP frame capture, then `viewModel.process*Event`.
6. **Frame-time recording** (`recordFrameTime`): the nanoTime delta goes to the profiler.
7. **Permission result mapping** (`permissionLauncher`): map of granted flags → an `IntArray` of `PERMISSION_GRANTED` / `PERMISSION_DENIED`.
8. **Teardown** (`onDestroy`): an ordered list of disposes.
9. **The `PipHost` implementation**: thin by design, leave it.
10. **The shutdown animation** (`startShutdownAnimation`): CRT reverse animation plus a callback.

**Why unit tests can't just launch `GameActivity` under Robolectric:** `setupRetroView` loads the native core through `GLRetroView`. So follow the PiP and rotation pattern: move logic into small classes behind narrow interfaces, test those, and keep the Activity as wiring. The on-device tests (`EmulatorSmokeTest`, `GameActivityCleanupIntegrationTest`, `RotationMenuIntegrationTest`) keep covering the real wiring.

## TODO (recommended order)

### [x] 1. `fix/input-device-listener-leak`: extract the input-device watcher and unregister it
- **Bug to confirm first:**
  - `registerInputListener` registers an anonymous `InputManager.InputDeviceListener` that captures the Activity, and `onDestroy` never unregisters it.
  - `InputManager` is process-wide, so every destroyed `GameActivity` (every recreation) stays reachable and keeps receiving device events.
  - Write the regression test first and watch it fail.
- **New `controllers/InputDeviceWatcher`:**
  - `register()` / `dispose()` around `registerInputDeviceListener` / `unregisterInputDeviceListener`
  - one `onDevicesChanged: () -> Unit` callback for all three events (replacing the three copies)
- **`GameActivity`:** create the watcher in `initializeViewsControllersAndInput` and dispose of it in `onDestroy`, next to `rotationController.dispose()`.
- **Tests (Robolectric):**
  - add, remove and change events each call the callback once
  - `dispose()` unregisters the same listener that was registered (verify on a mocked or shadow `InputManager`)
  - `dispose()` twice is safe
  - the Activity-level regression: after `onDestroy`, no listener is left registered. Use `ShadowInputManager` if it exposes listeners; otherwise verify through the watcher's host seam.

### [x] 2. `refactor/system-back-router`: extract the system back decision
- **New pure `controllers/SystemBackRouter`:**
  - `decide(isMenuActive: Boolean, shouldHandleBack: Boolean): SystemBackAction` returns `NAVIGATE_BACK`, `OPEN_MENU` or `DEFAULT`
  - a small executor sends the matching `NavigationEvent` with `InputSource.SYSTEM_BACK` (and `KEYCODE_BACK` for `NavigateBack`)
- The `OnBackPressedCallback` in `GameActivity` becomes a three-line delegation.
- **Tests:**
  - the decision table, all four input combinations (menu open wins over `shouldHandleBack`)
  - the events sent, including `inputSource` and `keyCode`
  - the `DEFAULT` path disables the callback and re-dispatches
- **Check while doing it:** after the `DEFAULT` path, the callback stays disabled for the rest of the Activity's life. Confirm whether that path always finishes the Activity. If it doesn't, a later back press skips the menu routing. That would be a bug: fix it with a test, or document why it's fine.

### [x] 3. `refactor/gameactivity-small-pure-pieces`: the pure bits of `onCreate` and input
- **`SystemBarsAppearance`:** `forUiMode(uiMode): Pair<statusMask, navMask>` (light icons iff night mode). The Activity applies it.
- **`FrameTimeRecorder`:** holds the last timestamp; `record(nowNs)` returns the delta, or null for the first frame; `reset(nowNs)` for `onResume`. The Activity forwards the delta to the profiler.
- **`PermissionResults.toGrantResults(map)`:** all granted → every entry `PERMISSION_GRANTED`, any denied → every entry `PERMISSION_DENIED`. Pin this current all-or-nothing behavior in a test and ask the user whether per-permission results were intended.
- **`LoadPreviewOverlayBinder`:** a bitmap shows the overlay; null hides it and clears the drawable. Robolectric with a real `ImageView`.
- **Startup timing:** replace the repeated `Log.e("STARTUP_TIMING", …)` blocks with a tiny `StartupTimer(startTime, clock)` whose `mark(step)` logs at debug level. Test the message format and the elapsed time with a fake clock.
- Tests for each piece. Keep `onCreate`'s call order unchanged; the on-device tests guard it.

### [x] 4. `refactor/gameactivity-input-routing`: key and motion handling
- **New `controllers/GameInputRouter`** with a narrow host: `recordFrame()`, `triggerButtonFade()`, `capturePipFrame()`, `processKey(keyCode, event)`, `processMotion(event)`, super fallbacks.
- It covers `onKeyDown` (record, fade, capture, then the ViewModel or super), `onKeyUp` (ViewModel or super), `onGenericMotionEvent` (like `onKeyDown`) and `dispatchTouchEvent` (capture only).
- **Tests:**
  - call order per event type
  - the ViewModel's result wins, and `null` falls back to super
  - `onKeyUp` doesn't record, fade or capture (pin the current behavior; ask if that asymmetry is intended)

### [x] 5. `refactor/gameactivity-teardown`: the `onDestroy` sequence
- Move the ordered disposes into `GameSessionTeardown.run(parts)`, where `parts` are the controllers, profiler and overlay, ViewModel, screenshot PiP frame and audio focus behind small interfaces.
- **Tests:**
  - the exact order, verified with `verifyOrder`
  - audio focus is abandoned only when the manager was created
  - one failing dispose doesn't skip the rest. Today it would. Decide with the user; if you change it, it's a behavior change and needs a test.
- `GameActivityCleanupIntegrationTest` (on device) already checks cleanup end to end. Keep it passing.

### [x] 6. `test/splash-and-crt-boot`: the startup screens
- **`SplashActivity`** (0% everywhere): Robolectric test that it starts `GameActivity` (check the next-started Intent) and finishes. Read it first; it may need a seam around the animation-end callback.
- **`CRTBootView`** (0% unit, 9% instrumented): extract the animation timeline, meaning the phase and parameters at time `t` for boot and for reverse (shutdown), into a pure class and test it: phase boundaries, the reverse order, the end callback firing once. Keep the drawing in the view.
- Also test `GameActivity.startShutdownAnimation` through a seam (the view is made visible, the listener set, the reverse animation started, `onComplete` called once).

### [x] 7. `test/brand-name-guard`: enforce the naming rule in Kotlin sources
- **Known violations today:**
  - `utils/ConfigIdGenerator.kt` KDoc: its example uses a real game title and core id
  - `repositories/DefaultSettingsRepository.kt` KDoc: its example uses real platform ids
  - Sweep for more.
- **Guard test (JVM unit test)** that scans `app/src/main/java`, `tests/` and `app/src/androidTest/`:
  - **The test must not contain the names itself.** Build the denylist at runtime from `icons/platforms.json` (platform, core and extension ids), `app/src/main/assets/default_settings.json` (profile ids) and `config.json` (`name`, `rom`, and `core` when present). This follows `icons/tests/test_platforms.py`.
  - **Match whole words, case-insensitive.** A naive substring scan flags ordinary words: a Portuguese test name containing "primario" hits one title. Allow very short ids only as whole tokens, and keep a small, commented allowlist for real false positives.
  - Exclude the config assets themselves, which are where the values are allowed.
- Fix every hit with neutral wording ("the game", "a platform id", "the core").

### [x] 8. `docs/stale-material-comments`: comments that contradict the no-Material rule
- **Comments** claiming "Material Design 3" or "Material You" or "handled by Material 3 theme inheritance":
  - `RevengerApplication.kt`
  - `GameActivity.kt` (`initializeSdk36Features`)
  - `AboutFragment.kt`, `SettingsMenuFragment.kt`, `ExitFragment.kt`
  - `RetroViewUtils.kt` (the "Required for Material You menu…" KDocs)
  - `AndroidCompatibility.kt`
- Material is deliberately not used, and the dependency is commented out in `app/build.gradle`. Reword these comments to describe what the code does. Delete any function whose only stated purpose was a Material You menu and that nothing calls; check the callers first.
- **Test:** extend the guard from item 7, or add a small one, so `com.google.android.material` never appears in main sources or Gradle dependencies (the rule in `CLAUDE.md`).

### [x] 9. `test/device-menu-rotation-e2e`: on-device menu flows (ask before running)
- Read `RotationMenuIntegrationTest` first; it already exists. Extend it rather than duplicating.
- **Flows:**
  - rotate while a submenu (Settings, then Progress) is open: the same submenu comes back on top of the main menu, with a working back
  - save to a slot from the menu, then load it: a state round trip through the UI
  - system back with the menu closed opens it; with it open, it goes back one level
- **Why on a device:** the rotation rebuild depends on fixed delays of 250, 100, 150 and 600 ms. Robolectric's clock can't observe them faithfully, as seen in PR #131.
- Rerun the instrumented coverage and put the new numbers in the PR.

### [x] 10. `chore/raise-kover-floor-3`
- After items 1–7, raise the floor to about 1 point below the new unit-test numbers.
- Update the comment in `app/build.gradle` and the floor line in `CLAUDE.md`.
- Include the negative check (the floor + 1 must fail).

## Result (2026-09-27)

All ten items are merged (#135–#142, #144, and this floor raise), plus #143, a menu bug the item 9 device tests found. Kotlin unit-test coverage went from 84.9% lines / 66.8% branches to 87.4% / 68.5%; instrumented coverage from 30.5% / 12.0% to 42.7% / 20.4%. The Kover floor is now 87% lines / 68% branches.

## Notes
- Items 1 and 2 are the most valuable: a probable leak, and the back behavior players hit constantly. Items 7–8 are small and can go anywhere in the order.
- Keep `GameActivity`'s public API unchanged (`requestPermissionsModern`, `startShutdownAnimation`, the `PipHost` / `FloatingButtonVisibilityHost` implementations). Other code and the on-device tests use it.
- Update `CLAUDE.md`'s runtime-architecture section when new controllers land, as was done for `PipController` and `RotationController`.
