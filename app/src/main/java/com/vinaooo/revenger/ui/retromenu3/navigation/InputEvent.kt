package com.vinaooo.revenger.ui.retromenu3.navigation

/**
 * Evento unificado de navegação que representa todas as ações possíveis nos menus, independente da
 * fonte de entrada (gamepad, touch, teclado).
 *
 * Todos os adaptadores de entrada traduzem suas entradas específicas para estes eventos
 * NavigationEvent, permitindo que o NavigationController processe todas as entradas de forma
 * uniforme.
 */
sealed class NavigationEvent {
        /** Timestamp do evento em milissegundos (System.currentTimeMillis()) */
        abstract val timestamp: Long

        /** Fonte de entrada que gerou este evento */
        abstract val inputSource: InputSource

        /**
         * Evento de navegação direcional (DPAD, setas do teclado, gestos).
         *
         * Exemplos:
         * - Usuário pressiona DPAD_DOWN → Navigate(direction = DOWN)
         * - Usuário pressiona seta para cima → Navigate(direction = UP)
         */
        data class Navigate(
                val direction: Direction,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Evento de seleção direta de um item específico (normalmente touch).
         *
         * Exemplo:
         * - Usuário toca no item 3 → SelectItem(index = 3)
         */
        data class SelectItem(
                val index: Int,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Evento de ativação do item atualmente selecionado.
         *
         * Exemplos:
         * - Usuário pressiona botão A → ActivateSelected
         * - Usuário pressiona Enter → ActivateSelected
         * - Touch: após 100ms do SelectItem → ActivateSelected
         *
         * @param keyCode O código da tecla/botão que ativou (para grace period). Null para touch.
         */
        data class ActivateSelected(
                val keyCode: Int? = null,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Evento de navegação para trás (voltar ao menu anterior).
         *
         * Exemplos:
         * - Usuário pressiona botão B → NavigateBack
         * - Usuário pressiona Escape → NavigateBack
         * - Usuário toca no item "Voltar" → NavigateBack
         * - Usuário pressiona botão Back do Android → NavigateBack
         *
         * @param keyCode O código da tecla/botão que voltou (para grace period). Null para touch.
         */
        data class NavigateBack(
                val keyCode: Int? = null,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Evento de abertura do menu principal.
         *
         * Exemplos:
         * - Usuário pressiona SELECT+START → OpenMenu
         * - Usuário pressiona START (quando em jogo) → OpenMenu
         */
        data class OpenMenu(
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Evento de fechamento COMPLETO de todos os menus (direto para o jogo).
         *
         * Exemplos:
         * - Usuário pressiona START (quando menu está aberto) → CloseAllMenus
         * - Usuário pressiona botão Menu/Hamburguer (quando menu está aberto) → CloseAllMenus
         *
         * Este evento difere de NavigateBack, que volta um passo de cada vez (submenu → main →
         * jogo). CloseAllMenus fecha tudo diretamente (qualquer menu → jogo).
         */
        data class CloseAllMenus(
                val keyCode: Int? = null,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()
}

/** Direções de navegação possíveis. */
enum class Direction {
        UP,
        DOWN,
        LEFT, // Reservado para uso futuro
        RIGHT // Reservado para uso futuro
}

/**
 * Fonte de entrada que gerou o evento.
 *
 * Usado para logging, debugging e possíveis ajustes de comportamento específicos por tipo de
 * entrada (ex: delay touch diferente de gamepad).
 */
enum class InputSource {
        /** Gamepad emulado (botões virtuais na tela) */
        EMULATED_GAMEPAD,

        /** Gamepad físico (Bluetooth, USB) */
        PHYSICAL_GAMEPAD,

        /** Toque na tela (touch events) */
        TOUCH,

        /** Teclado físico (USB, Bluetooth) */
        KEYBOARD,

        /** Botão Back do sistema Android */
        SYSTEM_BACK
}
