package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import android.os.Bundle

/**
 * Menu type in the RetroMenu3 system.
 *
 * Represents all menus available in the application. Each menu has a unique type that identifies
 * which fragment to display.
 */
enum class MenuType {
    /** Main menu (Continue, Reset, Progress, Settings, Exit, Save Log) */
    MAIN,

    /** Settings submenu (Audio, Shader, Game Speed, Back) */
    SETTINGS,

    /** Progress submenu (Load State, Save State, Manage Saves, Back) */
    PROGRESS,

    /** Exit submenu (Save & Exit, Exit without Save, Back) */
    EXIT,

    /** About/information submenu */
    ABOUT,

    /** LibRetro core variables submenu */
    CORE_VARIABLES,

    /** Save slots grid (3x3 grid with 9 slots) */
    SAVE_SLOTS,

    /** Load slots grid (3x3 grid with 9 slots) */
    LOAD_SLOTS,

    /** Manage slots grid (rename, copy, move, delete) */
    MANAGE_SAVES,

    /** Save slots grid used during Save and Exit flow */
    EXIT_SAVE_SLOTS
}

/**
 * Complete state of a menu at a specific moment.
 *
 * Contains all information needed to restore a menu exactly as it was, including after
 * screen rotation.
 *
 * @property menuType Type of the active menu
 * @property selectedIndex Index of the selected item (0-based)
 */
data class MenuState(val menuType: MenuType, val selectedIndex: Int = 0) {
    /**
     * Serializa o estado para um Bundle (para onSaveInstanceState).
     *
     * @param prefix Prefixo para as keys no Bundle (evita colisões)
     * @return Bundle contendo o estado serializado
     */
    fun toBundle(prefix: String = "menu_"): Bundle {
        return Bundle().apply {
            putString("${prefix}type", menuType.name)
            putInt("${prefix}index", selectedIndex)
        }
    }

    companion object {
        /**
         * Deserializes the state from a Bundle (from onRestoreInstanceState).
         *
         * @param bundle Bundle containing the saved state
         * @param prefix Prefix used during serialization
         * @return Restored MenuState, or null if the Bundle doesn't contain valid data
         */
        fun fromBundle(bundle: Bundle?, prefix: String = "menu_"): MenuState? {
            if (bundle == null) return null

            val typeString = bundle.getString("${prefix}type") ?: return null
            val index = bundle.getInt("${prefix}index", 0)

            return try {
                val menuType = MenuType.valueOf(typeString)
                MenuState(menuType, index)
            } catch (e: IllegalArgumentException) {
                // Invalid MenuType in Bundle
                Log.w("NavigationState", "Invalid MenuType in bundle: $typeString")
                null
            }
        }
    }
}

/**
 * Navigation stack that keeps a history of visited menus.
 *
 * Used to implement back navigation. When the user navigates to a submenu,
 * the current state is pushed. When the user goes back, the state is popped
 * and restored.
 *
 * Example:
 * ```
 * // User is on MAIN menu
 * stack.push(MenuState(MAIN, selectedIndex=0))
 * // Navigate to SETTINGS
 * currentState = MenuState(SETTINGS, 0)
 *
 * // User presses Back
 * currentState = stack.pop()  // Return to MAIN with selectedIndex=0
 * ```
 */
class NavigationStack {
    private val stack = mutableListOf<MenuState>()

    /** Push a state (when navigating forward). */
    fun push(state: MenuState) {
        stack.add(state)
    }

    /**
     * Pop a state (when navigating backward).
     *
     * @return Previous MenuState, or null if the stack is empty
     */
    fun pop(): MenuState? {
        return if (stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
        } else {
            null
        }
    }

    /** Checks if the stack is empty. */
    fun isEmpty(): Boolean = stack.isEmpty()

    /** Returns the current size of the stack. */
    fun size(): Int = stack.size

    /** Clears the entire stack. */
    fun clear() {
        stack.clear()
    }

    /**
     * Peek at the top of the stack without removing it.
     *
     * @return MenuState at the top, or null if the stack is empty
     */
    fun peek(): MenuState? = stack.lastOrNull()

    /** Serializa a pilha para um Bundle. */
    fun toBundle(prefix: String = "stack_"): Bundle {
        return Bundle().apply {
            putInt("${prefix}size", stack.size)
            stack.forEachIndexed { index, state ->
                val stateBundle = state.toBundle("${prefix}${index}_")
                putString("${prefix}${index}_type", stateBundle.getString("${prefix}${index}_type"))
                putInt("${prefix}${index}_index", stateBundle.getInt("${prefix}${index}_index"))
            }
        }
    }

    /** Deserializa a pilha de um Bundle. */
    fun fromBundle(bundle: Bundle?, prefix: String = "stack_") {
        clear()
        if (bundle == null) return

        val size = bundle.getInt("${prefix}size", 0)
        for (i in 0 until size) {
            val typeString = bundle.getString("${prefix}${i}_type") ?: continue
            val index = bundle.getInt("${prefix}${i}_index", 0)

            try {
                val menuType = MenuType.valueOf(typeString)
                stack.add(MenuState(menuType, index))
            } catch (e: IllegalArgumentException) {
                Log.w("NavigationStack", "Invalid MenuType in stack: $typeString")
            }
        }
    }
}
