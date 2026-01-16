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

Here's how Revenger is configured:

```
└── Revenger
    ├── rom
    ├── save
    ├── state
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
- Edit `app/src/main/res/values/config.xml` and change your configuration
- Copy your ROM to `app/src/main/res/raw/` (filename should match `config_rom` in config.xml)

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

**Expected APK size:** ~60MB (includes LibretroDroid cores)

# Autogen Tool
Revenger has a directory called `autogen` which contains a basic script to batch-generate Revenger packages. To use it, simply navigate to this folder. Place your ROMs in the `input` folder. In this same folder, put a `config.xml` file with your preferred configuration for these ROMs. Ignore the ID and NAME fields, as they will be overwritten. The script also supports nested folders, in which each can contain their own configuration file. Execute the script with `python generate.py`.

# Recent Updates - RetroMenu3 Refatoração

## Sistema de Menus Refatorado ✅

A partir da versão atual, o Revenger conta com um sistema de menus completamente refatorado baseado em **Command Pattern + State Machine**:

### 🏗️ Arquitetura Unificada
- **Command Pattern**: MenuAction sealed class para comandos type-safe
- **State Machine**: MenuState enum centralizando navegação
- **Interface Unificada**: MenuFragment padronizando comportamento de menus
- **MenuManager**: Coordenação central de todos os menus

### 🎯 Melhorias Implementadas
- **Manutenibilidade**: Código organizado e fácil de extender
- **Performance**: Eliminação de duplicação (~100 linhas reduzidas)
- **Testabilidade**: Testes unitários abrangentes
- **Compatibilidade**: Backward compatibility mantida

### 📱 Controles de Menu
- **RetroMenu3**: Ativado com `SELECT + START`
- **Navegação**: DPAD para navegar, A/B para confirmar/cancelar

### 🧪 Status de Testes
- ✅ **Unit Tests**: 50 tarefas passando
- ✅ **Build**: Compilação limpa e rápida
- ✅ **Runtime**: Inicialização <1 segundo
- ✅ **Device**: Testado em emulador Android

### 📚 Documentação Técnica
Consulte `docs/FASE6_FINALIZACAO_TESTES.md` para detalhes completos da refatoração.

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

### Contributors

- **tytydraco** - Original Ludere project creator and maintainer
- **vinaooo** - Revenger fork maintainer and modifications

---

**© 2023 tytydraco (Original Ludere Project)**  
**© 2025 vinaooo (Revenger Modifications)**

*This project complies with GNU GPL v3.0 license requirements for derivative works.*
