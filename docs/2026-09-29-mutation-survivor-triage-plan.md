# Mutation survivors triage plan — 2026-09-29

Source: `python3 tools/mutation/mutation_testing.py --all --report-dir ~/revenger-mutation-report` on `develop` @ `a5f836c` (after PR #186). Started 2026-09-28 20:50, finished 2026-09-29 about 08:30 (about 11.7 h, 3 jobs, `--gradle-heap 3g`). The raw results are outside the repo in `~/revenger-mutation-report/`:
- `mutants.jsonl`: one line per mutant;
- `summary.md`: score per file and every survivor;
- `run.log`.

The line numbers below and in the report are for `a5f836c`.

## Baseline

| | |
|---|---:|
| Mutants | 2,029 |
| Killed | 1,417 (1,371 by the class's own tests, 46 by the tests of classes that use it) |
| **Survived** | **492** |
| Compile errors (not counted) | 120 |
| Timeouts (counted as killed) | 1 |
| **Score** | **74.2%** |

Survivors by kind of change:

| Change | Survivors |
|---|---:|
| remove call | 339 |
| `false -> true` | 61 |
| `true -> false` | 32 |
| `&&` / `\|\|` swapped | 29 |
| `==` / `!=` swapped | 14 |
| `+ 1` / `- 1` swapped | 6 |
| comparisons flipped | 11 |

The 60 swaps and comparison flips are the most likely to hide real logic gaps; look at those first in every PR. Many "remove call" survivors are UI calls (text, visibility, animation, focus) and some will be equivalent.

## How to work through this plan

- **One branch and one PR per item below**, each branched from the latest `develop`, opened against `develop`. **After opening a PR, stop and wait for the user to merge it** before starting the next one.
- For each survivor in the item, read the code around the line and put it in one of two buckets:
  - **Missing assertion:** a real bug on that line would go unnoticed. Write a test that fails with the mutant and passes without it.
  - **Equivalent / not worth a test:** the change can't alter behaviour, or only alters something a unit test can't see (a synchronous vs asynchronous prefs write, pixel output of an animation frame, a log). List it in the PR description with a one-line reason, as #182 did.
- **Dead code gets deleted, not tested.** If a survivor sits in code nothing calls, delete it (rename-and-compile check: `./gradlew compileDebugKotlin -PskipAssetStaging`).
- **When a test finds a real bug:** stop, report it, and fix it in its own PR with a regression test, unless the user says to fold it in.
- **Check that the new tests kill the mutants.** Apply the mutant by hand and run the test, or rerun the script on the item's files with a separate report dir, then compare with the baseline:
  ```bash
  python3 tools/mutation/mutation_testing.py app/src/main/java/.../File.kt --report-dir ~/revenger-mutation-report/pr-N
  ```
  Put the per-file before/after score in the PR.
- Before each PR, run `./gradlew check -PskipAssetStaging`. Run `claude-usage` between items. Never name games, platforms or brands anywhere.
- Don't run Gradle in the checkout while a mutation run is going (the 4th daemon plus test JVM makes the machine swap).
- Mark each item `[x]` here with its PR number when it's done.

## Items

Paths are under `app/src/main/java/com/vinaooo/revenger/`. Columns: survived / killed.

### [x] 1a. Utils, repositories, app config — 70 survivors (#188: 14 left, 13 equivalent + the DebugGate bug)

| File | S | K |
|---|---:|---:|
| `utils/LogSaver.kt` | 17 | 9 |
| `utils/ScreenshotCaptureUtil.kt` | 11 | 20 |
| `utils/CroppedScreenshotStore.kt` | 8 | 4 |
| `AppConfig.kt` | 7 | 19 |
| `utils/ScreenshotGeometry.kt` | 4 | 8 |
| `repositories/Storage.kt` | 4 | 3 |
| `repositories/DefaultSettingsRepository.kt` | 3 | 4 |
| `utils/MenuLogger.kt` | 3 | 0 |
| `RevengerApplication.kt` | 2 | 0 |
| `repositories/PipConfigRepository.kt` | 2 | 3 |
| `repositories/SharedPreferencesRepository.kt` | 2 | 6 |
| `utils/PipFrameStore.kt` | 2 | 4 |
| `ConfigSources.kt` | 1 | 0 |
| `utils/DebugGate.kt`, `utils/OrientationManager.kt`, `utils/PipAspectRatioResolver.kt`, `utils/ViewUtils.kt` | 1 each | |

First look: `RevengerApplication.onCreate` initialization isn't checked by any test. `ConfigSources.kt:92` (`lastDot >= 0`) has no test for a name whose only dot is at index 0. The 7 `AppConfig` survivors are the default `false` of `fake_button_*` fields; check whether the defaults can ever be seen.

### [x] 1b. Input, gamepad, managers, performance — 49 survivors (#190: 18 left, all equivalent)

| File | S | K |
|---|---:|---:|
| `gamepad/GamePadLayoutAdjuster.kt` | 7 | 15 |
| `input/KeyEventRouter.kt` | 7 | 35 |
| `performance/DebugOverlayController.kt` | 7 | 7 |
| `input/GamePadButtonRouter.kt` | 6 | 34 |
| `gamepad/GamePad.kt` | 3 | 18 |
| `input/ComboKeyLogTracker.kt` | 3 | 29 |
| `managers/GameLifecycleObserver.kt` | 3 | 14 |
| `managers/SaveStateManager.kt` | 3 | 26 |
| `input/ControllerInput.kt` | 2 | 4 |
| `input/ControllerInputCallbacks.kt` | 2 | 5 |
| `managers/SlotMetadataStore.kt` | 2 | 18 |
| `input/MotionEventRouter.kt`, `managers/SlotFileLayout.kt`, `performance/ProfilingSessionController.kt`, `performance/HardwareMetricsCollector.kt` | 1 each | |

`SaveStateManager` and `SlotMetadataStore` survivors were partly reviewed in #182 (the `mkdirs()`/`overwrite` ones are equivalent); don't redo those.

### [x] 2. PiP and rotation controllers — 34 survivors (#191: 4 left, all equivalent)

| File | S | K |
|---|---:|---:|
| `controllers/PipController.kt` | 18 | 40 |
| `controllers/PipParamsFactory.kt` | 4 | 1 |
| `controllers/MenuRotationRecreator.kt` | 3 | 15 |
| `controllers/RotationController.kt` | 3 | 12 |
| `controllers/SpeedController.kt` | 3 | 5 |
| `controllers/AudioController.kt`, `controllers/PipQuickSaveExecutor.kt`, `controllers/ShaderController.kt` | 1 each | |

First look: the forced frame capture on entering PiP (`PipController.kt:136, 156`), the overlay snapshot (`:157`) and the params refresh (`:118`) can all be removed unnoticed, and so can the menu reference update and first-item focus after rotation (`MenuRotationRecreator.kt:207, 225, 254`). `AudioController.kt:45` (`commit = true`) is likely equivalent under Robolectric.

### [x] 3. Menu infrastructure — 88 survivors (#PRNUM: 14 left, all equivalent)

| File | S | K |
|---|---:|---:|
| `ui/retromenu3/RetroKeyboard.kt` | 15 | 34 |
| `ui/retromenu3/RetroEditText.kt` | 14 | 17 |
| `ui/retromenu3/SubmenuCoordinator.kt` | 8 | 22 |
| `ui/retromenu3/navigation/NavigationEventProcessor.kt` | 8 | 62 |
| `ui/retromenu3/SlotDialogController.kt` | 7 | 25 |
| `ui/retromenu3/MenuConfiguration.kt` | 6 | 0 |
| `ui/retromenu3/SlotDialogViews.kt` | 6 | 6 |
| `ui/retromenu3/navigation/NavigationController.kt` | 5 | 11 |
| `ui/retromenu3/GlowAnimationController.kt` | 4 | 9 |
| `ui/retromenu3/navigation/KeyboardInputAdapter.kt` | 4 | 23 |
| `ui/retromenu3/RetroCardView.kt` | 3 | 10 |
| `ui/retromenu3/config/MenuLayoutConfig.kt` | 3 | 19 |
| `ui/retromenu3/MenuSystem.kt` | 2 | 8 |
| `ui/retromenu3/MenuViewManager.kt` | 2 | 11 |
| `ui/retromenu3/navigation/EventQueue.kt` | 1 | 9 |

`MenuConfiguration.kt` has no killed mutant at all; check first whether it is used. #182 already reviewed some `NavigationEventProcessor` and `EventQueue` survivors (`eventQueue.clear()` in the close paths is equivalent).

### [ ] 4a. Save menus — 57 survivors

| File | S | K |
|---|---:|---:|
| `ui/retromenu3/SaveStateGridFragment.kt` | 34 | 30 |
| `ui/retromenu3/ManageSavesFragment.kt` | 12 | 18 |
| `ui/retromenu3/ExitSaveGridFragment.kt` | 7 | 21 |
| `ui/retromenu3/SaveSlotsFragment.kt` | 4 | 9 |

### [ ] 4b. Other menu fragments — 97 survivors

| File | S | K |
|---|---:|---:|
| `ui/retromenu3/AboutFragment.kt` | 19 | 13 |
| `ui/retromenu3/CoreVariablesFragment.kt` | 15 | 23 |
| `ui/retromenu3/ProgressFragment.kt` | 15 | 25 |
| `ui/retromenu3/SettingsMenuFragment.kt` | 15 | 23 |
| `ui/retromenu3/ExitFragment.kt` | 13 | 21 |
| `ui/retromenu3/RetroMenu3Fragment.kt` | 13 | 35 |
| `ui/retromenu3/MenuLifecycleManager.kt` | 3 | 12 |
| `ui/retromenu3/MenuFragmentBase.kt` | 2 | 16 |
| `ui/retromenu3/GridSelectionState.kt`, `ui/retromenu3/MenuStateController.kt` | 1 each | |

Big item; split it in two PRs if the triage shows many real gaps. A UI change that alters pixels must also update the Roborazzi goldens (`recordRoborazziDebug`).

### [ ] 5a. Game screen and view models — 57 survivors

| File | S | K |
|---|---:|---:|
| `views/GameActivity.kt` | 32 | 28 |
| `viewmodels/menu/SubmenuFragmentDismisser.kt` | 5 | 9 |
| `viewmodels/AudioViewModel.kt` | 4 | 2 |
| `viewmodels/ShaderViewModel.kt` | 4 | 1 |
| `viewmodels/GameActivityViewModel.kt` | 3 | 20 |
| `viewmodels/SpeedViewModel.kt` | 3 | 5 |
| `viewmodels/menu/SubmenuFragmentRegistrar.kt` | 2 | 7 |
| `viewmodels/InputViewModel.kt`, `viewmodels/MenuViewModel.kt`, `viewmodels/menu/MenuCloseHandler.kt`, `viewmodels/menu/SaveLoadCentralizedController.kt` | 1 each | |

`InputViewModel.kt:30` (`setupControllerInputCallbacks()`) was already seen surviving in the calibration run.

### [ ] 5b. Splash — 40 survivors

| File | S | K |
|---|---:|---:|
| `ui/splash/CRTBootView.kt` | 30 | 10 |
| `views/SplashActivity.kt` | 10 | 5 |

Mostly drawing and animation calls; expect many equivalents. Test what decides behaviour (timing, when the next screen starts, skip on input), not pixels.

## After the last item

Rerun the baseline on the latest `develop`, with a fresh report dir so this one stays for comparison, and add the new score here:
```bash
python3 tools/mutation/mutation_testing.py --all --report-dir ~/revenger-mutation-report-after
```
