# RetroMenu3 Maintenance and Development Guide

## Overview
Essential principles and practices for RetroMenu3 development. Maintain UI/UX, follow SOLID, ensure performance.

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
│   └── MenuViewManager (UI Updates)
├── AnimationOptimizer (Performance)
└── Test Suite (Validation)
```

## Testing Requirements
- Unit tests for all classes
- Integration tests for workflows
- Performance benchmarks before/after changes
- UI tests for visual states

## Code Standards
- PascalCase classes, camelCase methods/variables
- KDoc for public APIs
- No print statements - use logging
- Immutable state objects

## Maintenance Rules
1. Preserve existing UI/UX
2. Maintain backward compatibility
3. Add tests for new features
4. Update this guide for architecture changes
5. Performance test all changes
6. Follow established patterns
7. Code review required

## Development Guidelines
- Extend via composition, not inheritance
- Use reactive patterns for state
- Optimize animations with pools
- Test thoroughly before deploy
- Document complex logic
- Monitor performance metrics

## Best Practices
- SOLID principles in all code
- Reactive state with StateFlow
- Optimized animations
- Comprehensive testing
- Conditional logging
- Incremental changes
- Peer reviews mandatory