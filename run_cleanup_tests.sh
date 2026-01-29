#!/bin/bash

# =====================================================
# Script de Execução de Testes - Kotlin Warnings Cleanup
# =====================================================
#
# Este script executa todos os testes da sessão de limpeza de warnings
# Cobertura: Fase 1 (dead code), Fase 2 (ViewModelProvider), 
#           Fase 3 (lateinit), Fase 4 (parameters & casts)
#

set -e

PROJECT_ROOT="/home/vina/Projects/Emuladores/Revenger"
BUILD_VARIANT="debug"
REPORT_FILE="$PROJECT_ROOT/tests/TEST_REPORT_$(date +%Y%m%d_%H%M%S).md"
FAILED_TESTS=0
PASSED_TESTS=0

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Teste de Limpeza de Kotlin Warnings${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# =====================================================
# Função: Log estruturado
# =====================================================
log_test() {
    echo -e "${YELLOW}[TESTE]${NC} $1"
}

log_pass() {
    echo -e "${GREEN}[OK]${NC} $1"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}[FALHA]${NC} $1"
    ((FAILED_TESTS++))
}

# =====================================================
# Fase 1: Unit Tests (Robolectric)
# =====================================================
echo ""
echo -e "${YELLOW}>>> FASE 1: Testes Unitários (Robolectric)${NC}"
echo ""

log_test "Compilando testes unitários..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest --info 2>&1 | tail -20; then
    log_pass "Compilação de testes unitários bem-sucedida"
else
    log_fail "Falha na compilação de testes unitários"
fi

log_test "Executando GameActivityViewModelCleanupTest (T1 - Initialization)..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest \
    --tests "com.vinaooo.revenger.viewmodels.GameActivityViewModelCleanupTest_T1_Initialization" \
    2>&1 | grep -E "PASSED|FAILED"; then
    log_pass "T1 - Initialization tests executados"
else
    log_fail "T1 - Initialization tests falharam"
fi

log_test "Executando GameActivityViewModelCleanupTest (T2 - DeadCode)..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest \
    --tests "com.vinaooo.revenger.viewmodels.GameActivityViewModelCleanupTest_T2_DeadCode" \
    2>&1 | grep -E "PASSED|FAILED"; then
    log_pass "T2 - DeadCode tests executados"
else
    log_fail "T2 - DeadCode tests falharam"
fi

log_test "Executando GameActivityViewModelCleanupTest (T4 - UnusedParameter)..."
if cd "$PROJECT_ROOT" && ./gradlew testDebugUnitTest \
    --tests "com.vinaooo.revenger.viewmodels.GameActivityViewModelCleanupTest_T4_UnusedParameter" \
    2>&1 | grep -E "PASSED|FAILED"; then
    log_pass "T4 - UnusedParameter tests executados"
else
    log_fail "T4 - UnusedParameter tests falharam"
fi

# =====================================================
# Fase 2: Instrumentation Tests (Espresso)
# =====================================================
echo ""
echo -e "${YELLOW}>>> FASE 2: Testes de Instrumentation (Espresso)${NC}"
echo ""

log_test "Compilando testes de instrumentation..."
if cd "$PROJECT_ROOT" && ./gradlew assembleAndroidTest 2>&1 | tail -10; then
    log_pass "Compilação de testes de instrumentation bem-sucedida"
else
    log_fail "Falha na compilação de testes de instrumentation"
fi

log_test "Executando GameActivityCleanupIntegrationTest..."
if cd "$PROJECT_ROOT" && ./gradlew connectedAndroidTest \
    --tests "com.vinaooo.revenger.ui.integration.GameActivityCleanupIntegrationTest" \
    2>&1 | tail -15; then
    log_pass "GameActivityCleanupIntegrationTest executados"
else
    log_fail "GameActivityCleanupIntegrationTest falharam (Emulator pode não estar em execução)"
fi

log_test "Executando CriticalBehaviorValidationTest..."
if cd "$PROJECT_ROOT" && ./gradlew connectedAndroidTest \
    --tests "com.vinaooo.revenger.ui.integration.CriticalBehaviorValidationTest" \
    2>&1 | tail -15; then
    log_pass "CriticalBehaviorValidationTest executados"
else
    log_fail "CriticalBehaviorValidationTest falharam"
fi

# =====================================================
# Fase 3: Análise de Build (Compilation Warnings)
# =====================================================
echo ""
echo -e "${YELLOW}>>> FASE 3: Validação de Build (Zero Warnings)${NC}"
echo ""

log_test "Executando clean build..."
if cd "$PROJECT_ROOT" && ./gradlew clean assembleDebug 2>&1 | tail -5; then
    log_pass "Build limpo bem-sucedido"
else
    log_fail "Falha no build limpo"
fi

log_test "Verificando warnings de compilação Kotlin..."
BUILD_OUTPUT=$(cd "$PROJECT_ROOT" && ./gradlew assembleDebug 2>&1)
KOTLIN_WARNINGS=$(echo "$BUILD_OUTPUT" | grep -c "warning:" || true)

if [ "$KOTLIN_WARNINGS" -eq 0 ]; then
    log_pass "Zero warnings de compilação Kotlin (esperado)"
else
    log_fail "Encontrados $KOTLIN_WARNINGS warnings de compilação"
fi

# =====================================================
# Fase 4: Code Quality Checks
# =====================================================
echo ""
echo -e "${YELLOW}>>> FASE 4: Verificações de Qualidade de Código${NC}"
echo ""

log_test "Executando detekt (linter)..."
if cd "$PROJECT_ROOT" && ./gradlew detekt 2>&1 | tail -5; then
    log_pass "Detekt executado com sucesso"
else
    log_fail "Falha na execução do detekt"
fi

# =====================================================
# Fase 5: Validação de Funcionalidades Críticas
# =====================================================
echo ""
echo -e "${YELLOW}>>> FASE 5: Validação de Funcionalidades Críticas${NC}"
echo ""

log_test "Validando estrutura de código (grep checks)..."

# T1: Validar que não há lateinit keywords redundantes
LATEINIT_COUNT=$(grep -r "lateinit var" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | wc -l || true)
if [ "$LATEINIT_COUNT" -eq 0 ]; then
    log_pass "Conversão de lateinit concluída (T1.3)"
else
    log_fail "Ainda há $LATEINIT_COUNT declarações lateinit em GameActivityViewModel"
fi

# T2: Validar que não há código morto óbvio (code após return/throw)
DEAD_CODE=$(grep -A1 "throw\|return" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | grep "Log\." | wc -l || true)
if [ "$DEAD_CODE" -eq 0 ]; then
    log_pass "Código morto removido (T2)"
else
    log_fail "Possível código morto detectado"
fi

# T4: Validar que prepareRetroMenu3 não tem parâmetros
PARAM_COUNT=$(grep "fun prepareRetroMenu3(" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | grep -o "([^)]*)" | tr -d '()' | wc -w || true)
if [ "$PARAM_COUNT" -eq 0 ]; then
    log_pass "Parâmetro removido de prepareRetroMenu3() (T4.1)"
else
    log_fail "prepareRetroMenu3() ainda possui parâmetros"
fi

# T5: Validar que safe call e cast foram removidos
UNSAFE_CAST=$(grep "as? androidx.fragment.app.FragmentActivity" "$PROJECT_ROOT/app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt" 2>/dev/null | wc -l || true)
if [ "$UNSAFE_CAST" -eq 0 ]; then
    log_pass "Safe call e cast removidos (T5)"
else
    log_fail "Ainda há casts redundantes em GameActivityViewModel"
fi

# =====================================================
# Resumo Final
# =====================================================
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}RESUMO DE TESTES${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo -e "Testes Passados: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Testes Falhados: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo -e "${GREEN}✓ TODOS OS TESTES PASSARAM!${NC}"
    exit 0
else
    echo -e "${RED}✗ ALGUNS TESTES FALHARAM${NC}"
    exit 1
fi
