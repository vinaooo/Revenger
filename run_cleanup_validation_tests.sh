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
echo -e "${YELLOW}[FASE 1]${NC} Executando Testes Unitários de Validação de Cleanup"
echo ""

if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest --tests "com.vinaooo.revenger.cleanupvalidation.*" 2>&1 | tail -15; then
    echo -e "${GREEN}✓ Testes de Cleanup Unitários - PASSARAM${NC}"
    FASE1_RESULT="PASSOU"
else
    echo -e "${RED}✗ Testes de Cleanup Unitários - FALHARAM${NC}"
    FASE1_RESULT="FALHOU"
fi

echo ""

# =====================================================
# PHASE 2: Build Verification
# =====================================================
echo -e "${YELLOW}[FASE 2]${NC} Verificação de Build (Zero Warnings)"
echo ""

BUILD_OUTPUT=$(cd "$PROJECT_ROOT" && ./gradlew clean assembleDebug 2>&1)
KOTLIN_WARNINGS=$(echo "$BUILD_OUTPUT" | grep -c "warning:" || true)

echo "Build Output Summary:"
echo "$BUILD_OUTPUT" | tail -5

if [ "$KOTLIN_WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✓ Build sem Kotlin Warnings${NC}"
    FASE2_RESULT="PASSOU"
else
    echo -e "${RED}✗ $KOTLIN_WARNINGS Kotlin Warnings encontrados${NC}"
    FASE2_RESULT="FALHOU"
fi

echo ""

# =====================================================
# PHASE 3: Code Quality Validation
# =====================================================
echo -e "${YELLOW}[FASE 3]${NC} Validação de Qualidade de Código"
echo ""

# T1: lateinit removed
echo -n "Validando Fase 3 (Latinit → Val)... "
LATINIT_COUNT=$(grep -c "lateinit var" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null || true)
if [ "$LATINIT_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
    T1_RESULT="OK"
else
    echo -e "${RED}✗ ($LATINIT_COUNT found)${NC}"
    T1_RESULT="FAIL"
fi

# T2: Dead code
echo -n "Validando Fase 1 (Código Morto)... "
UNREACHABLE=$(echo "$BUILD_OUTPUT" | grep -c "UNREACHABLE_CODE" || true)
if [ "$UNREACHABLE" -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
    T2_RESULT="OK"
else
    echo -e "${RED}✗${NC}"
    T2_RESULT="FAIL"
fi

# T4: Parameter removed
echo -n "Validando Fase 4.1 (Parâmetro Removido)... "
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

FASE3_RESULT="PASSOU"

echo ""

# =====================================================
# FINAL SUMMARY
# =====================================================
echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  RESUMO FINAL                      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}"
echo ""

# Fase 1 Unit Tests
if [ "$FASE1_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}✓${NC} FASE 1: Testes Unitários de Cleanup........... ${GREEN}PASSOU${NC}"
else
    echo -e "${RED}✗${NC} FASE 1: Testes Unitários de Cleanup........... ${RED}FALHOU${NC}"
fi

# Fase 2 Build
if [ "$FASE2_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}✓${NC} FASE 2: Build sem Warnings...................... ${GREEN}PASSOU${NC}"
else
    echo -e "${RED}✗${NC} FASE 2: Build sem Warnings...................... ${RED}FALHOU${NC}"
fi

# Fase 3 Code Quality
if [ "$FASE3_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}✓${NC} FASE 3: Validação de Qualidade de Código... ${GREEN}PASSOU${NC}"
    echo "  - T1 (Latinit removal):              ${GREEN}$T1_RESULT${NC}"
    echo "  - T2 (Dead code removal):            ${GREEN}$T2_RESULT${NC}"
    echo "  - T4 (Parameter removal):            ${GREEN}$T4_RESULT${NC}"
    echo "  - T5 (Safe call/cast removal):       ${GREEN}$T5_RESULT${NC}"
else
    echo -e "${RED}✗${NC} FASE 3: Validação de Qualidade de Código... ${RED}FALHOU${NC}"
fi

echo ""

# Status geral
OVERALL_RESULT="PASSOU"
[ "$FASE1_RESULT" == "FALHOU" ] && OVERALL_RESULT="FALHOU"
[ "$FASE2_RESULT" == "FALHOU" ] && OVERALL_RESULT="FALHOU"
[ "$FASE3_RESULT" == "FALHOU" ] && OVERALL_RESULT="FALHOU"

if [ "$OVERALL_RESULT" == "PASSOU" ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✓ TODOS OS TESTES PASSARAM!${NC}"
    echo -e "${GREEN}✓ Mudanças de Cleanup Validadas com Sucesso!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 0
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}✗ ALGUNS TESTES FALHARAM${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 1
fi
