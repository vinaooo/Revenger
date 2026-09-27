# Test battery follow-ups — 2026-09-26

Handoff for a new session. Source: the full test battery run on `develop` @ `9d3cdfc`, after PRs #108–#118, which closed every item of `2026-09-25-test-battery-followups.md`. This battery also covers the **Python icon scripts** (`icons/scripts/*.py`) and the **shell script** (`pick_icon.sh`). The TODO list below prepares tests for both.

## How to work through this list

- **One branch and one PR per item**, each branched from the latest `develop`. Open every PR against `develop`, never `master`.
- **After opening a PR, stop and wait for the user to merge it** before starting the next item.
- Every change includes tests (see `CLAUDE.md`). Never name games, consoles or brands anywhere: not in code, test data, commit messages or PR text. Use placeholders such as `"Some Game (Rev 1).zip"`.
- Before each PR:
  - run `./gradlew check -PskipAssetStaging`
  - for Python/shell items, also run `python3 -m pytest icons/tests`
- Run `claude-usage` between items (see the global `CLAUDE.md`).

### ⚠️ Rule for every Python/shell test: no real paths, no network
The scripts as written touch real files and the network. A careless test would read secrets, overwrite the app's icons, or call the icon APIs. Every test must:
- **Never read the real `icons/.env`**, which holds API keys. `fetch_icon.py` and `fetch_smart.py` currently call `load_env()` **at import time** and copy the keys into module constants. So until item 3 makes that lazy, importing them from `icons/scripts` reads the real file.
- **Never write to `app/src/main/res`.** `generate_android_icons()` writes the launcher icons there, based on the hard-coded `PROJECT_ROOT`.
- **Never write to `icons/.cache`.** `main()` stores the override icon and the ROM lock there.
- **Never touch the network.** Monkeypatch `requests.get` and `requests.post`, which needs no new dependency. Bind servers to `127.0.0.1` only.
- **Restore `os.environ`** with `monkeypatch` (`delenv` or `setenv` before the code under test writes).

## Baseline (2026-09-26)

### Android / Gradle
| Suite | Result |
|---|---|
| `detekt` / `detektMain` / `detektTest` | 0 / 0 / 0 issues (detektTest force-rerun) |
| `testDebugUnitTest` / `testReleaseUnitTest` | 1287 / 1287 passed each, 147 classes, force-rerun (`--rerun`); includes the 7 Roborazzi screenshot tests |
| Kover (debug) | **74.0% lines, 58.5% branches**, 75.4% methods, 87.4% classes. The floor is 72 / 56 |
| `lintDebug` | 0 Fatal, 1 Error (`MissingPermission`, the known false positive), 177 warnings |
| `assembleDebug` (full staging) | OK |
| `connectedDebugAndroidTest` | 20 / 20 passed on the `Pixel_9a_Android_16` AVD (x86_64, arm64 APK under ARM translation) |

The 177 lint warnings break down as:

| Warning | Count |
|---|---|
| `UnusedResources` | 52 |
| `UseKtx` | 26 |
| `Overdraw` | 20 |
| `NestedWeights` | 17 |
| `ObsoleteSdkInt` | 15 |
| `HardcodedText` | 11 |
| `SetTextI18n` | 7 |
| other | 29 |

### Python (`icons/scripts`, 6 modules, ~1250 lines)
| Check | Result |
|---|---|
| `py_compile` (scripts and tests) | OK |
| Import smoke test (sandbox copy without `.env`) | all 6 modules import; `master_icon.py --help` exits 0 |
| `pytest icons/tests` | 38 / 38 passed |
| Coverage (`pytest-cov`) | **3% overall**: `utils.py` 100%; `fetch_icon`, `fetch_smart`, `generate_typo`, `master_icon`, `web_gui` all **0%** (647 of 668 statements uncovered) |
| pyflakes | 5 findings (see "Real findings") |
| ruff, broad rule set | 331, mostly style: E501 99, W293 96, W291 33, E402 29, S101 21 (asserts in tests) |

### Shell (`pick_icon.sh`; `gradlew` is generated and out of scope)
| Check | Result |
|---|---|
| `bash -n` | OK |
| shellcheck 0.11 | 0 findings |
| git mode | 100755 |
| Behavior checks (stub `python3` on `PATH`) | **2 bugs**, see below |

### Real findings (fix these in the items below, each with a regression test)
- **`pick_icon.sh` hides failures.** When `python3 master_icon.py` exits non-zero, the script still prints "Selection complete!" and exits **0**. With a stub `python3` that exits 3, the script exits 0.
- **`pick_icon.sh` only works from the repo root.** It calls `icons/scripts/master_icon.py` by a relative path. Run from another directory, Python can't find the file, and the script still reports success and exits 0.
- **Missing request timeout.** `fetch_smart.py:155` has a `requests.get(url)` with no timeout, so the build can hang on a stalled connection. Everything else passes `timeout=`.
- **Web picker bound to all interfaces.** `web_gui.start_web_picker` binds `("", port)`, so while it waits the picker is reachable from the local network, and a POST there picks the app icon. It should bind `127.0.0.1`.
- **`master_icon.py` exits 0 when every icon method fails.** It logs an error, `generateIcons` succeeds, and the build silently keeps the old icons. Whether to change this is a decision (see below).
- **Default settings read from the wrong place.** `parse_config_xml(config_path)` always reads `default_settings.json` from `PROJECT_ROOT`, even when `--config` points to another tree. This is also the main obstacle to testing it.
- **Unused code (pyflakes):**
  - unused imports: `xml.etree.ElementTree` and `pathlib.Path` in `master_icon.py`, `clean_rom_name` in `fetch_smart.py`, `socketserver` in `web_gui.py`
  - an unused `global server_instance` at `web_gui.py:357`
  - a bare `except: pass` at `master_icon.py:344`
- **Undeclared runtime dependencies.** The scripts need `requests` and Pillow, but no requirements file lists them; `icons/requirements-dev.txt` only has pytest.
- **The repo `venv/` is broken.** Its `bin/python` is 3.14 but its libs are 3.13, so Pillow isn't importable from it. Use the system `python3` or a fresh venv; the Gradle `generateIcons` task already uses the system `python3`.
- **`generateIcons` calls the icon API on every full build** unless a manual override is cached. That makes the build slower, requires network access, and fails when the API is down or keys are missing (in that case the cascade falls back to local icons).
- **`.coverage` isn't ignored.** `pytest --cov` writes it to the repo root.

## How the battery was run (reproduce)

```bash
# Gradle (unit tests forced to actually rerun)
./gradlew check koverXmlReportDebug lintDebug -PskipAssetStaging --continue
./gradlew testDebugUnitTest --rerun testReleaseUnitTest --rerun detektTest --rerun -PskipAssetStaging
# Device: boot the AVD headless (the Android Studio skin isn't installed, hence -skin)
~/Android/Sdk/emulator/emulator -avd Pixel_9a_Android_16 -skin 1080x2424 -no-window -no-audio -no-snapshot -no-boot-anim &
./gradlew assembleDebug connectedDebugAndroidTest

# Python tools in a throwaway venv (never commit it)
python3 -m venv /tmp/pyvenv && /tmp/pyvenv/bin/pip install pytest pytest-cov ruff pyflakes shellcheck-py requests Pillow
/tmp/pyvenv/bin/python -m py_compile icons/scripts/*.py icons/tests/*.py
/tmp/pyvenv/bin/pyflakes icons/
/tmp/pyvenv/bin/ruff check --select E,F,W,B,S,SIM,PL --statistics icons/
/tmp/pyvenv/bin/python -m pytest icons/tests -p no:cacheprovider --cov=icons/scripts --cov-report=term
# Import smoke test: copy the scripts somewhere without a .env first, so the real keys are never read
cp -r icons/scripts /tmp/sandbox/ && cd /tmp/sandbox/scripts && for m in utils generate_typo fetch_icon fetch_smart web_gui master_icon; do python -c "import $m"; done

# Shell: static checks, then behavior checks with a stub python3 that prints its cwd/args and exits $STUB_EXIT
bash -n pick_icon.sh && /tmp/pyvenv/bin/shellcheck pick_icon.sh
PATH=/tmp/stubbin:/usr/bin:/bin STUB_EXIT=3 ./pick_icon.sh; echo $?      # prints success, exits 0 (bug)
(cd /tmp && /path/to/repo/pick_icon.sh); echo $?                        # can't find the script, exits 0 (bug)
```

## TODO (recommended order)

### [x] 1. (PR #119) `chore/python-test-tooling`: tooling and running the script tests from Gradle
- Add `icons/requirements.txt` with the runtime dependencies (`requests`, `Pillow`).
- Add `pytest-cov` and `shellcheck-py` to `icons/requirements-dev.txt`.
- Commit a small `ruff.toml` at the repo root with a pinned rule set (for example `E4,E7,E9,F,B`), so results don't depend on local ruff config. The "default" run here reported rules beyond ruff's built-in defaults.
- Add `.coverage`, `htmlcov/` and `.ruff_cache/` to `.gitignore`.
- Fix the pyflakes findings: 4 unused imports and the unused `global`.
- Tests: the existing 38 must still pass. Add a test that runs `pyflakes`/`ruff` over `icons/` if the tool is installed and skips otherwise, so the suite keeps the scripts lint-clean.
- **Run the Python/shell tests from Gradle** (user decision, 2026-09-26):
  - A `testScripts` task runs `python -m pytest icons/tests`. `check` depends on it.
  - It bootstraps its own venv under `build/` (`python3 -m venv` + `pip install -r icons/requirements.txt -r icons/requirements-dev.txt`), rebuilt only when the requirements files change. It doesn't depend on the broken repo `venv/` or on system pip, which Ubuntu blocks under PEP 668.
  - Declare `icons/scripts`, `icons/tests`, `*.sh` and the requirements files as inputs, so it stays up to date and cacheable.
  - If `python3` is missing, fail with a clear message rather than skipping silently.
  - `-PskipAssetStaging` doesn't skip it, since it needs no ROM or core.
  - Test: `./gradlew testScripts` passes. With a deliberately failing pytest test it fails. With `python3` removed from `PATH` it prints the message.
- CLAUDE.md: `./gradlew testScripts`, the fact that `check` includes it, and the note that the repo `venv/` is broken.

### [x] 2. (PR #120) `refactor/neutral-console-icon-ids`: no brand names in `icons/`
User decision, 2026-09-26: rename to neutral ids. This conflicts with the CLAUDE.md naming rule, which never covered `icons/`.
- **Rename:** all 137 files in `icons/images/` become neutral ids, `platform_001.png` … `platform_137.png`, in `git mv` order, so history follows each file.
- **New data file, `icons/images/platform_icons.json`:**
  - maps each `platform_id` (the same ids `default_settings.json` uses) to its neutral file, for the platforms the table in `master_icon.py` covers today
  - `CORE_TO_PLATFORM` moves into the same file
  - like `config.json` / `default_settings.json`, it's the one place where platform and core ids live, and nothing in code or docs quotes them
- **Code:**
  - `master_icon.py` loads the JSON; `CONSOLE_ICONS` and `CORE_TO_PLATFORM` are removed from the code
  - comments, log lines and `--help` get neutral wording
- **Unmapped images:** about 125 of the 137 images aren't used by any mapping. Default: rename and keep them, and ask the user before deleting any.
- **Tests,** in pytest and run by `testScripts`:
  - every entry in `platform_icons.json` points at an existing file
  - every file in `icons/images/` matches `platform_\d{3}\.png`
  - `fetch_console_fallback` resolves each mapped platform through the JSON, and returns `None` for an unknown one
  - `determine_platform` core fallback reads the JSON
  - a guard that no tracked path under `icons/` contains a name from a small denylist, kept in the test file as hashes so the test itself names nothing
- Before and after the rename, `./gradlew generateIcons` must produce the same launcher icons for the configured ROM when the console fallback is forced (`--force 3`). Compare the PNG hashes.

### [x] 3. (PR #121) `test/icon-scripts-pure-logic`: `master_icon` logic, `generate_typo`, image helpers
Add the path seams in the same PR as the tests that need them:
- `parse_config_xml(config_path, default_settings_path=None)`, which by default derives the path from the config tree.
- `generate_android_icons(img, res_dir=...)`. The default stays the real `res`; tests pass a temp directory.

Tests, in `icons/tests/test_master_icon.py` and `test_generate_typo.py`:
- **`parse_config_xml`**, over temp config trees:
  - `default_settings` false → `core` from the config
  - `default_settings` true → profile chosen by `platform`, with fallback to the ROM extension
  - `config_manual.json` merge wins
  - missing file or malformed JSON → `("", "")`
  - surrounding whitespace is stripped
- **`determine_platform`:**
  - extension map
  - the `.bin` special case (the core name picks between two platforms)
  - core fallback with `_libretro` / `_libretro_android` suffixes
  - `unknown`
  - pin the current substring matching of `CORE_TO_PLATFORM` (a short key can match inside a longer core name)
  - use neutral core names where possible
- **`generate_typo`:**
  - `extract_initials`: at most 3; `?` for empty input or punctuation only
  - `generate_stable_color`: same color for the same name, always a palette entry
  - `generate_typo_icon`: 512×512 RGBA, background from the palette, dark text on the light palette entry
- **Image helpers:**
  - `make_perfect_square`: a square input comes back unchanged; w≠h becomes max×max with the original pasted at the center
  - `make_squircle_image` / `make_round_image`: corners transparent, center opaque
- **`generate_android_icons` into a temp `res`:**
  - every density folder has the 4 PNGs at the right sizes (48/72/96/144/192; adaptive layers ×2.25)
  - both `mipmap-anydpi-v26` XMLs reference `@mipmap/ic_launcher_background` / `_foreground`
- **`fetch_console_fallback`:** unknown platform → `None`; known platform with a missing file → `None`. Point it at a temp images directory; don't depend on the real image files.

### [x] 4. (PR #122) `test/icon-fetchers-mocked-network`: `fetch_icon` / `fetch_smart`, no network
- **First make the env read lazy.** Read `SGDB_API_KEY` / `IGDB_CLIENT_ID` / `IGDB_CLIENT_SECRET` at call time, not import time, and call `load_env()` from `main()` (or on first use). Add a regression test: importing either module doesn't touch `os.environ` or any `.env`.
- Fix the missing timeout at `fetch_smart.py:155`. Add a regression test asserting every `requests.get` / `requests.post` call receives `timeout=`, checked through a monkeypatched fake.
- Tests with monkeypatched `requests`:
  - `_safe_json` on non-JSON responses
  - a missing key returns `None` without a request
  - HTTP errors and `RequestException` return `None` or `[]`
  - `fetch_sgdb_icon` / `fetch_steamgriddb_icon` pick by ratio and size
  - `fetch_*_multiple_*` honor `limit` and skip broken images
  - `get_token` success and failure
  - the `interactive=True` paths with `input` monkeypatched
- Images come from `Image.new(...)` saved to `BytesIO`; no files and no network.

### [x] 5. (PR #123) `test/master-icon-main-and-web-gui`: orchestration and the web picker
- **`main()`,** with `PROJECT_ROOT` pointed at a temp tree, fetchers monkeypatched and `sys.argv` set. Pin the current behavior:
  - the cascade order SGDB → IGDB → console → typography, stopping at the first image
  - `--force 1..4`
  - `--skip-downloads`
  - missing core or ROM → exit 1
  - override lock for the same ROM → uses the cached icon and skips fetchers
  - lock for a different ROM → cache deleted, and the bare `except` replaced with a narrow one
  - `--gui-web` with `start_web_picker` monkeypatched to return `CANCEL` (exit 0), `CLEAR_OVERRIDE` (lock removed, cascade runs) or an image (lock written)
- **`web_gui`:**
  - `image_to_base64` (`None` → `""`; the thumbnail is at most 256 px)
  - `find_free_port` returns a bindable port
  - handler tests against a real server on `127.0.0.1` in a thread: GET renders the page with the provided images; POST with a selection sets the result and shuts the server down
  - `webbrowser.open` monkeypatched
- **Fix:** bind the picker to `127.0.0.1`, with a regression test on the server address.
- **Behavior change** (user decision, 2026-09-26): when every icon method fails, `main()` exits **non-zero** instead of only logging, so `generateIcons` fails the build.
  - Test with all four methods monkeypatched to return `None`: expect `SystemExit` with code ≠ 0.
  - Another test: the typography fallback alone still succeeds with exit 0. That fallback works offline, so in practice the build fails only on a real breakage.

### [x] 6. (PR #124) `feat/icon-auto-pick-cache`: stop calling the icon API on every build
User decision, 2026-09-26: cache the automatically picked icon per ROM.
- **How it works:**
  - After the cascade picks an image, save it as `icons/.cache/auto_icon.png` with the ROM in `auto_last_rom.txt`. Keep this separate from the web-picker override, which still wins.
  - On the next build, if the ROM matches, go straight to `generate_android_icons` with no network.
  - When the ROM changes, drop the stale cache.
  - A `--refresh` flag forces a new scrape. Document it, and `./gradlew generateIcons -PrefreshIcons` passes it through.
  - `--force N` also bypasses the cache.
- **Tests,** in a temp `PROJECT_ROOT` with fetchers monkeypatched to count calls:
  - first run scrapes and writes the cache
  - second run with the same ROM makes 0 fetcher calls
  - another ROM scrapes again and replaces the cache
  - `--refresh` and `--force` scrape
  - a web override beats the auto cache
  - a corrupt cache file is ignored and replaced
- `icons/.cache/` must stay gitignored. Check it with `git check-ignore`.

### [x] 7. (PR #125) `fix/pick-icon-sh-exit-status`: fix the shell script and add shell tests
- **Fix:**
  - `cd "$(dirname "$0")"`, so it works from any directory
  - pass Python's exit status through, and print "Selection complete!" only on success (`python3 … || exit $?`, or `set -e`, or `exec`)
- **Shell tests,** in pytest (`icons/tests/test_shell_scripts.py`) rather than bats, so there's no new toolchain. Run the script with `subprocess` and a stub `python3` on `PATH` that records its cwd and args and exits with `$STUB_EXIT`:
  - args are forwarded after `--gui-web`
  - the non-zero exit is propagated
  - it works from another cwd
  - with no `python3` on `PATH` → exit 1 with the error message
  - a guard that runs shellcheck (via `shellcheck-py`) on every tracked `*.sh` except the generated `gradlew`, and skips if it isn't installed
- The two bug tests must fail on the current script.

### [x] 8. (PR #126) `chore/raise-kover-floor`: coverage grew
- The floor is 72% lines and 56% branches; the baseline is now 74.0% / 58.5%. Raise it to about 73 / 57, per CLAUDE.md ("when coverage grows, raise the floor").
- Test: `./gradlew check` passes at the new floor.

## Decisions (answered 2026-09-26)
- **Exit code when every icon method fails:** exit non-zero → item 5.
- **Network on every build:** cache the automatically picked icon per ROM → item 6.
- **Gradle wiring for pytest:** run through Gradle (`testScripts`, part of `check`) → item 1.
- **Brand names in the console-icon mapping and the `icons/images/` filenames:** rename to neutral ids → item 2. Why they existed: the images kept their download filenames on 2026-02-28, and the naming rule came on 2026-09-03.

## Known false positives (no change needed)
- Lint `MissingPermission` on `Build.getSerial()` in `utils/LogSaver.kt`: the `SecurityException` is caught.
- Lint `UnspecifiedRegisterReceiverFlag` in `views/GameActivity.kt` (`registerPipReceiver`): `RECEIVER_NOT_EXPORTED` is passed on API 33+.
- ruff `S324` (md5) in `generate_typo.py`: it only picks a color, with no security use.
- ruff `S101` in `icons/tests`: pytest asserts.
