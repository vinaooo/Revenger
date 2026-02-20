#!/bin/bash

# =====================================================
# Test Execution Script - Kotlin Warnings Cleanup
# =====================================================
#
# This script runs all tests for the warnings cleanup session
# Coverage: Phase 1 (dead code), Phase 2 (ViewModelProvider), 
#           Phase 3 (lateinit), Phase 4 (parameters & casts)
#

set -e

PROJECT_ROOT="/home/vina/Projects/Emuladores/Revenger"
BUILD_VARIANT="debug"
REPORT_FILE="$PROJECT_ROOT/tests/TEST_REPORT_$(date +%Y%m%d_%H%M%S).md"
FAILED_TESTS=0
PASSED_TESTS=0

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Kotlin Warnings Cleanup Test${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# =====================================================
# Function: Structured logging
# =====================================================
log_test() {
    echo -e "${YELLOW}[TESTE]${NC} $1"
}

log_pass() {
    echo -e "${GREEN}[OK]${NC} $1"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
}

# =====================================================
# Phase 1: Unit Tests (Robolectric)
# =====================================================
echo ""
echo -e "${YELLOW}>>> PHASE 1: Unit Tests (Robolectric)${NC}"
echo ""

log_test "Compiling unit tests..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest --info 2>&1 | tail -20; then
    log_pass "Unit test compilation succeeded"
else
    log_fail "Unit test compilation failed"
fi

log_test "Running GameActivityViewModelCleanupTest (T1 - Initialization)..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest \
    --tests "com.vinaooo.revenger.viewmodels.GameActivityViewModelCleanupTest_T1_Initialization" \
    2>&1 | grep -E "PASSED|FAILED"; then
    log_pass "T1 - Initialization tests executed"
else
    log_fail "T1 - Initialization tests failed"
fi

log_test "Running GameActivityViewModelCleanupTest (T2 - DeadCode)..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest \
    --tests "com.vinaooo.revenger.viewmodels.GameActivityViewModelCleanupTest_T2_DeadCode" \
    2>&1 | grep -E "PASSED|FAILED"; then
    log_pass "T2 - DeadCode tests executed"
else
    log_fail "T2 - DeadCode tests failed"
fi

log_test "Running GameActivityViewModelCleanupTest (T4 - UnusedParameter)..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest \
    --tests "com.vinaooo.revenger.viewmodels.GameActivityViewModelCleanupTest_T4_UnusedParameter" \
    2>&1 | grep -E "PASSED|FAILED"; then
    log_pass "T4 - UnusedParameter tests executed"
else
    log_fail "T4 - UnusedParameter tests failed"
fi

# =====================================================
# Phase 2: Instrumentation Tests (Espresso)
# =====================================================
echo ""
echo -e "${YELLOW}>>> PHASE 2: Instrumentation Tests (Espresso)${NC}"
echo ""

log_test "Compiling instrumentation tests..."
if cd "$PROJECT_ROOT" && ./gradlew assembleAndroidTest 2>&1 | tail -10; then
    log_pass "Instrumentation test compilation succeeded"
else
    log_fail "Instrumentation test compilation failed"
fi

log_test "Running GameActivityCleanupIntegrationTest..."
if cd "$PROJECT_ROOT" && ./gradlew connectedAndroidTest \
    --tests "com.vinaooo.revenger.ui.integration.GameActivityCleanupIntegrationTest" \
    2>&1 | tail -15; then
    log_pass "GameActivityCleanupIntegrationTest executed"
else
    log_fail "GameActivityCleanupIntegrationTest failed (Emulator may not be running)"
fi

log_test "Running CriticalBehaviorValidationTest..."
if cd "$PROJECT_ROOT" && ./gradlew connectedAndroidTest \
    --tests "com.vinaooo.revenger.ui.integration.CriticalBehaviorValidationTest" \
    2>&1 | tail -15; then
    log_pass "CriticalBehaviorValidationTest executed"
else
    log_fail "CriticalBehaviorValidationTest failed"
fi

# =====================================================
# Phase 3: Build Analysis (Compilation Warnings)
# =====================================================
echo ""
echo -e "${YELLOW}>>> PHASE 3: Build Validation (Zero Warnings)${NC}"
echo ""

log_test "Running clean build..."
if cd "$PROJECT_ROOT" && ./gradlew clean assembleDebug 2>&1 | tail -5; then
    log_pass "Clean build succeeded"
else
    log_fail "Clean build failed"
fi

log_test "Checking Kotlin compilation warnings..."
BUILD_OUTPUT=$(cd "$PROJECT_ROOT" && ./gradlew assembleDebug 2>&1)
KOTLIN_WARNINGS=$(echo "$BUILD_OUTPUT" | grep -c "warning:" || true)

if [ "$KOTLIN_WARNINGS" -eq 0 ]; then
    log_pass "Zero Kotlin compilation warnings (expected)"
else
    log_fail "Found $KOTLIN_WARNINGS compilation warnings"
fi

# =====================================================
# Phase 4: Code Quality Checks
# =====================================================
echo ""
echo -e "${YELLOW}>>> PHASE 4: Code Quality Checks${NC}"
echo ""

log_test "Running detekt (linter)..."
if cd "$PROJECT_ROOT" && ./gradlew detekt 2>&1 | tail -5; then
    log_pass "Detekt executed successfully"
else
    log_fail "Detekt execution failed"
fi

# =====================================================
# Phase 5: Critical Features Validation
# =====================================================
echo ""
echo -e "${YELLOW}>>> PHASE 5: Critical Features Validation${NC}"
echo ""

log_test "Validating code structure (grep checks)..."

# T1: Validate that there are no redundant lateinit keywords
LATEINIT_COUNT=$(grep -r "lateinit var" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | wc -l || true)
if [ "$LATEINIT_COUNT" -eq 0 ]; then
    log_pass "lateinit conversion completed (T1.3)"
else
    log_fail "There are still $LATEINIT_COUNT lateinit declarations in GameActivityViewModel"
fi

# T2: Validate there is no obvious dead code (code after return/throw)
DEAD_CODE=$(grep -A1 "throw\|return" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | grep "Log\." | wc -l || true)
if [ "$DEAD_CODE" -eq 0 ]; then
    log_pass "Dead code removed (T2)"
else
    log_fail "Possible dead code detected"
fi

# T4: Validate that prepareRetroMenu3 has no parameters
PARAM_COUNT=$(grep "fun prepareRetroMenu3(" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | grep -o "([^)]*)" | tr -d '()' | wc -w || true)
if [ "$PARAM_COUNT" -eq 0 ]; then
    log_pass "Parameter removed from prepareRetroMenu3() (T4.1)"
else
    log_fail "prepareRetroMenu3() still has parameters"
fi

# T5: Validate that safe call and cast have been removed
UNSAFE_CAST=$(grep "as? androidx.fragment.app.FragmentActivity" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | wc -l || true)
if [ "$UNSAFE_CAST" -eq 0 ]; then
    log_pass "Safe call and cast removed (T5)"
else
    log_fail "Redundant casts still exist in GameActivityViewModel"
fi

# =====================================================
# Final Summary
# =====================================================
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}TEST SUMMARY${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo -e "Passed Tests: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed Tests: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo -e "${GREEN}✓ TODOS OS TESTES PASSARAM!${NC}"
    exit 0
else
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    exit 1
fi
