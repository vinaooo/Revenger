# Maintenance and Development Guide

## Overview
Essential principles and practices for RetroMenu3 development. This document was updated to reflect the completion of the navigation refactor described in `docs/NAVIGATION_REFACTORING_PLAN.md`.

**Last Updated**: October 2025 **Version**: 2.0 - RetroMenu3 Complete Refactoring
**Last Updated**: January 2026 **Version**: 2.1 - Navigation Refactor Applied

## High-level summary of the navigation refactor
- The original `NavigationController` has been decomposed into two focused classes: `NavigationStateManager` (state ownership) and `NavigationEventProcessor` (event processing and business rules). This reduces complexity, improves testability and follows single-responsibility principles.
- Duplication and critical TODOs in menu-related controllers were resolved (see `docs/NAVIGATION_REFACTORING_PLAN.md`).
- Public menu APIs and fragment interfaces were preserved to maintain backward compatibility with the rest of the codebase.

## UI Design Principles
The RetroMenu3 system does **NOT** use and should **NOT** use Material Design, Material You, or Material You Expressive. Following the successful migration completed in October 2025, the UI exclusively uses custom **RetroCardView** components with retro styling that resembles old video games.

### UI Characteristics
- Retro fonts and pixel-perfect styling
- Navigation via touch DPAD or USB gamepad
- Confirmation via START/SELECT or touch
- `RetroCardView` with transparent backgrounds
- No Material Design theming or components

## Core Principles (updated)
- **SOLID**: Enforced across navigation subsystems; state and event responsibilities separated
- **Reactive**: `StateFlow` remains the canonical state carrier for UI
- **Composition**: Continue to prefer small managers (`MenuActionHandler`, `SubmenuCoordinator`, `MenuViewManager`, `NavigationStateManager`, `NavigationEventProcessor`)

## Navigation Subsystem Responsibilities
- `NavigationStateManager`: owns current menu, selected index, navigation stack and provides safe state mutation APIs.
- `NavigationEventProcessor`: receives raw input/events, applies debouncing/rate-limiting, validates actions and delegates state mutations to `NavigationStateManager`.
- `MenuViewModel` (or equivalent ViewModel) observes `NavigationStateManager` for UI updates; UI code does not directly mutate internal navigation structures.

## Key Techniques
- **Animations**: `ViewPropertyAnimator` + object pools for performance
- **Configuration**: `MenuConfigurationBuilder` for dynamic menus
- **Logging**: Use conditional logging through project `MenuLogger` / `Log.d` in debug; no `print`/`println`

## Architecture
```
RetroMenu3Fragment (UI)
├── MenuViewModel (StateFlow)
├── MenuSystem (Logic)
│   ├── MenuActionHandler (Actions)
│   ├── SubmenuCoordinator (submenus)
│   ├── NavigationEventProcessor (input -> events)
│   ├── NavigationStateManager (state ownership)
│   └── MenuViewManager (UI Updates with RetroCardView)
├── RetroCardView (Custom UI Component)
├── AnimationOptimizer (Performance)
└── Test Suite (Unit + Integration + Robolectric)
```

### Component Standards
- `RetroCardView`: custom card component used across all menus
- No external UI libraries or Material components
- Pixel-perfect, arcade-style visuals

## Testing Requirements
- **Unit tests**: add/maintain tests for `NavigationStateManager` and `NavigationEventProcessor`
- **Integration tests**: validate end-to-end menu flows (`RetroMenu3IntegrationTest`) including debouncing and concurrent events
- **Robolectric**: keep framework-level interaction tests for fragment lifecycle
- **Performance benchmarks**: ensure no regressions in menu responsiveness after refactor

### Testing Implementation
- **JUnit 4/5**, **Robolectric 4.11.1**, **MockK** and CI via **GitHub Actions** remain the standard
- Add targeted unit tests that assert single responsibility: state mutations exclusively in `NavigationStateManager`, event handling logic exclusively in `NavigationEventProcessor`

## Code Standards
- PascalCase for classes, camelCase for methods/variables
- KDoc for public APIs
- No `print`/`println` — use `android.util.Log` or project logger
- Immutable state objects where practical

## Maintenance Rules (refined)
1. **Preserve UI/UX**: Retain retro styling and `RetroCardView` appearance
2. **Backward compatibility**: Preserve public fragment/viewmodel contracts
3. **Test coverage**: New classes must have unit + integration tests
5. **Performance verification**: Benchmark menu responsiveness after changes
6. **Consistency**: Use `NavigationStateManager` for state reads/writes; do not mutate state directly from UI
7. **Code review**: Peer review required for navigation or UI changes
8. **No Material Design**: Reintroducing Material Design is forbidden

## Migration History

### Material Design Migration (October 2025) ✅ COMPLETED
**Objective**: Complete removal of Material Design dependencies and implementation of custom RetroCardView
### Navigation Refactor (January 2026) ✅ COMPLETED
**Objective**: Reduce complexity of `NavigationController` by extracting state and event responsibilities into dedicated classes.

**Outcomes**:
- `NavigationController` complexity reduced by decomposition
- New classes: `NavigationStateManager`, `NavigationEventProcessor`, `MenuManagerFactory` (where applicable)
- Duplicate `init` blocks and critical TODOs removed
- Tests added/updated for navigation responsibilities

**Technical Achievements**:
- Custom UI component architecture
- Robolectric integration testing
- Zero breaking changes
- Performance optimization maintained
- Complete test suite validation

## Development Guidelines (concise)
- Prefer composition for extending behavior
- Keep reactive `StateFlow` for UI-observable state
- Write small, focused unit tests for each manager
- Document architectural decisions in `docs/`

## Best Practices (summary)
- **SOLID**, **Reactive state**, **RetroCardView only**
- **Optimized animations** using object pools
- **Comprehensive testing**: Unit + Integration + Robolectric
- **Consistent logging** (no prints)
- **Incremental, tested changes** with code review

For implementation details and the step-by-step refactor plan, see: `docs/NAVIGATION_REFACTORING_PLAN.md`.

---

## Dependencies and Libraries

### Core Emulation
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **LibretroDroid** | 0.13.1 | LibRetro frontend & emulation engine | Provides GLRetroView, core loading, and viewport API |
| **LibRetro Cores** | Latest (buildbot) | Emulation cores (gambatte, bsnes, genesis_plus_gx, smsplus, picodrive) | Downloaded dynamically during build from buildbot.libretro.com |

### Game Controls & Input
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **RadialGamePad** | 2.0.0 | Virtual touchscreen controls | Custom gamepad with radial layout, Kotlin Flow-based |
| **AndroidX GameController** | Latest | Physical gamepad support | Standard Android gamepad API integration |

### UI & Graphics
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **AndroidX AppCompat** | Latest | UI compatibility layer | Minimal Material Design (being phased out) |
| **Material Design 3** | Latest | Dynamic color theming | Theme customization only; no Material components used |
| **Custom RetroCardView** | In-house | Retro-styled card component | Custom implementation for arcade/retro UI |
| **Kotlin Coroutines** | Latest | Asynchronous operations | Used for ROM loading, core initialization |

### Android Framework
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **AndroidX Core** | Latest | Core functionality (Activities, Fragments) | Fragment lifecycle, permissions handling |
| **AndroidX Lifecycle** | Latest | LiveData & ViewModel | Reactive state management |
| **AndroidX Navigation** | Latest | Fragment navigation | Menu navigation within the app |
| **AndroidX ConstraintLayout** | Latest | Layout system | Efficient UI layout management |

### Configuration & Data Binding
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **Data Binding** | Built-in | XML-based data binding | Used for config reading (game_scale.xml, config.xml) |
| **Kotlin Serialization** | Latest | Configuration serialization | ROM metadata and core settings |

### Testing
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **JUnit 4** | Latest | Unit testing framework | Core testing for managers and state |
| **JUnit 5** | Latest | Advanced unit testing | Parameterized tests support |
| **Robolectric** | 4.11.1 | Android framework testing | Fragment lifecycle, UI component testing |
| **MockK** | Latest | Kotlin mocking library | Mock GameController, LibRetro APIs |
| **AndroidX Test** | Latest | Android instrumented tests | Device-specific emulation testing |

### Build System
| Tool | Version | Purpose | Notes |
|------|---------|---------|-------|
| **Gradle** | 8.x+ | Build automation | Dependency resolution, APK packaging |
| **Android Gradle Plugin** | Latest | Android-specific build tasks | APK signing, resource compilation |
| **Kotlin Gradle Plugin** | Latest | Kotlin compilation | Source compilation, test compilation |

### Signing & Release
| Tool | Version | Purpose | Notes |
|------|---------|---------|-------|
| **jarsigner** | JDK built-in | APK signing | Release APK signing with revenger.jks |
| **zipalign** | Android SDK | APK optimization | Memory alignment optimization |

### Logging & Debugging
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| **Android Logging (Log)** | Built-in | Debug logging | Used for gameplay debug info, viewport application logs |
| **Logcat** | Built-in | Runtime log capture | ADB logging for debugging |

### Build Configuration
| Setting | Value | Purpose |
|---------|-------|---------|
| **SDK Target** | 36 (Android 15) | Latest Android features |
| **Min SDK** | 30 (Android 11) | Backward compatibility |
| **Java Version** | 17 | Modern Java features |
| **Kotlin Version** | Latest (2.x) | Latest Kotlin features |

### Architecture-Specific Builds
| ABI | Status | Notes |
|-----|--------|-------|
| **armeabi-v7a** | Supported | 32-bit ARM processors |
| **arm64-v8a** | Supported | 64-bit ARM processors |
| **x86** | Supported | Intel x86 emulators |
| **x86_64** | Supported | Intel x86_64 emulators |
| **universal** | Supported | All ABIs in single APK |

### Dependency Management
- **Centralized versioning**: Use `gradle.properties` for version definitions
- **Dynamic core downloads**: LibRetro cores fetched from buildbot during build
- **Minimal external dependencies**: Prefer Android Framework APIs when available
- **Kotlin-first**: Leverage Kotlin stdlib and coroutines for concurrency

### Notable Version Changes
- **LibretroDroid 0.12.0 → 0.13.1** (January 2026): Added viewport API support for game screen inset feature
- **Kotlin 1.x → 2.x** (Latest): Improved performance and language features
- **Target SDK 34 → 36**: Compliance with latest Android requirements

### Dependency Security
- Regular updates via Dependabot (when available)
- Security patches applied immediately
- Verification of library sources (official repositories only)

### Performance Considerations
- **LibRetro cores**: JNI overhead minimized with optimized native bindings
- **RadialGamePad**: Kotlin Flow-based reactive updates reduce garbage collection
- **Memory pooling**: Animation object pools prevent GC pauses
- **Viewport calculations**: Normalized coordinates (0.0-1.0) avoid floating-point precision issues