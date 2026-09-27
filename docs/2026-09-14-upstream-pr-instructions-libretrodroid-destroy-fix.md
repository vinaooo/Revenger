# Instructions: Open a PR against swordfish90/LibretroDroid fixing a null-pointer SIGSEGV in `destroy()`

**Audience:** a fresh Claude Code session with no prior context on this investigation.
**Goal:** fork `swordfish90/LibretroDroid` on GitHub, apply a small defensive fix to
its native `destroy()` function, and open a pull request against the upstream repo.

This document is self-contained. You do not need to read anything else in this
repository (Revenger) to execute it — everything required is below. Do not
reference Revenger, its internal file paths, or any game/ROM name anywhere in the
PR, issue, commit message, or branch name; the fix and its justification stand
entirely on LibretroDroid's own code and public issue tracker.

## Background (context only, not required reading elsewhere)

LibretroDroid (`https://github.com/swordfish90/LibretroDroid`) is an Android
library wrapping libretro cores behind a `GLRetroView` (a `GLSurfaceView`
subclass) and a JNI layer (`com.swordfish.libretrodroid.LibretroDroid`) that is
**entirely static** — every native call (`create`, `destroy`, `pause`, `resume`,
`step`, ...) operates on one process-global native core, not a per-instance
handle.

While testing repeated `GLRetroView` teardown/recreation (an app tearing down an
Activity that owns a `GLRetroView` and creating a new one shortly after — e.g. via
Android's own Activity recreation on configuration change or process restore),
calling the view's `onDestroy()` reliably crashes the process with a native
`SIGSEGV`, tested against both `0.13.1` and `0.14.0`.

## The confirmed bug

`libretrodroid/src/main/cpp/libretrodroid.cpp`, function `LibretroDroid::destroy()`,
as of tag `0.14.0` (unchanged from `0.13.1` except for a mutex added around the
whole function body):

```cpp
void LibretroDroid::destroy() {
    std::lock_guard<std::mutex> lock(coreLock);

    LOGD("Performing libretrodroid destroy");

    if (Environment::getInstance().getHwContextDestroy() != nullptr) {
        Environment::getInstance().getHwContextDestroy()();
    }

    core->retro_unload_game();   // <-- core is a std::unique_ptr<Core>, never null-checked
    core->retro_deinit();

    video = nullptr;
    core = nullptr;
    rumble = nullptr;
    fpsSync = nullptr;
    audio = nullptr;

    Environment::getInstance().deinitialize();
    VFS::getInstance().deinitialize();
}
```

`core` is a `std::unique_ptr<Core>` member. If `destroy()` runs while `core` is
already null — because `destroy()` was called twice, or called before `create()`
ever successfully ran for this cycle — `core->retro_unload_game()` dereferences a
null pointer and the whole process dies with `SIGSEGV`. There is no null check
anywhere in the function.

### Reproduction and evidence

Symbolized native backtrace pulled from an on-device tombstone (Android 15/16,
arm64-v8a), captured while destroying and recreating a `GLRetroView` repeatedly
(e.g. via `Activity.recreate()` or `ActivityScenario.recreate()` in an
instrumented test) and calling the view's `onDestroy()` on each teardown:

```
Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x80 (read)
Cause: null pointer dereference

#00 liblibretrodroid.so (libretrodroid::LibretroDroid::destroy()+84)
#01 liblibretrodroid.so (Java_com_swordfish_libretrodroid_LibretroDroid_destroy+44)
#02-06 art (JNI/interpreter trampoline frames)
#07 base.apk (com.swordfish.libretrodroid.GLRetroView$onDestroy$1.invoke+0)
#17 base.apk (com.swordfish.libretrodroid.GLRetroView.catchExceptions+0)
#22 base.apk (com.swordfish.libretrodroid.GLRetroView.onDestroy+0)
```

`fault addr 0x80` is consistent with dereferencing a null `Core*` at a small,
fixed member offset (e.g. a stored function pointer field) inside
`retro_unload_game()`/`retro_deinit()`.

Also worth noting for the PR body: `GLRetroView.catchExceptions()` (the Kotlin
wrapper) only catches Kotlin/JVM exceptions — a native SIGSEGV crossing the JNI
boundary is **not** catchable there, so there is no app-side way to guard against
this. This matches a old, closed upstream report with the same complaint,
`swordfish90/LibretroDroid#81` ("onDestroy() Crash in 0.6.0" — "it didn't throw a
java exception, it just segfaulted, so that try-catch doesn't work either"),
though that issue was about a different code path (`Environment::deinitialize()`,
a `strlen()` bug) and was already fixed. This is a separate, still-open defect.

## The fix

Add an early-return guard at the top of `destroy()` (after acquiring the lock, if
the lock is still present on current `master` — see step 3 below) so a null/already
-torn-down `core` is a safe no-op instead of a crash:

```cpp
void LibretroDroid::destroy() {
    std::lock_guard<std::mutex> lock(coreLock);

    LOGD("Performing libretrodroid destroy");

    if (!core) {
        LOGD("libretrodroid destroy called with no active core, ignoring");
        return;
    }

    if (Environment::getInstance().getHwContextDestroy() != nullptr) {
        Environment::getInstance().getHwContextDestroy()();
    }

    core->retro_unload_game();
    core->retro_deinit();

    video = nullptr;
    core = nullptr;
    rumble = nullptr;
    fpsSync = nullptr;
    audio = nullptr;

    Environment::getInstance().deinitialize();
    VFS::getInstance().deinitialize();
}
```

This is the entire functional change: one guard clause, no behavior change for the
normal (non-null `core`) path.

**Do not** attempt to fix the separate, unconfirmed hypothesis that `create()`
isn't synchronized with `step()`/the render thread via the same `coreLock` — that
was investigated but never confirmed as a real bug (it's plausible but unproven,
and out of scope for this PR). Keep this PR strictly to the null-check guard
above. A reviewer is far more likely to accept a minimal, obviously-correct
one-guard-clause fix than a larger speculative change.

## Step-by-step execution

### 1. Preflight

```bash
gh auth status
```

Confirm you're authenticated as the account that should own the fork (ask the
user if unclear — do not assume).

Check nobody has already fixed or reported this exact issue:

```bash
gh api search/issues --method GET -f q='repo:swordfish90/LibretroDroid destroy null' --jq '.items[] | {title, number, state, url}'
gh api search/issues --method GET -f q='repo:swordfish90/LibretroDroid SIGSEGV core' --jq '.items[] | {title, number, state, url}'
gh pr list --repo swordfish90/LibretroDroid --search "destroy null" --state all
```

If you find an existing open PR or issue covering the same fix, stop and report
that back instead of duplicating it.

### 2. Fork and clone

```bash
gh repo fork swordfish90/LibretroDroid --clone --remote
cd LibretroDroid
git checkout master
git pull origin master   # or the fork's upstream remote, whichever gh set up — check `git remote -v`
```

### 3. Verify current `destroy()` matches what's described above

```bash
grep -n -A20 "^void LibretroDroid::destroy" libretrodroid/src/main/cpp/libretrodroid.cpp
```

The exact surrounding code (the mutex, the log line) may have changed since this
document was written. Re-read the current function fully before editing. The
fix's shape (an early `if (!core) return;` guard, placed after the lock is
acquired and before the first dereference of `core`) still applies regardless of
minor surrounding changes — adapt to what you actually see, don't blindly patch
line numbers.

### 4. Apply the fix

Edit `libretrodroid/src/main/cpp/libretrodroid.cpp`'s `LibretroDroid::destroy()`
per the "The fix" section above, adapted to the current source if it has drifted.

### 5. Try to build (best effort)

```bash
./gradlew :libretrodroid:assembleRelease
```

This requires the Android NDK. If it's not available in this environment, skip the
build and say so plainly in the PR description rather than claiming it was
verified. Do not attempt to install the NDK just for this — that's a heavier
action than this fix warrants; note the limitation instead.

### 6. Commit

```bash
git checkout -b fix/destroy-null-core-guard
git add libretrodroid/src/main/cpp/libretrodroid.cpp
git commit -m "$(cat <<'EOF'
Fix native SIGSEGV in destroy() when core is already null

LibretroDroid::destroy() dereferences its `core` unique_ptr
(core->retro_unload_game(), core->retro_deinit()) with no null check.
If destroy() runs while core is already null - e.g. called twice, or
called before create() ever completed for a given cycle - this
crashes the whole process with a native SIGSEGV that cannot be caught
from the Kotlin side (GLRetroView.catchExceptions only catches JVM
exceptions, not native faults crossing the JNI boundary).

Add an early-return guard so a null core is a safe no-op.

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
git push fork fix/destroy-null-core-guard   # remote name may differ - check `git remote -v`
```

(Use whatever attribution convention the user who runs this session actually
wants — ask if their own session context specifies something different from the
line above.)

### 7. STOP before opening the PR

Opening a PR is a public, visible, hard-to-fully-reverse action against a
third-party repository you don't control. Before running `gh pr create`:

- Show the user the final diff (`git diff master...fix/destroy-null-core-guard`).
- Show the user the exact PR title and body you intend to submit (draft below).
- Get explicit confirmation to proceed.

Do not skip this checkpoint even though this document was written in advance —
"the user already asked for this document" is not the same as "the user approved
this exact diff and PR text."

### 8. Open the PR

```bash
gh pr create --repo swordfish90/LibretroDroid \
  --base master \
  --head <your-github-username>:fix/destroy-null-core-guard \
  --title "Fix native SIGSEGV in destroy() when core is already null" \
  --body "$(cat <<'EOF'
## Summary

`LibretroDroid::destroy()` (`libretrodroid/src/main/cpp/libretrodroid.cpp`)
dereferences its `core` member (`std::unique_ptr<Core>`) with no null check:

```cpp
core->retro_unload_game();
core->retro_deinit();
```

If `destroy()` runs while `core` is already null - e.g. called twice in a row, or
called before `create()` has completed for a given cycle - this crashes the whole
process with a native SIGSEGV. This is not catchable from the Kotlin side:
`GLRetroView.catchExceptions()` only catches JVM exceptions, and a native fault
crossing the JNI boundary skips it entirely.

## Evidence

Symbolized tombstone backtrace (Android 15/16, arm64-v8a), reproduced by
repeatedly tearing down and recreating a `GLRetroView` (e.g. via
`Activity.recreate()`):

```
Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x80 (read)
Cause: null pointer dereference

#00 liblibretrodroid.so (libretrodroid::LibretroDroid::destroy()+84)
#01 liblibretrodroid.so (Java_com_swordfish_libretrodroid_LibretroDroid_destroy+44)
#07 base.apk (com.swordfish.libretrodroid.GLRetroView$onDestroy$1.invoke+0)
#17 base.apk (com.swordfish.libretrodroid.GLRetroView.catchExceptions+0)
#22 base.apk (com.swordfish.libretrodroid.GLRetroView.onDestroy+0)
```

`fault addr 0x80` is consistent with a null `Core*` dereference at a small fixed
member offset.

## Fix

Add an early-return guard at the top of `destroy()`, after the existing lock is
acquired, before the first dereference of `core`:

```cpp
if (!core) {
    LOGD("libretrodroid destroy called with no active core, ignoring");
    return;
}
```

No behavior change for the normal (non-null `core`) path. This is the minimal
fix for the confirmed defect; I deliberately did not touch `create()` or attempt
any broader synchronization change.

## Testing

<state plainly here whether `:libretrodroid:assembleRelease` was actually run and
passed, or whether the NDK wasn't available and the change was verified by
inspection only - do not claim testing that didn't happen>
EOF
)"
```

Report the resulting PR URL back to the user.

## Notes for the executing session

- If `gh repo fork` reports the fork already exists (e.g. a prior attempt), reuse
  it (`git remote -v` to check) rather than erroring out or force-recreating it.
- If `master`'s `destroy()` already has a null check (someone beat us to it),
  stop, say so, and don't open a redundant PR.
- Keep the whole PR scoped to this one guard clause. Do not "clean up" nearby
  code, rename things, or fix unrelated issues you notice while in the file.
