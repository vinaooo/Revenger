# XML to JSON Refactoring Plan — [app/src/main/res/values/](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values)

## Background

The Revenger project currently stores all its application configuration, gamepad settings, menu UI parameters, and UI strings as Android XML resources in [app/src/main/res/values/](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values). The goal is to migrate these to JSON files stored in `app/src/main/assets/`, making them more portable, easier to edit, and consistent with the existing `optimal_settings.json` pattern.

---

## File Classification

### ✅ Migratable to JSON (6 files)

| File | Category | Consumers |
|------|----------|-----------|
| `config.xml` | Build/identity config | `AppConfig.kt`, `build.gradle` (Groovy parser), fragments, utilities |
| `config_manual.xml` | Gameplay settings | `AppConfig.kt` exclusively |
| `gamepad.xml` | Controller styling | `GamePadConfig.kt`, `GamePadAlignmentManager.kt`, `GameActivity.kt` |
| `retro_menu3_colors.xml` | Menu colors | ~15 UI fragment/manager files via `R.color.rm_*` |
| `retro_menu3_dimens.xml` | Menu dimensions | Layout XML files, `MenuViewManager.kt`, `MenuLayoutConfig.kt` |
| `retro_menu3_strings.xml` | Menu strings | ~20 UI fragment files via `R.string.*` |

### ❌ Must Stay as XML (3 files — Android framework requirements)

| File | Reason |
|------|--------|
| `ids.xml` | Defines Android View IDs (`R.id.*`) — framework dependency |
| `retro_keyboard_styles.xml` | Defines `<style>` — only resolvable by Android resource system |
| `splash_theme.xml` | Defines `<style>` — referenced in `AndroidManifest.xml` |

---

## User Review Required

> [!IMPORTANT]
> **Qualifier overrides**: `values-port/retro_menu3_dimens.xml` contains portrait-specific dimension overrides. After migrating to JSON, we'll need to handle orientation-awareness in code (e.g., checking `Configuration.orientation` and loading the right JSON section). The same applies to `values-v31/splash_theme.xml`, but that file stays as XML.

> [!WARNING]
> **Build-time dependency**: `build.gradle` reads `config.xml` at Gradle configuration time using a Groovy XML parser (`getConfigValue` function). This must be updated to read JSON instead. The `generateIcons` Gradle task also passes `config.xml` path to a Python script — that script will need updating too.

> [!CAUTION]
> **`retro_menu3_colors.xml` colors are referenced in XML layouts** (via `@color/rm_*`). Migrating colors to JSON means we lose direct XML layout references. We have two options:
> 1. Keep `retro_menu3_colors.xml` as XML (simpler, no layout changes needed)
> 2. Move to JSON and set all colors programmatically (more consistent but requires layout changes)
>
> **Recommendation**: Keep colors as XML for now. They're tightly coupled to Android layouts and styles. Same reasoning applies to `retro_menu3_dimens.xml` — dimens referenced in layouts should stay XML.

---

## Revised Migration Scope

After considering the layout coupling, here is the **recommended priority**:

| Phase | Files | Risk | Complexity |
|-------|-------|------|------------|
| **Phase 1** | `config.xml` + `config_manual.xml` | Medium (build.gradle change) | Medium |
| **Phase 2** | `gamepad.xml` | Low | Low |
| **Phase 3** | `retro_menu3_colors.xml` + `retro_menu3_dimens.xml` | Higher (layout refs) | High |
| **Phase 4** | `retro_menu3_strings.xml` | Medium | Medium |

> [!NOTE]
> **Phases 3 and 4** are optional and carry higher risk due to deep integration with Android layouts and the resource system. I recommend starting with Phases 1–2 and evaluating whether Phases 3–4 are worthwhile based on the benefits seen.

---

## Proposed Changes

### Library: Gson

Add Google Gson to handle JSON parsing. It's already widely used in Android projects, lightweight, and has no additional transitive dependencies.

#### [MODIFY] [build.gradle](file:///home/vina/Projects/Emuladores/Revenger/app/build.gradle)
- Add `implementation 'com.google.code.gson:gson:2.11.0'` to dependencies
- Update `getConfigValue` function to read from JSON instead of XML

---

### Phase 1: `config.xml` + `config_manual.xml` → JSON

**Branch**: `refactor/config-xml-to-json`

#### [NEW] [config.json](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/assets/config.json)
Combined JSON structure:
```json
{
  "identity": {
    "name": "Não jogar",
    "core": "picodrive",
    "rom": "Sonic The Hedgehog (USA, Europe, Brazil) (En) (2).sms",
    "target_abi": "arm64-v8a",
    "platform": "",
    "load_bytes": false,
    "optimal_settings": true
  },
  "manual_settings": {
    "variables": "picodrive_overclk68k=+400%,...",
    "fast_forward_multiplier": 2,
    "fullscreen": true,
    "orientation": 2,
    "shader": "disabled",
    "menu_mode_fab": "bottom-right",
    "menu_mode_gamepad": true,
    "menu_mode_back": true,
    "menu_mode_combo": false,
    "gamepad": true,
    "gp_haptic": true,
    "gp_allow_multiple_presses_action": false,
    "gp_a": true, "gp_b": true, "gp_x": false, "gp_y": false,
    "gp_start": true, "gp_select": true,
    "gp_l1": false, "gp_r1": false, "gp_l2": false, "gp_r2": false,
    "left_analog": false,
    "performance_overlay": false
  },
  "fake_buttons": {
    "show_0": false, "show_1": false, "show_5": false,
    "show_6": false, "show_7": false, "show_9": false,
    "show_10": false, "show_11": false
  }
}
```

#### [NEW] [ConfigJson.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/models/ConfigJson.kt)
Data classes for Gson deserialization: `ConfigJson`, `IdentityConfig`, `ManualSettingsConfig`, `FakeButtonsConfig`.

#### [NEW] [ConfigRepository.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/repositories/ConfigRepository.kt)
Singleton that loads and caches `config.json` from assets. Provides typed getters for all fields.

#### [MODIFY] [AppConfig.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/AppConfig.kt)
Replace all `resources.getString(R.string.conf_*)`, `resources.getBoolean(R.bool.conf_*)`, `resources.getInteger(R.integer.conf_*)` calls with `ConfigRepository` calls.

#### [MODIFY] [build.gradle](file:///home/vina/Projects/Emuladores/Revenger/app/build.gradle)
Update `getConfigValue` closure to parse `config.json` instead of `config.xml`.

#### [MODIFY] Various UI files
Update direct `R.string.conf_*` references in:
- `ExitSaveGridFragment.kt`, `ExitFragment.kt`, `SaveSlotsFragment.kt`
- `AboutFragment.kt`, `ScreenshotCaptureUtil.kt`, `LogSaver.kt`
- `MenuViewManager.kt`, `MenuViewInitializer.kt`
- `GameActivityViewModel.kt`, `GameActivity.kt`

#### [DELETE] [config.xml](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values/config.xml)
#### [DELETE] [config_manual.xml](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values/config_manual.xml)

---

### Phase 2: `gamepad.xml` → JSON

**Branch**: `refactor/gamepad-xml-to-json`

#### [NEW] [gamepad_config.json](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/assets/gamepad_config.json)
```json
{
  "colors": {
    "button_color": "#88ffffff",
    "pressed_color": "#66ffffff"
  },
  "dimensions": {
    "padding_vertical_dp": 20
  },
  "offsets": {
    "portrait_percent": 100,
    "landscape_percent": 50
  }
}
```

#### [NEW] [GamepadConfigJson.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/models/GamepadConfigJson.kt)
Data classes for gamepad config.

#### [MODIFY] [GamePadConfig.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/gamepad/GamePadConfig.kt)
Replace `ContextCompat.getColor(context, R.color.gp_*)` with parsed colors from JSON.

#### [MODIFY] [GamePadAlignmentManager.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/gamepad/GamePadAlignmentManager.kt)
Replace `resources.getInteger(R.integer.gp_offset_*)` with JSON values.

#### [MODIFY] [GameActivity.kt](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/java/com/vinaooo/revenger/views/GameActivity.kt)
Replace `resources.getInteger(R.integer.gp_offset_*)` with JSON values.

#### [DELETE] [gamepad.xml](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values/gamepad.xml)

---

### Phase 3: RetroMenu3 Colors & Dimensions → JSON (Optional)

**Branch**: `refactor/retromenu-style-to-json`

> [!WARNING]
> This phase is **optional** and carries higher risk. Colors like `@color/rm_text_color` are referenced in XML layout files and styles (`retro_keyboard_styles.xml`). Moving to JSON requires setting all these programmatically. **I recommend deferring this phase** unless you have a strong motivation to remove XML color/dimen definitions.

---

### Phase 4: RetroMenu3 Strings → JSON (Optional)

**Branch**: `refactor/retromenu-strings-to-json`

> [!WARNING]
> This phase is also **optional**. The strings are accessed via `R.string.*` throughout ~20 files. Moving to JSON would require creating a centralized string provider class and updating many files. It's feasible but has wide blast radius.

---

## Verification Plan

### Automated Tests

**Phase 1** — New unit tests for `ConfigRepository`:

```bash
# Run after creating tests in app/src/test/java/com/vinaooo/revenger/repositories/
cd /home/vina/Projects/Emuladores/Revenger
./gradlew testDebugUnitTest --tests "com.vinaooo.revenger.repositories.ConfigRepositoryTest"
```

Test coverage:
- JSON parsing of all identity fields
- JSON parsing of all manual settings fields
- JSON parsing of fake buttons
- Fallback behavior when fields are missing

**Phase 2** — Unit tests for gamepad config loading:
```bash
./gradlew testDebugUnitTest --tests "com.vinaooo.revenger.models.GamepadConfigJsonTest"
```

### Build Verification
```bash
# Full build after each phase to ensure no R.* reference breaks
cd /home/vina/Projects/Emuladores/Revenger
./gradlew assembleDebug
```

### Manual Verification

After each phase, deploy to a device/emulator and verify:

1. **Phase 1**: App launches → game loads with correct config → menu shows correct game name → About screen shows correct ROM/core info → FAB menu button appears in correct position
2. **Phase 2**: Virtual gamepad visible → buttons have correct semi-transparent white color → gamepad offset positioning works in portrait and landscape

> [!NOTE]
> I'd appreciate your input on whether you have a preferred way to test on device, or if `./gradlew installDebug` followed by manual app launch works for you.
