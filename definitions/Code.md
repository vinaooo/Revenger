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