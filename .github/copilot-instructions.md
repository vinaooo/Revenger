# Revenger - AI Coding Assistant Instructions

## Project Overview
Revenger is a LibRetro-powered ROM packager for portable Android emulation. It packages ROMs, LibRetro cores, and configurations into single APKs for seamless gaming without external dependencies.

## Architecture Overview
- **Single-Package Philosophy**: ROM + LibRetro core + config = one APK (no external ROM importing)
- **Configuration-Driven**: All app behavior controlled via `app/src/main/res/values/config.xml`
- **Dynamic Core Loading**: LibRetro cores downloaded during build from buildbot.libretro.com
- **MVVM Architecture**: Kotlin-based with ViewModels, LiveData, and modern Android patterns

## Key Components
- `GameActivity`: Main emulator activity handling lifecycle, permissions, and UI
- `RetroView`: LibretroDroid-powered emulator display surface
- `RadialGamePad`: Virtual touchscreen controls with customizable layouts
- `GameMenuFullscreenFragment`: Fullscreen overlay menu system
- `config.xml`: Central configuration file controlling ROM, core, UI, and behavior

## Critical Workflows

### Building APKs
```bash
# Standard Gradle builds
./gradlew assembleDebug          # Debug APK (~60MB with cores)
./gradlew assembleRelease        # Release APK
./gradlew clean assembleDebug    # Clean + debug build

# Install on device
./gradlew installDebug
```

### Configuration Setup
1. Edit `app/src/main/res/values/config.xml`:
   - `config_id`: Unique app identifier (no spaces/special chars)
   - `config_core`: LibRetro core name (gambatte, bsnes, genesis_plus_gx, smsplus)
   - `config_rom`: ROM filename in `res/raw/` (without extension)
   - `config_name`: Display name

2. Place ROM file in `app/src/main/res/raw/` matching `config_rom` value

3. Build - cores download automatically via `prepareCore` task

### Batch Packaging (Autogen)
```bash
# Setup input structure
mkdir -p autogen/input/{game1,game2}
# Place config.xml and ROMs in each subdirectory
./autogen/generate  # Generates individual APKs
```

## Project Conventions

### Configuration Patterns
- **XML-Driven**: Never hardcode ROM/core specific values - use config.xml
- **Resource Naming**: ROM files in `res/raw/` match `config_rom` exactly
- **Core Mapping**: gambatte=GameBoy, bsnes=SNES, genesis_plus_gx=MegaDrive, smsplus=MasterSystem

### Code Organization
- **Portuguese Comments**: Documentation in Brazilian Portuguese
- **SOLID Principles**: Clean architecture with single responsibility
- **Logging Over Print**: Use `Log.d(TAG, message)` instead of `println()` or `print()`
- **Backup System**: Store config templates in `config_backup/` with `_backup` suffix
- **Language**: Always write comments and commit messages in English


### Build System
- **Dynamic Dependencies**: LibRetro cores downloaded during `prepareCore` task
- **ABI Support**: armeabi-v7a, arm64-v8a, x86, x86_64, universal
- **Signing**: Release builds use `revenger.jks` (password: ludere, alias: key0)

## Integration Points

### External Dependencies
- **LibretroDroid** (0.12.0): LibRetro frontend - monitor for 16KB alignment updates
- **RadialGamePad** (2.0.0): Virtual controls with Kotlin Flow support
- **Material Design 3**: Theming with dynamic colors
- **LibRetro Cores**: Downloaded from buildbot.libretro.com nightly builds

### Android Features
- **SDK 36 Target**: Modern Android features with backward compatibility
- **Privacy Controls**: Enhanced permission handling
- **Performance Profiling**: Advanced frame rate monitoring
- **Immersive Mode**: Fullscreen gaming experience

## Common Patterns

### Error Handling
```kotlin
try {
    // Operation that might fail
} catch (e: Exception) {
    Log.e(TAG, "Operation failed", e)
    // Graceful degradation
}
```

### Configuration Reading
```kotlin
// In build.gradle - use getConfigValue() function
applicationId "com.vinaooo.revenger.${getConfigValue('config_id')}"

// In Kotlin - read from resources
val romName = resources.getString(R.string.config_rom)
```

### Menu System
- Fullscreen Fragment overlay (not DialogFragment)
- Touch-to-dismiss on screen edges
- Material 3 theming with dynamic colors
- START button closes menu when menu is open, passes to core when menu is closed

## Development Notes
- **16KB Alignment Issue**: Monitor LibretroDroid for updates (currently using workaround)
- **Core Compatibility**: Test new cores thoroughly - some may have different APIs
- **Performance**: Frame rate monitoring enabled in debug builds
- **Privacy**: All storage operations require explicit permissions

## File Structure Reference
```
app/src/main/
├── java/com/vinaooo/revenger/     # Main app code
│   ├── views/GameActivity.kt      # Main activity
│   ├── retroview/                 # Emulator display
│   ├── gamepad/                   # Virtual controls
│   ├── ui/                        # UI components
│   │   └── menu/                   # Menu fragments (RetroMenu3)
│   └── views/                      # Activity classes
├── res/
│   ├── raw/                       # ROM files
│   ├── values/config.xml          # App configuration
│   └── layout/                    # UI layouts
autogen/                           # Batch packaging tool
config_backup/                     # Configuration templates
```

## Testing
- Manual testing required due to hardware-specific emulation
- Test on multiple Android versions (minSdk 30, targetSdk 36)
- Verify core downloads and ROM loading
- Check save state functionality</content>
<parameter name="filePath">/home/vina/Projects/Emuladores/Revenger/.github/copilot-instructions.md