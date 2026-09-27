# Bug Report: Native SIGSEGV on GLThread During Repeated Activity Recreation

**Date found:** 2026-09-14
**Status:** Open — root cause confirmed inside the closed-source native dependency;
six fix attempts tried (five app-side, one dependency version bump), all failed
(see "Update 2026-09-14 (session 2)")
**Severity:** High — native crash (process death), reproducible on-device
**Found via:** `./gradlew connectedDebugAndroidTest` on a physical device (Android, arm64-v8a)

## Summary

Two instrumented tests that call `ActivityScenario.recreate()` in a loop reliably
crash the app process with a native `SIGSEGV` on a GL rendering thread. This is not
a test-infrastructure problem — it reproduces the same way in two independent test
classes and points at a real lifecycle bug in how the emulator's GL surface
(`RetroView` / `GLRetroView`) is torn down and rebuilt when `GameActivity` is
recreated.

## Reproduction

Either of these triggers it:

```bash
./gradlew connectedDebugAndroidTest --tests \
  "com.vinaooo.revenger.ui.integration.GameActivityCleanupIntegrationTest.testActivityRecreationStress"

./gradlew connectedDebugAndroidTest --tests \
  "com.vinaooo.revenger.ui.integration.MemoryAndPerformanceTest.testMultipleRecreationsNoLeak"
```

Both are in `app/src/androidTest/java/com/vinaooo/revenger/GameActivityCleanupIntegrationTest_test.kt`
and do the same thing at different repeat counts:

```kotlin
// testActivityRecreationStress (3 iterations)
repeat(3) {
    activityRule.scenario.recreate()
    activityRule.scenario.onActivity { activity -> assert(!activity.isDestroyed) }
}

// testMultipleRecreationsNoLeak (5 iterations)
repeat(5) {
    activityRule.scenario.recreate()
    activityRule.scenario.onActivity { activity -> assert(!activity.isDestroyed) }
}
```

`ActivityScenario.recreate()` forces `GameActivity` through a full
`onPause → onStop → onDestroy → onCreate → onStart → onResume` cycle without
actually finishing the activity's task — the same mechanism used for a
configuration change. It typically survives past the second or third recreation
before the native crash kills the process (confirmed on both test methods,
independently, on the same device).

**Excluding both tests, the rest of the instrumented suite passes clean (11/11).**
This is an isolated, reproducible issue in the recreate path, not a flaky/environmental
failure.

## Crash evidence

From `adb logcat` at crash time:

```
F libc    : Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x77ea9da000 in tid 10145 (GLThread 58), pid 9984 (...)
F DEBUG   : signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x00000077ea9da000 (write)
I Zygote  : Process 9984 exited due to signal 11 (Segmentation fault)
I ActivityManager: Process com.vinaooo.revenger.<config-derived-id> (pid 9984) has died: fg  TOP
```

A tombstone was written on-device (`/data/tombstones/tombstone_37.pb`) but was not
pulled/symbolized as part of this session — pulling and symbolizing it (see "Suggested
next steps") should be the first move in the next session, since it will show the
native (C++/JNI) frame where the fault happened, which the JVM-side logcat above
does not.

The crash is a **write fault** on thread named `GLThread <n>` — this is the
rendering thread that `com.swordfish.libretrodroid.GLRetroView` (the LibretroDroid
library backing `RetroView`) manages internally. This points at either:
- a GL/EGL context or surface being used after it was destroyed, or
- two `GLRetroView` instances' GL threads overlapping (old one still tearing down
  natively while a new one starts rendering) during rapid recreate cycles.

## Root cause hypothesis (not yet confirmed — starting point for next session)

`GameActivity.onDestroy()` (`app/src/main/java/com/vinaooo/revenger/views/GameActivity.kt`,
around line 1323) does, in this order:

```kotlin
override fun onDestroy() {
    // ...
    viewModel.dispose()
    viewModel.detachRetroView(this)   // <-- removes the Lifecycle observer
    // ...
    super.onDestroy()                 // <-- ON_DESTROY event actually dispatched here
}
```

`GameActivityViewModel.detachRetroView()` (`app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt`,
line 1645):

```kotlin
fun detachRetroView(activity: ComponentActivity) {
    retroView?.let { activity.lifecycle.removeObserver(it.view) }
    retroView = null
}
```

`GLRetroView` was registered as a `Lifecycle` observer in `setupRetroView()`
(same file, line ~1394: `activity.lifecycle.addObserver(retroView.view)`), which is
how LibretroDroid presumably knows to release its GL/EGL resources on `ON_DESTROY`.

**The observer is removed *before* `super.onDestroy()` runs.** Since the
`ON_DESTROY` lifecycle event is what the Android `Lifecycle` registry dispatches to
observers as part of `super.onDestroy()`, removing the observer first means
`GLRetroView` may never receive its own `onDestroy()`/`ON_DESTROY` callback through
the intended path — i.e. its GL thread may not be told to shut down cleanly before
the Activity (and its Window/Surface) go away. A brand-new `GameActivity` instance
then calls `setupRetroView()` again almost immediately (recreate() runs
destroy→create back-to-back), creating a second `GLRetroView` / GL thread while the
first one's native resources may still be mid-teardown (or never properly
triggered) — a classic double-GL-context race that manifests as a `SEGV_MAPERR`
write fault on a GL thread.

This is a hypothesis backed by reading the call order, **not** confirmed by
stepping through LibretroDroid's own source or a symbolized native stack. It is the
most concrete lead available and should be the first thing checked.

Also relevant: `GameActivity.onDestroy()` does not check
`isChangingConfigurations()` anywhere. Even if the observer-ordering issue above
turns out not to be the (sole) cause, the absence of that check means `GameActivity`
tears down and rebuilds the entire `RetroView`/`GLRetroView` on *every* recreation,
including ones that are logically just a configuration change — expensive, and the
likely reason repeated recreation is what exposes the race (a single recreation may
"usually" get lucky on timing; 3–5 in a row reliably don't).

## Suggested next steps

1. **Pull and symbolize the tombstone** for a definitive native stack:
   ```bash
   adb pull /data/tombstones/tombstone_37.pb ./tombstone_37.pb
   # or: adb shell logcat -b crash -d   (while reproducing)
   ```
   Use `ndk-stack` or `addr2line` against the LibretroDroid/core `.so` in
   `app/build/intermediates/.../jniLibs` if the fault address falls inside a mapped
   native library (the report only pulled the JVM-visible `logcat`/`DEBUG` lines,
   not the full tombstone).
2. **Swap the `onDestroy()` ordering** in `GameActivity.kt` — call
   `retroView?.view?.onDestroy()` (or otherwise let the `GLRetroView` finish its own
   teardown) *before* removing the lifecycle observer / nulling the reference, or
   restructure `detachRetroView()` to explicitly invoke the GL teardown rather than
   relying solely on the (now possibly-skipped) lifecycle callback.
3. **Check LibretroDroid's `GLRetroView` source** (dependency, see
   `definitions/Code.md` → Dependencies table, version 0.13.1) for what its
   `Lifecycle` observer actually does on `ON_DESTROY`, and whether it exposes a
   synchronous/blocking teardown method safe to call before the observer is
   detached.
4. **Consider gating RetroView teardown on `isChangingConfigurations()`** so a
   plain rotation (or `recreate()`) does not tear down and rebuild the GL surface
   at all — this would sidestep the race entirely for the common case, at the cost
   of needing to verify state (loaded ROM/core, save state, frame buffer) is still
   valid across the swap of `GameActivity` instances that share a retained
   `GameActivityViewModel`.
5. Once a fix is in place, re-run both reproduction commands above 3+ times each
   (the crash needs a couple of recreations to surface) before considering it
   resolved, plus the full `connectedDebugAndroidTest` suite to confirm no
   regression in the other 11 tests.

## Session context (how this was found)

This crash was found while getting `connectedDebugAndroidTest` to run at all for
the first time in this project — it had never successfully executed before this
session. Three unrelated, already-fixed problems had to be cleared first (all
separate from this bug, listed here only for continuity):

- Stale API references in `GameActivityCleanupIntegrationTest_test.kt`
  (`GameActivity.viewModel`/`.retroView` no longer accessible/existent) —
  fixed by resolving `GameActivityViewModel` via `ViewModelProvider` inside the
  test instead.
- No `testInstrumentationRunner` configured in `app/build.gradle`, so
  instrumentation fell back to the legacy `android.test.InstrumentationTestRunner`,
  which cannot launch on this device's Android version at all. Fixed by declaring
  `androidx.test.runner.AndroidJUnitRunner`.
- `cleanupRom`'s `finalizedBy` wiring in `app/build.gradle` matched *any* task
  name starting with `"bundle"`, including AGP-internal tasks
  (`bundleDebugClassesToCompileJar`) that run on every compile — this raced ahead
  of `packageDebug` and shipped a test APK with no ROM staged. Fixed by excluding
  task names containing `"Classes"`.
- `androidx.test.espresso:espresso-core:3.6.1`'s event-injection path calls
  `InputManager.getInstance()` via reflection, which no longer exists on this
  device's Android version. Fixed by replacing the two `onView(...).check{}`
  presence-only checks with direct `findViewById` assertions (they never needed
  Espresso's input injection machinery in the first place).

With all four cleared, the instrumented suite actually runs, which is what
surfaced this GL thread crash — a real, previously-undetected bug rather than an
artifact of the broken test infrastructure.

## Update 2026-09-14 (session 2): root cause confirmed, four fixes tried, none work

A follow-up session set out to apply the "suggested next steps" above as a one-line
fix on branch `fix/glthread-crash-on-activity-recreate`. It ended up decompiling
LibretroDroid's bytecode, reading its native C++ source from GitHub, and searching
its issue tracker. The bug is real and is **inside the closed-source native
dependency**, not fixable with an app-side ordering change alone.

### What was confirmed

1. **`LibretroDroid` (the JNI binding class) is entirely static / process-global.**
   `create`, `destroy`, `pause`, `resume`, `step`, `serializeState`, etc. are all
   `public static native` methods with no per-instance handle — decompiled via
   `javap -p -classpath classes.jar com.swordfish.libretrodroid.LibretroDroid`
   against the `.aar` in the Gradle cache
   (`~/.gradle/caches/modules-2/files-2.1/com.github.swordfish90/libretrodroid/0.13.1/.../libretrodroid-0.13.1.aar`).
   There is exactly one native core alive per process, regardless of how many
   `GLRetroView` Kotlin objects exist.

2. **`GLRetroView.onDestroy()` and `.onCreate()` call the static natives directly
   on the calling thread** — `catchExceptions(...)` wraps the call in a try/catch
   for Kotlin exceptions only, with no dispatch through `runOnGLThread`/`queueEvent`.
   A native SIGSEGV inside that call is **not** catchable from Kotlin (matches
   `Swordfish90/LibretroDroid#81`, an old closed issue with the same complaint:
   "it didn't throw a java exception, it just segfaulted, so that try-catch
   doesn't work either").

3. **The native `destroy()` has no null guard**, confirmed by reading
   `libretrodroid/src/main/cpp/libretrodroid.cpp` at tag `0.13.1` from
   `github.com/swordfish90/LibretroDroid`:
   ```cpp
   void LibretroDroid::destroy() {
       LOGD("Performing libretrodroid destroy");
       if (Environment::getInstance().getHwContextDestroy() != nullptr) {
           Environment::getInstance().getHwContextDestroy()();
       }
       core->retro_unload_game();   // <-- core is a std::unique_ptr<Core>, never null-checked
       core->retro_deinit();
       video = nullptr;
       core = nullptr;
       // ...
   }
   ```
   `core` becomes null the moment `destroy()` runs, and nothing prevents `destroy()`
   from running against an already-null/never-created `core`. This exactly matches
   the crash signature seen whenever the app called `destroy()` explicitly: `Fatal
   signal 11 (SIGSEGV) ... fault addr 0x80 ... Cause: null pointer dereference`,
   with a fully symbolized tombstone frame:
   ```
   #00 liblibretrodroid.so (libretrodroid::LibretroDroid::destroy()+84)
   #01 liblibretrodroid.so (Java_com_swordfish_libretrodroid_LibretroDroid_destroy+44)
   #07 base.apk (com.swordfish.libretrodroid.GLRetroView$onDestroy$1.invoke+0)
   #17 base.apk (com.swordfish.libretrodroid.GLRetroView.catchExceptions+0)
   #22 base.apk (com.swordfish.libretrodroid.GLRetroView.onDestroy+0)
   #27 base.apk (com.vinaooo.revenger.retroview.RetroView.destroy+0)
   #32 base.apk (com.vinaooo.revenger.viewmodels.GameActivityViewModel.detachRetroView+0)
   #37 base.apk (com.vinaooo.revenger.views.GameActivity.onDestroy+0)
   ```
   (Frames repeating at e.g. #07/#12 are not recursion — Kotlin `Function0<Unit>`
   lambdas compile to a typed `invoke(): Unit` plus a bridge `invoke(): Object`
   that calls it, which the ART interpreter's stack walker shows as two frames for
   one logical call.)

4. **`0.14.0` adds a mutex around `destroy()`/`step()`, but it does not fix this
   crash (tested — see attempt #6 below).** Tags `0.13.2` and `0.14.0` exist above
   the `0.13.1` this project pins. Diffing `libretrodroid.cpp` at both tags
   (`gh api repos/swordfish90/LibretroDroid/contents/.../libretrodroid.cpp?ref=<tag>
   -H "Accept: application/vnd.github.raw"`) shows `0.14.0`'s `destroy()` gained a
   `std::lock_guard<std::mutex> lock(coreLock);` at its top, and `step()` (the
   per-frame call on the GL thread) takes the same lock — this closes a
   `destroy()`-vs-`step()` torn-write race, which is a real improvement. But
   `destroy()` still has **no null check** on `core` before `core->retro_unload_game()`,
   and `create()` still does **not** take `coreLock` at all. A mutex cannot fix a
   null-pointer read on entry, and empirically it didn't (attempt #6). `0.13.2`'s
   only other change ("Allow core interaction methods to be called on different
   threads", `Swordfish90/LibretroDroid#126`) touches `serializeState`/`setCheat`/etc.,
   not `create`/`destroy`. Upstream issue `#53` ("Crash on re-adding view to
   another parent") has the maintainer answering "there is a lot going on
   including the lifecycle observers and the OpenGL state" when asked if
   reparenting/recreating `GLRetroView` is safe — this area is
   acknowledged-fragile, and the `0.14.0` mutex is a partial hardening, not a fix
   for this specific crash.

### Six fix attempts, all failed

All tested against `testActivityRecreationStress` (3 back-to-back
`ActivityScenario.recreate()` calls), 3 reruns each, using
`-Pandroid.testInstrumentationRunnerArguments.class=<class>#<method>` (the
`--tests` flag is not supported by `connectedDebugAndroidTest`):

| # | Change | Result |
|---|--------|--------|
| 1 | `GameActivity.onDestroy()`: add explicit `viewModel.retroView?.destroy()` before `detachRetroView()` | New crash: null deref *inside* `LibretroDroid::destroy()`, main thread (see above) |
| 2 | Reorder `GameActivity.onDestroy()` so `super.onDestroy()` runs before `detachRetroView()`, letting the still-registered `GLRetroView` observer get its natural `ON_DESTROY` callback | Reverted to the original GLThread-based SIGSEGV, unsymbolized in a quick check |
| 3 | In `detachRetroView()`: remove the lifecycle observer, then `(it.view.parent as? ViewGroup)?.removeView(it.view)` (forces `GLSurfaceView.onDetachedFromWindow()` → `requestExitAndWait()`, which should synchronously join the GL thread), then call `it.destroy()` | Same null-deref-in-`destroy()` crash as #1, all 3 runs |
| 4 | Add a `RetroView.isDestroyed` guard so `destroy()` is a no-op on a second call, kept the parent-detach from #3 | Same crash, unchanged — proves it is **not** a double-call on the same `RetroView` Kotlin wrapper |
| 5 | Kept the parent-detach from #3, but stopped calling `destroy()` at all (relying on the next `create()`'s `core = std::make_unique<Core>(...)` to free the previous core, confirmed self-healing by the `create()` source) | Back to the *original* GLThread SIGSEGV (main thread crash gone, but the GL-thread race is back) |
| 6 | Bumped the dependency to `libretrodroid:0.14.0` (from `0.13.1`), kept the parent-detach from #3 and the explicit `destroy()` call | Same null-deref-in-`destroy()` crash as #1/#3/#4, all 3 runs, identical fault address `0x80` |

Net result: calling `destroy()` crashes reliably inside the native library's own
missing null check, on both `0.13.1` and `0.14.0`; not calling it leaves the
original GL-thread race from before any of this session's changes. Detaching the
view from its parent (`ViewGroup.removeView()`) did not, by itself, prevent the
race between the old `GLThread` and a new `GLRetroView`'s native init the way
`GLSurfaceView`'s documented `onDetachedFromWindow()`/`requestExitAndWait()`
contract suggested it would — why is unresolved (open question below). The
`0.14.0` mutex closes a torn-write race but not a null-pointer-on-entry, which is
what attempt #6 demonstrates empirically.

All app-side code from these attempts (including the version bump) was reverted;
nothing from this investigation is committed anywhere. Branch
`fix/glthread-crash-on-activity-recreate` exists but currently matches `develop` —
none of the six attempts fix the crash on its own, so none were worth keeping as a
starting point.

### Open questions for whoever picks this up next

- Why doesn't `removeView()` → `onDetachedFromWindow()` → `GLThread.requestExitAndWait()`
  actually prevent the old GL thread from racing the new one? Worth confirming with
  a log line immediately before/after the `removeView()` call (thread name/state)
  rather than assuming the framework contract holds here — `preserveEGLContextOnPause`
  or something else `GLRetroView`-specific may interfere.
- Whether `create()` can genuinely fail to run synchronously in `setupRetroView()`
  before `onDestroy()` fires on a fast enough recreate cycle (this session assumed
  it always completes synchronously, based on `AndroidX Lifecycle` dispatching
  observer callbacks synchronously up to current state on `addObserver()` — this
  was not directly instrumented/verified with logging, only inferred from bytecode).
- Whether a targeted guard is possible: only call `destroy()` when this specific
  `RetroView`/`GLRetroView` instance is known to have completed `create()`
  (e.g. gate on the first `GLRetroEvents.SurfaceCreated` event) — untried this
  session.
- Whether reporting/patching upstream (fork LibretroDroid, add the null check to
  `destroy()`, publish from a JitPack fork or similar) is more tractable than
  continuing to work around it app-side, given the native fix is a one-line
  `if (core) { ... }` guard in a file already read in full this session
  (`libretrodroid/src/main/cpp/libretrodroid.cpp`, function `LibretroDroid::destroy()`,
  ~line 400).
- Real-world impact is still bounded by what the original report noted:
  `GameActivity` declares broad `configChanges` in the manifest, so real device
  rotation never hits this path; the realistic trigger is process-death-restore
  from Recents or "Don't keep activities", not a tight recreate loop — this bug is
  real but may be lower-frequency in practice than the instrumented test's 3x-in-a-row
  reproduction suggests.

## Related files

- `app/src/main/java/com/vinaooo/revenger/views/GameActivity.kt` (`onDestroy()`, ~line 1323)
- `app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt`
  (`setupRetroView()` ~line 1385, `detachRetroView()` line 1645, `dispose()` line 1660)
- `app/src/main/java/com/vinaooo/revenger/retroview/RetroView.kt` (`init` block,
  `onResume()`/`onPause()`/`onDestroy()` wrappers around `GLRetroView`)
- `app/src/androidTest/java/com/vinaooo/revenger/GameActivityCleanupIntegrationTest_test.kt`
  (`testActivityRecreationStress`, `testMultipleRecreationsNoLeak`)
