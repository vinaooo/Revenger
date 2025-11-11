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
     * Quando `false`: Usa o sistema de navegação antigo (ControllerInput direto) Quando `true`: Usa
     * o novo NavigationController com suporte unificado para:
     * - Gamepad físico e emulado
     * - Touch (toque na tela)
     * - Teclado físico
     * - Navegação mista sem conflitos
     *
     * **IMPORTANTE**: Manter como `false` até Phase 4 estar completa e todos os testes passarem. Só
     * alterar para `true` quando o novo sistema estiver 100% validado.
     *
     * **ROLLBACK**: Se algo quebrar após definir como `true`, basta mudar de volta para `false` e o
     * sistema antigo volta a funcionar imediatamente.
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
    const val DEBUG_NAVIGATION = true // TEMP: Enabled for Phase 4 keyboard debugging
}
