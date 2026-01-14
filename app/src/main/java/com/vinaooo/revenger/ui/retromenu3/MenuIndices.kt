package com.vinaooo.revenger.ui.retromenu3

/**
 * Constantes para índices de itens do menu principal.
 *
 * Centraliza os valores mágicos (0-5) para facilitar manutenção. Se a ordem dos menus mudar,
 * alterar apenas aqui.
 *
 * Padrão SOLID: Evita duplicação de valores magic numbers Manutenibilidade: Mudança centralizada =
 * uma fonte da verdade
 */
object MenuIndices {
    /** Item 0: Continue (retomar jogo) */
    const val CONTINUE = 0

    /** Item 1: Reset (resetar jogo) */
    const val RESET = 1

    /** Item 2: Progress (save/load states) */
    const val PROGRESS = 2

    /** Item 3: Settings (configurações) */
    const val SETTINGS = 3

    /** Item 4: About (sobre o jogo/emulador) */
    const val ABOUT = 4

    /** Item 5: Exit (sair do emulador) */
    const val EXIT = 5

    /** Total de itens no menu principal */
    const val TOTAL_ITEMS = 6
}
