package com.vinaooo.revenger.ui.retromenu3.navigation


/**
 * Unified navigation event representing all possible menu actions, regardless of
 * input source (gamepad, touch, keyboard).
 *
 * All input adapters translate their specific inputs into these NavigationEvent types,
 * allowing the NavigationController to process all inputs uniformly.
 */
sealed class NavigationEvent {
        /** Event timestamp in milliseconds (System.currentTimeMillis()) */
        abstract val timestamp: Long

        /** Input source that generated this event */
        abstract val inputSource: InputSource

        /**
         * Directional navigation event (DPAD, keyboard arrows, gestures).
         *
         * Examples:
         * - User presses DPAD_DOWN → Navigate(direction = DOWN)
         * - User presses up arrow → Navigate(direction = UP)
         */
        data class Navigate(
                val direction: Direction,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Direct selection of a specific item (usually touch).
         *
         * Example:
         * - User taps item 3 → SelectItem(index = 3)
         */
        data class SelectItem(
                val index: Int,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Activation event for the currently selected item.
         *
         * Examples:
         * - User presses A button → ActivateSelected
         * - User presses Enter → ActivateSelected
         * - Touch: after 100ms from SelectItem → ActivateSelected
         *
         * @param keyCode The key/button code that activated it (for grace period). Null for touch.
         */
        data class ActivateSelected(
                val keyCode: Int? = null,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Back navigation event (return to the previous menu).
         *
         * Examples:
         * - User presses B button → NavigateBack
         * - User presses Escape → NavigateBack
         * - User taps the "Back" item → NavigateBack
         * - User presses Android Back button → NavigateBack
         *
         * @param keyCode The key/button code that triggered back (for grace period). Null for touch.
         */
        data class NavigateBack(
                val keyCode: Int? = null,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Event to open the main menu.
         *
         * Examples:
         * - User presses SELECT+START → OpenMenu
         * - User presses START (while in game) → OpenMenu
         */
        data class OpenMenu(
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()

        /**
         * Event to completely close all menus (directly to the game).
         *
         * Examples:
         * - User presses START (when menu is open) → CloseAllMenus
         * - User presses the Menu/Hamburger button (when menu is open) → CloseAllMenus
         *
         * This event differs from NavigateBack, which goes back one step at a time
         * (submenu → main → game). CloseAllMenus closes everything directly (any menu → game).
         */
        data class CloseAllMenus(
                val keyCode: Int? = null,
                override val timestamp: Long = System.currentTimeMillis(),
                override val inputSource: InputSource
        ) : NavigationEvent()
}

/** Possible navigation directions. */
enum class Direction {
        UP,
        DOWN,
        LEFT, // Reservado para uso futuro
        RIGHT // Reservado para uso futuro
}

/**
 * Input source that generated the event.
 *
 * Used for logging, debugging and potential behavior tweaks specific to input type
 * (e.g., different touch delay than gamepad).
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
