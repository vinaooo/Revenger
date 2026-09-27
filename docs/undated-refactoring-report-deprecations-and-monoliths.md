# Comprehensive Refactoring and Improvement Report

This report defines the structural and architectural fixes required for the Revenger codebase. Guided by static analysis (`detekt`), `definitions/Code.md` principles, and batch-packaging requirements, this report outlines the explicit action plan to stabilize the codebase.

## 1. Executive Summary & Core Directives

Based on systematic analysis of the codebase, the following directives govern this refactoring roadmap:
**A. Testing Prioritization**: Existing broken unit tests MUST be fixed **last**. Priority is strictly on stabilizing the app architecture and fixing deprecated structures first.
**B. Credential Delivery (Intended Exposure)**: Hardcoded credentials and `.jks` file exposure within `build.gradle` are **by design** due to the batch-generation nature of the project. These keys must be delivered to users to allow local APK generation. They will remain, but must be explicitly documented and exempted from security linter checks.
**C. Deprecation Remediation**: Deprecated build tools, outdated lint boundaries, and legacy SDK parameters **MUST BE FIXED FIRST**. They actively hinder modern SDK integration.

---

## 2. Deprecated Build Tools & Code (CRITICAL - FIX IMMEDIATELY)

These items are blocking modern compilation pipelines and must be modernized immediately.

**Identified Issues:**
1. **Deprecated `lintOptions` Block**: In `app/build.gradle` (approx. line 140), the legacy `lintOptions {}` block is used. 
2. **Fragmented Dependencies**: Multiple disparate `dependencies { ... }` blocks exist in the `build.gradle` script, reducing readability and causing potential resolution errors.
3. **Suppressed SDK Warnings**: Artificial masking of SDK incompatibilities in `gradle.properties`.

**Proposed Amendments:**
*   **Fix 2.1**: Rewrite the `lintOptions` in `app/build.gradle` to the modern `lint {}` block:
    ```gradle
    lint {
        abortOnError false
        // other rules migrated
    }
    ```
*   **Fix 2.2**: Consolidate all dependency blocks into a single cohesive `dependencies {}` block sorted by type (Core, UI, LibRetro, Test).
*   **Fix 2.3**: Update Target SDK and Compile SDK versions natively without relying on suppression tags.

---

## 3. Structural/Architectural Monoliths (HIGH PRIORITY)

The core emulator components violate the Single Responsibility Principle (SRP) and hold excessive monolithic logic. 

**Identified Issues:**
1. **`GameActivity.kt` & `GameActivityViewModel.kt` Monoliths**: Spanning over ~1,600 and ~1,900 lines respectively, these files handle UI routing, Audio setup, lifecycle, and initialization natively.
2. **Dead Entrypoints**: Files like `ControllerInput.kt.broken` lingering in the source tree.
3. **Incomplete State Behaviors**: Missing `StateFlow` implementations and leftover `TODOs` in `GameStateViewModel`, `AudioViewModel`, `MenuViewModel`, `InputViewModel`, and `SpeedViewModel`.

**Proposed Amendments:**
*   **Fix 3.1 (Monolith Breakdown)**: Extract sub-components from `GameActivity` into focused modular delegates:
    *   Create `AudioRoutingManager` for audio setup.
    *   Create `GameLifecycleObserver` for lifecycle bindings.
*   **Fix 3.2 (Clean Worktree)**: Purge `.broken` files entirely.
*   **Fix 3.3 (State Completeness)**: Replace `TODOs` in the ViewModel cluster with sealed `StateFlow` abstractions matching the `RetroMenu3Fragment` standard.

---

## 4. Operational Assets & Code Quality (MEDIUM PRIORITY)

Script automation and code hygiene elements that must be standardized.

**Identified Issues:**
1. **Public Credentials (JKS)**: Revenger requires users to dynamically build their APK, meaning signature keys must be accessible to them. This throws false-positive CI/CD security errors.
2. **CLI Output vs Logging**: `icons/scripts/master_icon.py` heavily uses raw `print()` statements (e.g., line 75, 187, 219). While this technically violates the debug code standard, it is currently necessary to show users what is happening during script execution. Conversely, `GameActivity` misuses generic `println` which is purely for internal debug and must be fixed.
3. **Detekt Violations**: High volume of `MaxLineLength` and `MagicNumber` violations.

**Proposed Amendments:**
*   **Fix 4.1 (Credential Documentation)**: Create a `SECURITY.md` in the root explicitly defining the `.jks` and `build.gradle` keystore passwords ("ludere") as public-domain deployment assets for end-user builds.
*   **Fix 4.2 (Logging System Updates)**: 
    *   In Python scripts: Maintain standard output for UX, but upgrade from raw `print()` to Python's `logging` module configured for formatted console output (e.g., `logging.getLogger().setLevel(logging.INFO)`), or integrate a better CLI library like `Rich` to provide clean user feedback.
    *   In Kotlin code: Replace `println`/`print` with Android's `Log.d(TAG, ...)` or a custom `AppLogger`.

---

## 5. Testing Validation (LOW PRIORITY - DEFERRED TO END)

As per the core directives, tests are fixed ONLY after Architectural and Deprecation tasks are completed.

**Identified Issues:**
1. **`SaveStateManagerTest.kt` failures**: Logic mismatch where `Bitmap?` parsing fails and returns a `String`.
2. **Missing Coverage**: `NavigationEventProcessor` states remain uncovered.

**Proposed Amendments:**
*   **Fix 5.1**: Resolve the `SaveStateManagerTest.kt` string-cast anomaly.
*   **Fix 5.2**: Write isolated ViewModel state tests for the newly extracted component managers once Phase 2 is complete.
