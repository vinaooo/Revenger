# RetroMenu3 - Refactored Architecture and Development Guide

## Overview

The `RetroMenu3Fragment` was completely refactored following SOLID principles, especially the **Single Responsibility Principle (SRP)**. The previous architecture with 1072 lines in a single file was divided into specialized and decoupled classes.

**Refactoring Date**: October 2025
**Architecture**: Composition over Inheritance - Coordinator Pattern
**Lines of Code**: ~407 (62% complexity reduction)
**Classes Created**: 7 specialized managers

**Last Updated**: October 2025
**Version**: 3.0 - Post Complete Refactoring

## UI Design Principles
The RetroMenu3 system does **NOT** use and should **NOT** use Material Design, Material You, or Material You Expressive. Following the successful migration completed in October 2025, the UI exclusively uses custom **RetroCardView** components with retro styling that resembles old video games.

### UI Characteristics
- Retro fonts and pixel-perfect styling
- Navigation via touch DPAD or USB gamepad
- Confirmation buttons on touch or USB gamepads
- Direct touch control (finger touch on options)
- Custom RetroCardView with transparent backgrounds
- No Material Design shadows, corners, or theming

## Current Architecture - Coordinator Pattern

The `RetroMenu3Fragment` now acts as a **coordinator** that delegates specific responsibilities to specialized managers, following the principle of **composition over inheritance**.

```
RetroMenu3Fragment (Coordinator)
├── MenuLifecycleManager     (Lifecycle)
├── MenuInputHandler         (Input/Navigation)
├── MenuStateController      (Selection State)
├── MenuViewInitializer      (View Setup)
├── MenuAnimationController  (Animations)
├── MenuCallbackManager      (Callbacks)
├── MenuActionHandler        (Complex Actions)
└── SubmenuCoordinator       (Submenu Coordination)
```

### Architecture Components

#### 1. RetroMenu3Fragment (Coordinator)
- **Responsibility**: General coordination and dependency injection
- **Main Methods**:
  - `initializeManagers()`: Dependency injection
  - `dismissMenu()`: Dismiss coordination with protection
  - `showMainMenu()`: Main menu display coordination

#### 2. MenuLifecycleManager
- **Responsibility**: Fragment lifecycle management
- **Methods**: `onCreateView()`, `onViewCreated()`, `onResume()`

#### 3. MenuInputHandler
- **Responsibility**: User input processing
- **Methods**: `performNavigateUp/Down()`, `performConfirm()`, `performBack()`

#### 4. MenuStateController
- **Responsibility**: Selection state management
- **State**: Current selected index
- **Methods**: `selectIndex()`, `selectNext/Previous()`, `updateSelectionVisual()`

#### 5. MenuViewInitializer
- **Responsibility**: View initialization and configuration
- **Methods**: `initializeViews()`, `configureInitialViewStates()`

#### 6. MenuAnimationController
- **Responsibility**: Animation management
- **Methods**: `animateMenuIn/Out()`, `animateItemSelection()`, `dismissMenu()`

#### 7. MenuCallbackManager
- **Responsibility**: Callback delegation to GameActivity
- **Methods**: All `RetroMenu3Listener` methods

#### 8. MenuActionHandler
- **Responsibility**: Complex menu action coordination
- **Methods**: `executeNavigate()`, `executeAction()`, `executeConfirm()`

### Applied Design Principles
- **SOLID**: Single responsibility, open/closed, Liskov, interface segregation, dependency inversion
- **Composition over Inheritance**: Fragment coordinates, does not implement
- **Dependency Injection**: Manual dependency injection
- **Immutability**: Immutable states where possible
- **Testability**: Each class testable in isolation

## Testing Requirements
- **Unit tests** for all managers (MenuCallbackManagerTest, MenuManagersCreationTest)
- **Integration tests** for workflows (RetroMenu3IntegrationTest)
- **Performance benchmarks** (MenuPerformanceBenchmarkTest)
- **UI validation** for visual states and RetroCardView rendering

### Testing Implementation
- **JUnit 4/5** for unit and integration tests
- **MockK** for dependency mocking
- **GitHub Actions** for CI/CD validation
- **Current Coverage**: 4 test files implemented
- **Performance**: MenuPerformanceBenchmark with automated tests

### Current Test Files
- `MenuCallbackManagerTest.kt`: Callback delegation tests
- `MenuManagersCreationTest.kt`: Manager creation tests
- `RetroMenu3FragmentTest.kt`: Main fragment tests
- `RetroMenu3IntegrationTest.kt`: End-to-end integration tests
- `MenuPerformanceBenchmarkTest.kt`: Performance tests

## Code Standards
- PascalCase classes, camelCase methods/variables
- KDoc for public APIs
- No print statements - use logging
- Immutable state objects

## Maintenance Rules
1. **Coordinator Pattern**: Fragment coordinates, does not implement logic
2. **Manager Specialization**: Each manager has single responsibility
3. **Dependency Injection**: Use `initializeManagers()` for wiring
4. **Preserve existing UI/UX**: Maintain retro styling and RetroCardView appearance
5. **Maintain backward compatibility**: All existing functionality must work
6. **Add tests for new features**: Unit + integration + performance tests required
7. **Update this guide for architecture changes**: Document all modifications
8. **Performance test all changes**: Benchmark against established metrics
9. **Follow established patterns**: Use RetroCardView, avoid external UI libraries
10. **Code review required**: All changes must be peer-reviewed
11. **No Material Design**: Never reintroduce Material Design dependencies
12. **Fragment Safety**: Always check `isAdded` before fragment operations
13. **State Management**: Preserve selection state in submenu navigation

## Development Guidelines
- **Coordinator Pattern**: Fragment coordinates specialized managers
- **Composition over Inheritance**: Use injected managers, not inheritance
- **SOLID Principles**: Single responsibility in each manager
- **Dependency Injection**: Manual dependency injection in `initializeManagers()`
- **Reactive State**: StateFlow for state management
- **RetroCardView Exclusively**: UI components custom-built only
- **Performance Optimization**: Object pools for animations, AdvancedPerformanceProfiler
- **Comprehensive Testing**: Unit + Integration + Performance benchmarks
- **Conditional Logging**: MenuLogger with specific categories
- **Peer Reviews Mandatory**: All changes must be reviewed
- **Zero External UI Dependencies**: Never reintroduce Material Design
- **Incremental Changes**: Test thoroughly before deploy

## Key Techniques
- **Coordinator Pattern**: Fragment delegates to specialized managers
- **Manual DI**: `initializeManagers()` creates and wires dependencies
- **StateFlow**: Reactive state management for UI updates
- **Animations**: ViewPropertyAnimator + object pools for performance
- **Configuration**: Dynamic menus via MenuConfigurationBuilder
- **Logging**: Conditional logging with MenuLogger (debug only in dev)
- **Fragment Protection**: `isAdded` checks before fragment operations
- **State Preservation**: Index saving for submenu navigation

## Best Practices
- **Coordinator Pattern**: Fragment coordinates, managers execute
- **SOLID Principles**: Single responsibility in each class
- **Manual Dependency Injection**: `initializeManagers()` for wiring
- **Reactive State**: StateFlow for unidirectional data flow
- **RetroCardView Only**: No Material Design components ever
- **Performance Monitoring**: AdvancedPerformanceProfiler active
- **Comprehensive Testing**: Unit + Integration + Performance
- **Conditional Logging**: MenuLogger with categories (lifecycle, action, state)
- **Fragment Safety**: `isAdded` checks before operations
- **State Preservation**: Save/restore selection indices
- **Incremental Development**: Test thoroughly, peer review mandatory
- **Zero External UI Dependencies**: Custom components only