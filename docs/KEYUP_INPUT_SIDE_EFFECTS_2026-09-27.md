# Key-up and the input side jobs (fade, timing, PiP capture) — 2026-09-27

Status: **open, not scheduled.** This records a question left open by PR #138 (the `GameInputRouter` extraction) so it can be decided and fixed later. #138 moved the input code without changing what it does, including the gap described here.

## The question

Should a key-up event run the same three side jobs as key-down and generic motion (frame timing, floating button fade, PiP still-frame capture), or keep skipping them?

## Current behavior

`controllers/GameInputRouter.kt`, called from `GameActivity`'s input overrides:

| Event | Record frame time | Fade the button | Capture PiP frame | Then |
|---|---|---|---|---|
| `onKeyDown` | yes | yes | yes | ViewModel `processKey`, or `super` if it returns null |
| `onGenericMotionEvent` | yes | yes | yes | ViewModel `processMotion`, or `super` if null |
| `onKeyUp` | **no** | **no** | **no** | ViewModel `processKey`, or `super` if null |
| `dispatchTouchEvent` | no | no | yes | `super` only (never reaches the ViewModel) |

The three jobs run in that order, before the ViewModel is asked. `tests/GameInputRouter_test.kt` pins the table above, including `key up only asks the view model` (a `verifySequence` that allows only `processKey`).

## What each side job does, and what key-up would change

### 1. Frame timing

- **Path:** `GameInputHost.recordFrame()` → `GameActivity.recordFrameTime()` → `FrameTimeRecorder.record(System.nanoTime())` → `AdvancedPerformanceProfiler.recordFrameTime(...)`. `FrameTimeRecorder` returns null for the first sample and is reset in `onResume`.
- **What it really measures:** the interval between two input events, not render frame time. The name is historical.
- **If key-up also records:** each press adds two samples, and half of them measure how long the button was held. That skews the profiler's numbers without making them more meaningful.
- **Verdict:** leave key-up out. If this metric matters, the real fix is to feed the profiler from the render loop, which is a separate change.

### 2. Floating menu button fade

- **Path:** `GameInputHost.triggerButtonFade()` → `FloatingMenuButtonController.triggerFade()`.
- It does nothing while a menu is open or when the button isn't `VISIBLE`. The button shows only when the on-screen gamepad is hidden (`GamePad.shouldShowGamePads(...)` is false), so this only affects play with a physical controller.
- Otherwise it animates alpha to `DIMMED_ALPHA` (0.3) over `FADE_TRANSITION_DURATION_MS` (200 ms), cancels the pending restore runnable, and reposts it for `INACTIVITY_RESTORE_DELAY_MS` (10 s). When that runs, it animates back to `VISIBLE_ALPHA` (1.0) over `RESTORE_ANIMATION_DURATION_MS` (500 ms).
- **The only visible effect of today's gap:** if a button is held for more than 10 s with no other input, the menu button comes back to full opacity while the player is still holding it. The inactivity timer started at key-down and nothing restarts it before release.
- **If key-up also fades:** the button stays dim until 10 s after the last *release*, which matches "10 s of inactivity" better. The first animation is a no-op, because the alpha is already 0.3.
- **Verdict:** this is the only job where key-up would fix something a player can see. The case is rare (holding one button for 10 s or more with nothing else pressed).

### 3. PiP still-frame capture

- **Path:** `GameInputHost.capturePipFrame()` → `PipController.maybeCapturePipFrame()` → `ScreenshotCaptureUtil.capturePipFrame(...)` → `PipFrameStore.capturePipFrame(...)`.
- It is skipped below API 26, when PiP is disabled in the config, while already in PiP, or before the first frame is rendered. Otherwise it is throttled by `PIP_FRAME_MIN_INTERVAL_MS` (2.5 s) unless forced.
- **If key-up also captures:** an extra call is almost always dropped by the throttle, so the cost is negligible. The gain is also negligible: a frame taken at release is at most one throttle window fresher.
- **Verdict:** no reason to add it, and no real harm either.

## Open question to settle first

**Do held physical gamepad buttons auto-repeat `KEY_DOWN`?** Android auto-repeats held keys (`repeatCount > 0`); `ui/retromenu3/navigation/KeyboardInputAdapter` already ignores repeats in the menu. If gamepad buttons repeat too, `onKeyDown` keeps restarting the fade timer while a button is held, the "restored while held" case above never happens, and the fade fix isn't needed. On the other hand, repeats would also flood the frame-time samples, which would be worth knowing.

How to check: on a device with a physical controller, log `event.repeatCount` in `GameInputRouter.onKeyDown` (or with `adb shell getevent -lt` plus a temporary log), hold a face button for more than 10 s, and watch whether repeats arrive and whether the menu button comes back to full opacity while the button is held.

Analog sticks and triggers arrive as `onGenericMotionEvent` and only fire when a value changes, so a stick held still is the same case as a held button.

## Recommendation

1. **Default: keep the current behavior.** Nothing is broken for normal play, and #138 deliberately preserved it.
2. **If the device check shows no auto-repeat and the restore-while-held case is worth fixing:** make key-up restart **only the fade**, not the timing and not the PiP capture:

   ```kotlin
   fun onKeyUp(keyCode: Int, event: KeyEvent, superCall: () -> Boolean): Boolean {
       host.triggerButtonFade()
       return host.processKey(keyCode, event) ?: superCall()
   }
   ```

   Don't route key-up through `onUserInput()`. That would bring in the timing skew and the redundant PiP call.

## TODO checklist for the future fix

Follow the usual workflow: one branch from the latest `develop` (e.g. `fix/keyup-restarts-button-fade`), one PR against `develop`, and wait for the merge.

- [ ] Run the device check above and record the result (repeat or no repeat) in the PR.
- [ ] If the fix goes ahead, change `GameInputRouter.onKeyUp` as shown, and update its KDoc and the `GameInputHost` KDoc if they describe key-up.
- [ ] `tests/GameInputRouter_test.kt`:
  - replace `key up only asks the view model` with a test that verifies the sequence `triggerButtonFade` → `processKey`
  - add a test that key-up never calls `recordFrame` or `capturePipFrame`
  - keep the two tests for the result and the `super` fallback
- [ ] `FloatingMenuButtonController` tests: add or confirm a regression test showing that a second `triggerFade()` cancels the pending restore and reposts it for a full `INACTIVITY_RESTORE_DELAY_MS`. That is the property the fix relies on.
- [ ] Update the table in this document, or mark it resolved and link the PR.
- [ ] `./gradlew check -PskipAssetStaging` (unit tests, detekt main/test at 0 issues, Kover floor).
- [ ] Never name games, platforms or brands in code, tests, commits or the PR.

## References

- PR #138: `GameInputRouter` extraction (behavior preserved)
- `app/src/main/java/com/vinaooo/revenger/controllers/GameInputRouter.kt`
- `app/src/main/java/com/vinaooo/revenger/views/GameActivity.kt`: `inputRouter`, `recordFrameTime()`, `onKeyDown` / `onKeyUp` / `onGenericMotionEvent` / `dispatchTouchEvent`
- `app/src/main/java/com/vinaooo/revenger/controllers/FloatingMenuButtonController.kt`: `triggerFade()` and its timing constants
- `app/src/main/java/com/vinaooo/revenger/utils/FrameTimeRecorder.kt`
- `app/src/main/java/com/vinaooo/revenger/performance/AdvancedPerformanceProfiler.kt`
- `app/src/main/java/com/vinaooo/revenger/controllers/PipController.kt`: `maybeCapturePipFrame()`
- `app/src/main/java/com/vinaooo/revenger/utils/PipFrameStore.kt`: `PIP_FRAME_MIN_INTERVAL_MS`
- `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/navigation/KeyboardInputAdapter.kt`: existing `repeatCount` handling
- `tests/GameInputRouter_test.kt`
