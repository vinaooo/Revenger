# Revenger
A LibRetro-powered ROM packager for portable emulation
This is a try to keep this project updated, but my focus at this moment is my personal use

## About This Project

**Revenger** is a modified version of the original [Ludere](https://github.com/tytydraco/Ludere) project, created by [tytydraco](https://github.com/tytydraco). This project is distributed under the same GNU General Public License v3 (GPLv3) as the original work.

### Original Project Credits
- **Original Project**: Ludere  
- **Original Author**: tytydraco
- **Original Repository**: https://github.com/tytydraco/Ludere  
- **License**: GNU General Public License v3.0

### License Compliance
As required by the GNU General Public License v3, this modified version:
- ✅ Retains the original GPL license
- ✅ Provides clear attribution to the original author
- ✅ Documents all modifications made
- ✅ Makes source code freely available
- ✅ Preserves all original license notices

The complete license text is available in the [LICENSE](LICENSE) file.

# Philosophy
The current state of emulation on Android is excellent relative to other methods of emulation. However, the experience is not **seamless** and it is not **universal**. Allow me to elaborate. By seamless, I mean to say that there are very few steps involved between opening the application and actually playing the game. With most emulators from a fresh install, one must open the application, download a core (i.e. RetroArch), locate their ROM, and then begin playing, totally at least two steps of interference. Contrarily, Revenger reduces the process down to one simple step: open the application. The core, ROM, controls, core settings, and everything else are already configured. In terms of universality, one cannot easily duplicate their configuration across devices without repeating the steps for each device. Instead, Revenger is a simple APK with all configuration already prepared, so installing an exact duplicate of the game is as easy as installing any other APK.

# Purpose
The goal of Revenger is to increase the level of abstraction for emulation on Android.

Here's a diagram of how most Android emulators are configured:

```
└── Generic Emulator App
    ├── Roms
    │   ├── rom1.gba
    │   ├── rom2.gba
    │   └── rom3.gba
    ├── Saves
    │   ├── rom1.sav
    │   ├── rom2.sav
    │   └── rom3.sav
    └── States
        ├── rom1.state
        ├── rom2.state
        └── rom3.state
```

Here's how Revenger is configured with the new multi‑slot system:

```
└── Revenger
    ├── rom                 # packaged game data
    ├── save                # SRAM and game-specific files
    │   ├── slot1.sav       # first save slot
    │   ├── slot2.sav       # second save slot
    │   ├── slot3.sav       # third save slot
    │   └── slotN.sav       # …multiple slots supported
    ├── state               # save‑state directory (one file per slot)
    │   ├── slot1.state
    │   ├── slot2.state
    │   └── slotN.state
    └── *other system files*
```

# Features
- LibRetro core is fetched once on the first launch
- ROM is packaged inside the APK, no external importing required
- Save state support (single slot)
- SRAM is saved when the application loses focus
- All-in-one package, can be easily distributed once packaged

# Libraries
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid): Our LibRetro frontend that interacts with RetroArch cores
- [RadialGamePad](https://github.com/Swordfish90/RadialGamePad): Intuitive touchscreen controls
- [LibRetro](http://buildbot.libretro.com/nightly/): Emulator cores for Android

# Configuration
- Edit `app/src/main/assets/config/config.json` and change your configuration
- Place your ROM files in `roms_backup/` at the project root (the build system automatically stages the active ROM based on `conf_rom` in config.xml)

## Default Settings System

Revenger now supports an **default settings mode** that automatically configures the emulator based on the platform of the ROM. When enabled, the APK chooses the best LibRetro core, gamepad layout, orientation, shaders and other preferences by inspecting the ROM extension (or an explicit `conf_platform` tag). This eliminates manual tweaking and makes packaging one‑tap ready games trivial.

### Enabling
Add the following tags in `app/src/main/assets/config/config.json`:

```xml
<bool name="conf_default_settings">true</bool>
<string name="conf_platform"/><!-- optional, used when extension is ambiguous -->
```

`conf_default_settings` is a **build‑time flag**. When `false` (the default) Revenger behaves exactly as before, using values directly from `config.xml`.

### How it works
1. On build, the Gradle `prepareCore` task reads the default settings JSON (`app/src/main/assets/default_settings.json`).
2. It resolves a profile by `conf_platform` or ROM filename extension and selects the corresponding core for download.
3. At runtime, an `AppConfig` facade returns either the original config.xml values or overrides from the profile. All components (RetroView, GamePad, controllers, etc.) query `AppConfig` instead of resources directly.

### Supported platforms
The JSON currently includes profiles for: **Master System (sms/gg)**, **Mega Drive (md)**, **Super Nintendo (snes)**, **Game Boy/Color (gb/gbc)**, **Game Boy Advance (gba)** and **NES (nes)**. Adding new platforms is as simple as editing the JSON and optionally adjusting aspect ratio mappings.

### Benefits
- ✅ One‑tap packaging – just install and play
- ✅ Optimized core and variable presets per system
- ✅ Build‑time validation prevents incorrect cores
- 🚫 Zero impact when disabled (legacy configs still work)

For full details see [docs/default_settings.md](docs/default_settings.md).

## Shader Configuration
Revenger supports configurable video shaders for enhanced visual experience:

### Available Shaders
- **`disabled`**: No shader applied (raw output) - Best performance
- **`sharp`**: Sharp bilinear filtering (default) - Balanced quality/performance  
- **`crt`**: CRT monitor simulation - Retro gaming aesthetic
- **`lcd`**: LCD matrix effect - Modern display simulation

### Configuration
Set the desired shader in `config.xml`:
```xml
<string name="config_shader">sharp</string>
```

### Performance Impact
- **Disabled**: Minimal GPU usage
- **Sharp**: Light filtering, negligible impact
- **CRT/LCD**: Moderate GPU usage, may reduce FPS on low-end devices

### Recommendations
- Use **Sharp** for most games (default)
- Use **Disabled** for maximum performance
- Use **CRT** for retro gaming experience
- Use **LCD** for modern aesthetic

# Auto-Generated Icons
Revenger features an automated script system to generate all required Android app icons (including adaptive icons for Android 8.0+) based on the chosen console/emulator core.

## How it works
The scripts are located inside the `icons/scripts/` directory:
1. **`master_icon.py`**: The main controller. It reads your `config.xml` to determine what emulator core is currently configured. It finds the matching console icon, layers it onto a background shape, scales it to 108dp (for adaptive padding) and standard sizes (`mdpi` up to `xxxhdpi`), and outputs them into the `app/src/main/res/` mipmap directories. It also generates the `mipmap-anydpi-v26` XML definitions automatically.
2. **`generate_typo.py`**: A fallback system that generates a beautifully styled text-based icon if a matching console graphic cannot be found.
3. **`utils.py`**: Contains shared utility logic like environment loading and name normalization.

To regenerate icons manually, ensure you have Python and `Pillow` installed, and execute:
```bash
python3 icons/scripts/master_icon.py
```

## Icon Assets & References
The console graphics located in the `icons/images/` directory are officially sourced from the **[Libretro / RetroArch Assets](https://github.com/libretro/retroarch-assets)** repository. We use these clean, monochrome console illustrations to instantly brand the Revenger APK for the specific system it is emulating.

# Building
Use standard Gradle commands for building:

```bash
# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Install on connected device
./gradlew installDebug
```

The generated APK will be available at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-universal-release.apk`


# Recent Updates - RetroMenu3 Refactoring

## Refactored Menu System ✅

Starting with the current version, Revenger features a completely refactored menu system based on **Command Pattern + State Machine**:

### 🏗️ Unified Architecture
- **Command Pattern**: MenuAction sealed class for type-safe commands
- **State Machine**: MenuState enum centralizing navigation
- **Unified Interface**: MenuFragment standardizing menu behavior
- **MenuManager**: Central coordination of all menus

### 🎯 Improvements Implemented
- **Maintainability**: Organized, easy-to-extend code
- **Performance**: Duplication eliminated (~100 lines reduced)
- **Testability**: Comprehensive unit tests
- **Compatibility**: Backward compatibility maintained


---

# Keystore
There is a keystore for signing Revenger packages that is public and free to use. Here are the details you should know when signing with it:

- Keystore Password: `ludere`
- Key Alias: `key0`
- Key password: `ludere`

## License and Attribution

### GNU General Public License v3.0

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

### Original Work Attribution

This project is based on **Ludere** by **tytydraco**:
- Original repository: https://github.com/tytydraco/Ludere
- Licensed under GNU GPL v3.0
- All original copyright notices have been preserved

### Asset Attribution
Console icons and graphics used for automatically generated app icons (`icons/images/`) are sourced from the **[libretro/retroarch-assets](https://github.com/libretro/retroarch-assets)** repository.
- **License**: [Creative Commons Attribution 4.0 International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/)
- **Authors**: The Libretro Team and contributors. 

### Contributors

- **tytydraco** - Original Ludere project creator and maintainer
- **vinaooo** - Revenger fork maintainer and modifications

---

**© 2023 tytydraco (Original Ludere Project)**  
**© 2025 vinaooo (Revenger Modifications)**

*This project complies with GNU GPL v3.0 license requirements for derivative works.*
