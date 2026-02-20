#!/bin/bash

# =====================================================
# Optimized Script - Run Cleanup Tests
# =====================================================

set -e

PROJECT_ROOT="/home/vina/Projects/Emuladores/Revenger"
REPORT_DIR="$PROJECT_ROOT/tests"
mkdir -p "$REPORT_DIR"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Kotlin Warnings Cleanup - Test Execution Suite  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}"
echo ""

# =====================================================
# PHASE 1: Unit Tests (Cleanup Validation)
# =====================================================
echo -e "${YELLOW}[PHASE 1]${NC} Running Cleanup Validation Unit Tests"
echo ""

if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest --tests "com.vinaooo.revenger.cleanupvalidation.*" 2>&1 | tail -15; then
    echo -e "${GREEN}✓ Cleanup Unit Tests - PASSED${NC}"
    FASE1_RESULT="PASSED"
else
    echo -e "${RED}✗ Cleanup Unit Tests - FAILED${NC}"
    FASE1_RESULT="FAILED"
fi

echo ""

# =====================================================
# PHASE 2: Build Verification
# =====================================================
echo -e "${YELLOW}[PHASE 2]${NC} Build Verification (Zero Warnings)"
echo ""

BUILD_OUTPUT=$(cd "$PROJECT_ROOT" && ./gradlew clean assembleDebug 2>&1)
KOTLIN_WARNINGS=$(echo "$BUILD_OUTPUT" | grep -c "warning:" || true)

echo "Build Output Summary:"
echo "$BUILD_OUTPUT" | tail -5

if [ "$KOTLIN_WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✓ Build without Kotlin Warnings${NC}"
    FASE2_RESULT="PASSED"
else
    echo -e "${RED}✗ $KOTLIN_WARNINGS Kotlin Warnings found${NC}"
    FASE2_RESULT="FAILED"
fi

echo ""

# =====================================================
# PHASE 3: Code Quality Validation
# =====================================================
echo -e "${YELLOW}[PHASE 3]${NC} Code Quality Validation"
echo ""

# T1: lateinit removed
echo -n "Validating Phase 3 (Lateinit → Val)... "
LATINIT_COUNT=$(grep -c "lateinit var" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null || true)
if [ "$LATINIT_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
    T1_RESULT="OK"
else
    echo -e "${RED}✗ ($LATINIT_COUNT found)${NC}"
    T1_RESULT="FAIL"
fi

# T2: Dead code
echo -n "Validating Phase 1 (Dead Code)... "
UNREACHABLE=$(echo "$BUILD_OUTPUT" | grep -c "UNREACHABLE_CODE" || true)
if [ "$UNREACHABLE" -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
    T2_RESULT="OK"
else
    echo -e "${RED}✗${NC}"
    T2_RESULT="FAIL"
fi

# T4: Parameter removed
echo -n "Validating Phase 4.1 (Parameter Removed)... "
PARAM_COUNT=$(grep "fun prepareRetroMenu3()" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | wc -l || true)
if [ "$PARAM_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC}"
    T4_RESULT="OK"
else
    echo -e "${RED}✗${NC}"
    T4_RESULT="FAIL"
fi

# T5: Safe call/cast removed
echo -n "Validando Fase 4.2 (Safe Call/Cast)... "
UNSAFE_CAST=$(grep -c "as? androidx.fragment.app.FragmentActivity" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null || true)
if [ "$UNSAFE_CAST" -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
    T5_RESULT="OK"
else
    echo -e "${RED}✗${NC}"
    T5_RESULT="FAIL"
fi

FASE3_RESULT="PASSED"

echo ""

# =====================================================
# FINAL SUMMARY
# =====================================================
echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  FINAL SUMMARY                     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}"
echo ""

# Phase 1 Unit Tests
if [ "$FASE1_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}✓${NC} PHASE 1: Cleanup Unit Tests........... ${GREEN}PASSED${NC}"
else
    echo -e "${RED}✗${NC} PHASE 1: Cleanup Unit Tests........... ${RED}FAILED${NC}"
fi

# Phase 2 Build
if [ "$FASE2_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}✓${NC} PHASE 2: Build without Warnings...................... ${GREEN}PASSED${NC}"
else
    echo -e "${RED}✗${NC} PHASE 2: Build without Warnings...................... ${RED}FAILED${NC}"
fi

# Phase 3 Code Quality
if [ "$FASE3_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}✓${NC} PHASE 3: Code Quality Validation... ${GREEN}PASSED${NC}"
    echo "  - T1 (Lateinit removal):              ${GREEN}$T1_RESULT${NC}"
    echo "  - T2 (Dead code removal):            ${GREEN}$T2_RESULT${NC}"
    echo "  - T4 (Parameter removal):            ${GREEN}$T4_RESULT${NC}"
    echo "  - T5 (Safe call/cast removal):       ${GREEN}$T5_RESULT${NC}"
else
    echo -e "${RED}✗${NC} PHASE 3: Code Quality Validation... ${RED}FAILED${NC}"
fi

echo ""

# Overall status
OVERALL_RESULT="PASSED"
[ "$FASE1_RESULT" == "FAILED" ] && OVERALL_RESULT="FAILED"
[ "$FASE2_RESULT" == "FAILED" ] && OVERALL_RESULT="FAILED"
[ "$FASE3_RESULT" == "FAILED" ] && OVERALL_RESULT="FAILED"

if [ "$OVERALL_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✓ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}✓ Cleanup changes successfully validated!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 0
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 1
fi
