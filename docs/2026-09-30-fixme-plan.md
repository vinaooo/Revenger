# Open FIXMEs in `TODO.kt` — 2026-09-30

`TODO.kt` lists five FIXMEs. Each one was checked against `develop` (fafe737) on 2026-09-30. Four were still real bugs. #1 had already been fixed by #181 on 2026-09-28, and only its FIXME was left behind. The FIXME text in `TODO.kt` is in Portuguese; the summaries below are in English.

The remaining `TODO` entries in `TODO.kt` are feature ideas and are out of scope here.

## How to work through them

- One branch and one PR per item, branched from the latest `develop` and opened against `develop`. Wait for each merge before starting the next.
- Each fix needs a regression test that fails without the fix.
- Delete the item's FIXME from `TODO.kt` in the same PR, and mark the item `[x]` here with the PR number.
- Run `./gradlew check -PskipAssetStaging`, and `python3 tools/mutation/mutation_testing.py --changed` before opening the PR.
- Ask before any on-device check.

## Status

| # | FIXME (`TODO.kt` line) | Area | Reachable by users | Order |
|---|---|---|---|---|
| 1 | ~~19~~ (removed): Back/Escape can fire twice on a long press | Keyboard input | Only on a hold past 200 ms | 5th, already fixed by #181; FIXME removed in #205 |
| 2 | ~~24~~ (removed): menus clear pending input on an orphan `ControllerInput` | Input / ViewModels | Yes | 4th, done in #204 |
| 3 | ~~30~~ (removed): each rotation rebuilds the menu twice | Rotation | Yes (intermittent) | 3rd, done in #203 |
| 4 | ~~34~~ (removed): rotating with Core Variables open falls back to the main menu | Rotation | Yes | 1st, done in #201 |
| 5 | ~~34~~ (removed): save submenus aren't re-registered or refocused after a rotation | Rotation | Yes | 2nd, done in #202 |

## Items, in the order to do them

### [x] 4. Rotating with Core Variables open falls back to the main menu (#201)

- **What it really did:** the menu manager still reports About while Core Variables is open (Core Variables opens through the NavigationController, not the menu manager), so rotating rebuilt About, not the main menu. The navigation stack kept its extra About level, so the next Back showed the main menu while navigation still thought About was open. #201 rebuilds main menu → About → Core Variables, keeps the menu manager on About, registers that About, and restores focus to Core Variables' Back item.
- **FIXME:** `views/menu/RotationMenuStateResolver` has no mapping for `CoreVariablesFragment`. Rotating with that submenu open lands on the main menu instead of rebuilding it.
- **How users hit it:** About → Core Variables (`AboutFragment` navigates to `MenuType.CORE_VARIABLES`), then rotate the device.
- **Still true:** `RotationMenuStateResolver.kt:69` documents that `MenuState.CORE_VARIABLES_MENU` has no registration path and falls back to `MenuType.MAIN`.
- **Why first:** small and self-contained, and needs no device.

### [x] 5. Save submenus aren't re-registered or refocused after a rotation (#202)

- **What it really did:** the save grids are never registered with the ViewModel, not even without a rotation, so no registration was missing. They are nested submenus like Core Variables: Save, Load and Manage open from Progress, and the exit save grid from Exit, through the NavigationController, so the menu manager stays on the parent and two submenus are on the backstack. The rotation rebuilt only main menu → grid, and moved the menu manager to the grid's state, so the next Back showed the main menu while navigation still thought Progress (or Exit) was open. #202 rebuilds main menu → parent → grid, keeps the menu manager on the parent, registers that parent, and focuses the grid. The PiP "Save and Exit" grid, which opens on its own with one backstack entry, is still rebuilt without an Exit menu under it.

- **FIXME:** after a rotation only Settings, Progress, About and Exit are registered with the ViewModel again and get focus back. SaveSlots, LoadSlots, ManageSaves and ExitSaveGrid get neither.
- **Location is out of date in `TODO.kt`:** the FIXME points to `GameActivity.kt` (`createFragmentForRotationState` / `registerSubmenuAndSyncNavigationAfterRotation`). That code has since moved:
  - `controllers/MenuRotationRecreator.kt`, `registerSubmenuAndSyncNavigationAfterRotation`: its `when` handles only `SETTINGS_MENU`, `PROGRESS_MENU`, `ABOUT_MENU` and `EXIT_MENU`. Every other state hits the `else` branch, which logs "Unknown state, submenu not registered".
  - `controllers/MenuRotationRecreator.kt`, `restoreSubmenuFocusAfterRotation`: the same four states map to a view id, and the rest get `null`, so focus isn't restored.
  - `views/menu/RotationFragmentFactory.kt` already builds `SaveSlotsFragment`, `ManageSavesFragment` and the others. Only the registration and focus steps are missing.
- **Why second:** it's in the same code as item 4, and the fix should also correct the location in the FIXME text.

### [x] 3. Each rotation rebuilds the menu twice (#203)

- **What #203 changed:** `RotationController` now remembers the orientation the menu was laid out for, and rebuilds the menu only when `onConfigurationChanged` brings a different one. Every other callback still re-registers the menu callbacks but starts no rebuild chain. That covers a second callback for the same rotation, and the uiMode, screen size and screen layout changes the activity also handles itself. `layout-land` is the only configuration qualifier the menus use.
- **Cause not confirmed on a device:** setting `requestedOrientation` to the value it already has doesn't normally cause a configuration change, so the FIXME's explanation may be wrong. The guard works whatever sends the extra callback. Counting the `CHECKING FOR MENU AFTER ROTATION` log lines during one rotation would settle it.

- **FIXME:** `GameActivity.onConfigurationChanged` runs the whole menu-rebuild chain twice per rotation. `reapplyOrientation()` sets `requestedOrientation` again, which causes a second `onConfigurationChanged`. The two chains (about 1,100 ms each) end up in the right state only by lucky timing.
- **Risk:** the highest of the five. It probably causes intermittent glitches after a rotation, and a fix touches the rotation flow that items 4 and 5 depend on.
- **Why third:** do it after 4 and 5, so those fixes and their tests are in place to catch regressions.

### [x] 2. Menus clear pending input on an orphan `ControllerInput` (#204)

- **What #204 did:** removed the dead clear instead of pointing it at the real `ControllerInput`. On the real instance, `clearPendingInputsPreserveHeld()` also resets `MenuCallbackDebouncer`'s post-close grace period. That period stops the button that closed the menu from reaching the game when it is released, and a closing menu pauses right after the period starts. So re-pointing the clear would probably have added a leak to fix one nobody had seen. `RetroMenu3ToggleController.clearControllerInputState()` cleared the same orphan and had no callers, so it went too, along with `InputViewModel`, which had no other users. `tests/MenuFragmentPause_test.kt` pins that pausing a menu keeps the real grace period. Nothing changes on a device. Whether B/Backspace really leaks between submenus would need a device check.

- **FIXME:** `GameActivityViewModel` builds an `InputViewModel` that has its own `ControllerInput`, so `InputViewModel.getControllerInput()` is never the instance that handles real input. `MenuFragmentBase.onPause()` calls `inputViewModel.getControllerInput().clearPendingInputsPreserveHeld()` on the wrong instance. That leaves the fix meant to stop B/Backspace leaking between submenu transitions doing nothing.
- **How users hit it:** a stray "back" after moving between submenus.
- **Note:** `tests/SubmenuFragmentSetup_test.kt` (`pausar limpa as entradas pendentes do controle`) checks the clear against the `InputViewModel`'s instance. A fix that switches to the real instance must update that test.

### [x] 1. Back/Escape can fire twice on a long press (#181, FIXME removed in #205)

- **Already fixed before this plan:** #181 (2026-09-28) added `KEYCODE_BACK` and `KEYCODE_ESCAPE` to `KEY_UP_TRACKED_ACTION_KEYS`, so their `KEY_DOWN` is recorded like Backspace's and the `KEY_UP` only confirms it. Its regression tests are in `tests/KeyboardInputAdapter_test.kt` (`Back segurado alem do debounce gera um unico NavigateBack`, `Escape gera um unico CloseAllMenus por toque`, and the no-`KEY_DOWN` fallback test). #181 didn't delete the FIXME, and this plan's check missed the fix. #205 removes the FIXME only; there's no code change.

- **FIXME:** in `ui/retromenu3/navigation/KeyboardInputAdapter.kt`, only `KEYCODE_DEL` records `actionKeyDownTimestamps` on `KEY_DOWN`. `KEYCODE_BACK` and `KEYCODE_ESCAPE` therefore fall through to the `KEY_UP` fallback and fire `NavigateBack` / `CloseAllMenus` a second time.
- **Mitigation today:** the 200 ms debounce (`MENU_CLOSE_DEBOUNCE_MS` in `input/ControllerInput.kt`) hides it for normal taps. Only a key held longer than that fires twice.
- **Why last:** the lowest impact of the five.

## Related

- `docs/2026-09-27-keyup-input-side-effects.md` is a separate open question: whether key-up should run the fade, frame-timing and PiP-capture side jobs. It needs a device check before any change.
