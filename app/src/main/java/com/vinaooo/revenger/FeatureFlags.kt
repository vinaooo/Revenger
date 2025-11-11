package com.vinaooo.revenger

/**
 * Feature flags para controlar features em desenvolvimento.
 *
 * Este arquivo permite alternar entre implementações antigas e novas sem quebrar o código
 * existente. Útil para desenvolvimento incremental e testes A/B.
 */
object FeatureFlags {
    /**
     * Flag para controlar o novo sistema de navegação multi-input.
     *
     * **PERMANENTEMENTE HABILITADO APÓS PHASE 4 VALIDAÇÃO**
     *
     * Sistema novo NavigationController com suporte unificado para:
     * - Gamepad físico e emulado
     * - Touch (toque na tela)
     * - Teclado físico
     * - Navegação mista sem conflitos
     *
     * **VALIDADO**: Phase 4 completa - todos os testes passaram. **CLEANUP**: Phase 5 irá remover
     * todas as condicionais desta flag.
     */
    const val USE_NEW_NAVIGATION_SYSTEM = true

    /**
     * Flag para habilitar logs de debug detalhados do sistema de navegação.
     *
     * Quando `true`: Exibe logs detalhados de navegação, eventos, debounce, etc. Quando `false`:
     * Apenas logs críticos (erros e warnings)
     *
     * **IMPORTANTE**: Manter `false` em produção para melhor performance.
     */
    const val DEBUG_NAVIGATION = false
}
