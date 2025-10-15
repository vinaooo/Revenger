# RetroMenu3 Maintenance and Development Guide

## Overview
Essential principles and practices for RetroMenu3 development. Maintain UI/UX, follow SOLID, ensure performance.

**Last Updated**: October 2025
**Version**: 2.0 - Post Material Design Migration

## UI Design Principles
The RetroMenu3 system does **NOT** use and should **NOT** use Material Design, Material You, or Material You Expressive. Following the successful migration completed in October 2025, the UI exclusively uses custom **RetroCardView** components with retro styling that resembles old video games.

### UI Characteristics
- Retro fonts and pixel-perfect styling
- Navigation via touch DPAD or USB gamepad
- Confirmation buttons on touch or USB gamepads
- Direct touch control (finger touch on options)
- Custom RetroCardView with transparent backgrounds
- No Material Design shadows, corners, or theming

## Core Principles
- **SOLID**: Single responsibility, open/closed, Liskov, interface segregation, dependency inversion
- **Reactive**: StateFlow for state management, unidirectional data flow
- **Composition**: Use specialized classes (MenuActionHandler, SubmenuCoordinator, MenuViewManager)

## Key Techniques
- **Animations**: ViewPropertyAnimator + object pools for performance
- **Configuration**: Dynamic menus via MenuConfigurationBuilder
- **Logging**: Conditional logging with MenuLogger (debug only in dev)

## Architecture
```
RetroMenu3Fragment (UI)
├── MenuViewModel (StateFlow)
├── MenuSystem (Logic)
│   ├── MenuActionHandler (Actions)
│   ├── SubmenuCoordinator (Navigation)
│   └── MenuViewManager (UI Updates with RetroCardView)
├── RetroCardView (Custom UI Component)
├── AnimationOptimizer (Performance)
└── Test Suite (Unit + Integration + Robolectric)
```

### Component Standards
- **RetroCardView**: Custom card component replacing MaterialCardView
- **No External UI Dependencies**: All components are custom-built
- **Retro Styling**: Pixel-perfect, arcade-style appearance
- **Performance Optimized**: Object pooling for animations

## Testing Requirements
- **Unit tests** for all classes (RetroMenu3FragmentTest)
- **Integration tests** for workflows (RetroMenu3IntegrationTest with Robolectric)
<!-- - **Robolectric tests** for Android framework interactions -->
- **Performance benchmarks** before/after changes
- **UI validation** for visual states and RetroCardView rendering

### Testing Implementation
- **JUnit 4/5** for unit and integration tests
- **Robolectric 4.11.1** for Android framework simulation
- **MockK** for dependency mocking
- **GitHub Actions** for CI/CD validation

## Code Standards
- PascalCase classes, camelCase methods/variables
- KDoc for public APIs
- No print statements - use logging
- Immutable state objects

## Maintenance Rules
1. **Preserve existing UI/UX**: Maintain retro styling and RetroCardView appearance
2. **Maintain backward compatibility**: All existing functionality must work
3. **Add tests for new features**: Unit + integration tests required
4. **Update this guide for architecture changes**: Document all modifications
5. **Performance test all changes**: Benchmark against established metrics
6. **Follow established patterns**: Use RetroCardView, avoid external UI libraries
7. **Code review required**: All changes must be peer-reviewed
8. **No Material Design**: Never reintroduce Material Design dependencies

## Migration History

### Material Design Migration (October 2025) ✅ COMPLETED
**Objective**: Complete removal of Material Design dependencies and implementation of custom RetroCardView

**Technical Achievements**:
- Custom UI component architecture
- Robolectric integration testing
- Zero breaking changes
- Performance optimization maintained
- Complete test suite validation

## Development Guidelines
- **Extend via composition, not inheritance**
- **Use reactive patterns** for state management
- **Optimize animations** with object pools
- **Use RetroCardView exclusively** for UI components
- **Test thoroughly** before deploy (Unit + Integration + Robolectric)
- **Document complex logic** and architecture changes
- **Monitor performance metrics** against established benchmarks
- **Never reintroduce Material Design** dependencies
- **Maintain retro styling** in all UI components
- **Follow migration patterns** for future component updates

## Best Practices
- **SOLID principles** in all code
- **Reactive state** with StateFlow
- **RetroCardView only** - no Material Design components
- **Optimized animations** with object pools
- **Comprehensive testing**: Unit + Integration + Robolectric
- **Conditional logging** (debug only in development)
- **Incremental changes** with thorough testing
- **Peer reviews mandatory** for all changes
- **Performance monitoring** with benchmarks
- **Zero external UI dependencies**