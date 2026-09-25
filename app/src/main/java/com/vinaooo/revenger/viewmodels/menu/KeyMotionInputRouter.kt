package com.vinaooo.revenger.viewmodels.menu

import android.view.KeyEvent
import android.view.MotionEvent
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter

/** [GameActivityViewModel]'s physical keyboard/gamepad key and motion event routing surface. */
interface KeyMotionInputFacade {
    fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean?
    fun processMotionEvent(event: MotionEvent): Boolean?
    fun shouldHandleBackButton(): Boolean
}

/**
 * Implementation of [KeyMotionInputFacade], plus the SELECT+START/gamepad-menu-button gates that
 * [ControllerInput] queries through its own callbacks. [controllerInput], [retroView] and
 * [keyboardInputAdapter] are read via provider lambdas rather than captured, since all three are
 * mutated on the owning ViewModel after construction.
 */
class KeyMotionInputRouter(
        private val controllerInput: () -> ControllerInput,
        private val retroView: () -> RetroView?,
        private val keyboardInputAdapter: () -> KeyboardInputAdapter?,
        private val isAnyMenuActive: () -> Boolean,
        private val appConfig: AppConfig
) : KeyMotionInputFacade {

    /** Process a key event and return the result */
    override fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean? {
        // DEBUG: Log ALL key events to diagnose Backspace issue
        android.util.Log.d(
                "GameActivityViewModel",
                "[KEY-EVENT] keyCode=$keyCode, action=${event.action}, navigationSystemActive=true"
        )

        if (tryConsumeKeyboardNavigation(keyCode, event) == true) {
            return true // Event was consumed by menu navigation
        }

        // Process normally via ControllerInput (for game inputs)
        val retroView = retroView()
        return if (retroView != null) {
            controllerInput().processKeyEvent(keyCode, event, retroView)
        } else {
            false
        }
    }

    /**
     * PHASE 4.1c: routes [keyCode]/[event] to [keyboardInputAdapter] when it's a navigation key
     * the keyboard path should currently handle, returning whether it consumed the event.
     * Returns `null` when the keyboard path doesn't apply at all (no adapter, not a navigation
     * key, or the menu-active/F12 gate says not to route it there) and `false` when it applied
     * but the adapter didn't consume the event; [processKeyEvent] treats both the same way
     * (falls through to `ControllerInput`), so the distinction only matters to callers that care
     * why.
     */
    private fun tryConsumeKeyboardNavigation(keyCode: Int, event: KeyEvent): Boolean? {
        val adapter = keyboardInputAdapter() ?: return null
        if (!adapter.isNavigationKey(keyCode)) return null

        // PHASE 4.2c: Allow F12 even when menu is closed (to open menu)
        // But Backspace (DEL) only works when menu is OPEN (to navigate back)
        val isMenuActive = isAnyMenuActive()
        val shouldProcessKeyboard = isMenuActive || keyCode == KeyEvent.KEYCODE_F12

        android.util.Log.d(
                "GameActivityViewModel",
                "[PHASE4] Navigation key check: keyCode=$keyCode, " +
                        "action=${event.action}, isMenuActive=$isMenuActive, " +
                        "shouldProcess=$shouldProcessKeyboard"
        )

        if (!shouldProcessKeyboard) return null

        android.util.Log.d(
                "GameActivityViewModel",
                "[PHASE4] Routing key event to KeyboardInputAdapter: " +
                        "keyCode=$keyCode, action=${event.action}"
        )
        // Route to keyboard adapter based on action type
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> adapter.onKeyDown(keyCode, event)
            KeyEvent.ACTION_UP -> adapter.onKeyUp(keyCode, event)
            else -> false
        }
    }

    /** Process a motion event and return the result */
    override fun processMotionEvent(event: MotionEvent): Boolean? {
        // Process normally via ControllerInput
        retroView()?.let {
            return controllerInput().processMotionEvent(event, it)
        }

        return false
    }

    /** Check if menu should respond to back button based on menu_mode_back */
    override fun shouldHandleBackButton(): Boolean {
        return appConfig.getMenuModeBack()
    }

    /** Check if menu should respond to SELECT+START combo based on menu_mode_combo */
    fun shouldHandleSelectStartCombo(): Boolean {
        return appConfig.getMenuModeCombo()
    }

    /** Check if menu should respond to gamepad menu button based on menu_mode_gamepad */
    fun shouldHandleGamepadMenuButton(): Boolean {
        return appConfig.getMenuModeGamepad()
    }
}
