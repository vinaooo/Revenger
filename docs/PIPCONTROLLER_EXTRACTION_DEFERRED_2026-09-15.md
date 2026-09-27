# Report: Full `PipController` Extraction Deferred

**Date:** 2026-09-15
**Status:** Deferred — not started. Decision made explicitly with the project owner, not abandoned silently.
**Context:** Phase 2 of the "split God classes" refactor plan (branch `refactor/split-god-classes`, worktree `.worktrees/split-god-classes`), plan file: `~/.claude/plans/please-check-the-project-fluttering-summit.md`
**Related work completed:** `PipSnapshotSelector` and `PipAspectRatioResolver` extractions (commits `0dc7267..4d2352e` on `refactor/split-god-classes`) — see "What shipped instead" below.

## Summary

The refactor plan's Phase 2 called for extracting `GameActivity.kt`'s entire Picture-in-Picture
(PiP) subsystem — 12 methods and 5 fields, roughly 390 lines — into a new `controllers/PipController.kt`
class, verified by Robolectric unit tests written before the move (characterization testing).

On investigation, both the test plan and the extraction's assumed scope turned out to be
inaccurate. Rather than execute a plan known to be wrong, the PiP-subsystem class extraction was
deferred. Two smaller, genuinely safe extractions were done instead (see below), and this
document records why the full extraction was deferred and what a future attempt should account
for.

## What was found

### 1. The plan's characterization test didn't match the real code

The plan's first required test was: *"PiP entry while a submenu is open captures the frame
visible at that moment and sets `suppressNextScreenshotCapture` so the next save-menu screenshot
is skipped."*

Reading the actual code (`GameActivity.kt`, `onUserLeaveHint()` /
`maybeEnterPictureInPictureAfterMenuClosed()` — the real PiP-*entry* path) shows neither method
ever touches `suppressNextScreenshotCapture`. That flag is set in a completely different place:
`pipBroadcastReceiver`'s `ACTION_PIP_SAVE` handler, which fires when the user taps "Save and
Exit" *inside* the PiP window — an unrelated trigger from PiP *entry*. The plan's audit
conflated two separate flows. Any test written to the plan's literal description would have
been testing behavior that doesn't exist.

### 2. The extraction needs far more access to `GameActivity` internals than planned

The plan described `PipController`'s dependencies as "GameActivity (or a narrow interface) plus
the `pipOverlay` ImageView." Reading every PiP method's body shows it actually needs read/write
access to at least 9 more currently-`private` `GameActivity` members:

| Member | Why the PiP code touches it |
|---|---|
| `viewModel` | Menu-open state, first-frame-rendered state, `suppressNextScreenshotCapture` |
| `appConfig` | Whether PiP is enabled for the current game/config |
| `retroviewContainer` | Width/height for the aspect-ratio fallback calculation |
| `menuContainer` | Hidden/shown on PiP enter/exit |
| `leftContainer`, `rightContainer` | On-screen gamepad hidden/shown on PiP enter/exit |
| `gameLifecycleObserver` | Notified around the PiP transition |
| `pipBroadcastReceiver` | Registered/unregistered inside `onPictureInPictureModeChanged` |
| `restoreFloatingButtonVisibility()` | Called after leaving PiP |

Plus several Activity-only framework methods (`registerReceiver`, `enterPictureInPictureMode`,
`setPictureInPictureParams`, `finishAndRemoveTask`, `lifecycleScope`, `runOnUiThread`) that only
an `Activity` can call, meaning `PipController` would need a live `GameActivity` reference for
these regardless of any interface design.

Two more hazards surfaced that the plan's line-range estimate (1352-1743) missed entirely:

- `maybeCapturePipFrame()` is also called from `dispatchTouchEvent()` and `onKeyDown()` —
  outside the stated range. Those call sites would need explicit rewiring.
- `onPause()`/`onResume()` contain non-PiP work in the same method (`viewModel.preserveState()`,
  `frameStartTime = System.nanoTime()`). A "thin delegation" would either drop this or need to
  keep it in `GameActivity` alongside a delegated PiP call — not a clean 1:1 method move.
- The `maybeCapturePipFrame(force = true)` call in `onPause()` must run *before*
  `viewModel.preserveState()` and `super.onPause()` — the GL surface becomes invalid immediately
  after. This ordering constraint is easy to lose in a mechanical extraction and nothing would
  catch it except a real device test.

### 3. Why unit tests can't safely verify this extraction

Testing `PipController` with a relaxed mock (`mockk<GameActivity>(relaxed = true)`) would make
every predicate the PiP code checks (`viewModel.retroView?.frameRendered?.value`,
`appConfig.isPipEnabled()`, etc.) return a mocked default rather than a real value, causing
nearly every PiP code path to early-return. A test suite built this way would mostly prove "the
code calls methods on a mock" rather than "PiP behavior is preserved" — which is precisely the
category of finding this project's own review process is built to catch.

A physical device is available in this environment (`adb devices` showed one connected device
during this session), so instrumented tests (`./gradlew connectedDebugAndroidTest`) are a viable
verification path for a future attempt — but that is a materially different (slower,
device-dependent, less repeatable in CI) verification strategy than every other task in this
refactor plan, which is why it was called out as a separate decision rather than folded into
"Phase 2, Small effort" as originally scoped.

### 4. The visibility-widening tradeoff

Making the ~9 members above reachable from a separate `PipController` class requires removing
their `private` modifier in `GameActivity.kt`. This is a Kotlin/JVM access-level change only — it
does not alter any runtime behavior by itself. The actual cost is longer-term and structural:

- Kotlin's usual mitigation for "visible to related code, not everyone" is the `internal`
  modifier, which restricts access to the same Gradle module. This project is a **single-module**
  app (`:app` is the only module), so `internal` and `public` are practically equivalent here —
  there is no module boundary to lean on. Containment would rely on convention/code review, not
  the compiler.
- Once these members are reachable from outside `GameActivity`, later phases of this same
  refactor plan (the gamepad-layout extraction, the floating-button extraction — both also
  planned against `GameActivity.kt`) will be working against the same widened seam and may be
  tempted to reach through it directly rather than through a clean method call, compounding the
  coupling this whole refactor effort is meant to reduce.

This is a known, common cost in extracting classes out of a legacy "God class" — not alarming,
but real, and it was worth deciding explicitly rather than incurring as a side effect of one
task.

## What shipped instead

Rather than attempt the full class move against an inaccurate test plan, two small, genuinely
pure pieces of PiP logic were extracted with real unit tests and zero visibility changes:

- **`utils/PipSnapshotSelector.kt`** — the snapshot fallback-chain selection logic
  (`getPipFrame() ?: getCachedFullScreenshot() ?: ... ?: lastSlotScreenshotOrNull()`), with tests
  covering short-circuit ordering and the recycled-bitmap edge case.
- **`utils/PipAspectRatioResolver.kt`** — the PiP aspect-ratio math (primary ratio vs.
  container-derived fallback, Android's `0.418..2.39` allowed range), with bit-exact boundary
  tests.

Both are behavior-preserving (independently verified against the pre-change code by a separate
review pass) and required zero changes to any `GameActivity` field's visibility. Commits
`0dc7267..4d2352e` on `refactor/split-god-classes`.

## Recommendation for a future attempt at the full extraction

If/when the full `PipController` class move is picked up:

1. Treat it as its own task, not a sub-item of a "Small" phase — budget for instrumented-test
   verification, not unit tests.
2. Confirm a device or emulator is available (`adb devices`) before starting; there is no honest
   fallback verification path without one.
3. Extend `app/src/androidTest/java/com/vinaooo/revenger/GameActivityCleanupIntegrationTest_test.kt`
   with real behavioral assertions (not just "doesn't crash") covering at minimum: PiP overlay
   visibility on `onUserLeaveHint`, gamepad-visibility restore on PiP exit matching its
   pre-PiP-entry state, and the "Save and Exit" pending-menu flow.
4. Explicitly account for the two call sites outside the original line-range estimate
   (`dispatchTouchEvent`, `onKeyDown`), the non-PiP work embedded in `onPause`/`onResume`, and the
   `onPause` ordering constraint described above.
5. Prefer exposing methods on `PipController` for the broadcast receiver to call
   (`onQuickSaveRequested()`, `onSaveAndExitRequested()`) rather than public mutable flags
   (`pendingPipQuickSave`, `pendingPipSaveMenu`) — otherwise the extraction reintroduces the same
   loose-public-mutable-state shape that an earlier phase of this same plan (`ControllerInput`'s
   callback bundling) was written specifically to eliminate.
6. If visibility must be widened, prefer `internal` over leaving members implicitly `public`
   even though this project's single-module structure means it buys little today — it documents
   intent for if/when the module structure ever changes, and costs nothing.
