package com.vinaooo.revenger.input

import android.view.KeyEvent

/**
 * Owns the debounce timestamps for RetroMenu3's callback invocations and the post-menu-close
 * grace period that blocks only the button that closed the menu, extracted from [ControllerInput]
 * to keep that class's function count within detekt's `TooManyFunctions` threshold.
 * [ControllerInput]'s three event dispatchers call [executeMenuCallback] at the same points in
 * their logic as before this extraction -- only the receiver changed.
 */
class MenuCallbackDebouncer(private val callbacksProvider: () -> ControllerInputCallbacks) {

        companion object {
                private const val MENU_CALLBACK_DEBOUNCE_MS = 150L
        }

        private val callbacks get() = callbacksProvider()

        // Debouncing timestamps for menu callbacks to prevent rapid successive calls
        var lastMenuBackCallbackTime: Long = 0
        var lastMenuConfirmCallbackTime: Long = 0
        var lastMenuNavigateUpCallbackTime: Long = 0
        var lastMenuNavigateDownCallbackTime: Long = 0
        var lastMenuNavigateLeftCallbackTime: Long = 0
        var lastMenuNavigateRightCallbackTime: Long = 0
        var lastStartButtonCallbackTime: Long = 0
        var lastGamepadMenuButtonCallbackTime: Long = 0

        /**
         * Flag to keep interception active for a period after menu closes. This prevents
         * ACTION_UP from leaking to the game
         */
        private var keepInterceptingUntil: Long = 0L

        /** Which button closed the menu (to block only that button during grace period) */
        private var buttonThatClosedMenu: Int? = null

        /** Activate temporary interception for X ms after menu closes */
        fun keepInterceptingButtons(durationMs: Long = 500, closingButton: Int? = null) {
                keepInterceptingUntil = System.currentTimeMillis() + durationMs
                buttonThatClosedMenu = closingButton
        }

        /**
         * Checks if a SPECIFIC button should be blocked during the grace period. Blocks only the
         * button that closed the menu during the grace period. Does NOT block other buttons even
         * if the menu is "technically active".
         */
        fun shouldInterceptSpecificButton(keyCode: Int): Boolean {
                val menuActive = callbacks.shouldInterceptDpadForMenu()
                val now = System.currentTimeMillis()
                val gracePeriodActive = now < keepInterceptingUntil

                // Intercept if the menu is open, or if we're still in the grace period for the
                // specific button that closed it.
                return menuActive || (gracePeriodActive && keyCode == buttonThatClosedMenu)
        }

        /** Helper function to execute menu callbacks with debouncing protection */
        fun executeMenuCallback(
                callback: () -> Unit,
                lastExecutionTime: Long,
                updateTime: (Long) -> Unit
        ): Boolean {
                // First check if it's safe to execute menu operations
                if (!callbacks.isMenuOperationSafe()) {
                        return false
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastExecutionTime >= MENU_CALLBACK_DEBOUNCE_MS) {
                        callback()
                        updateTime(currentTime)
                        return true
                }
                return false
        }

        /**
         * Handles BUTTON_A-as-menu-confirm interception, shared by
         * [ControllerInput.processGamePadButtonEvent] and [ControllerInput.processKeyEvent].
         * Returns true if this call intercepted the event (caller should return true
         * immediately); false if [keyCode]/[action] don't match (caller should continue its own
         * logic).
         */
        fun interceptButtonAConfirm(keyCode: Int, action: Int): Boolean {
                if (keyCode != KeyEvent.KEYCODE_BUTTON_A || !callbacks.shouldInterceptDpadForMenu()) {
                        return false
                }

                android.util.Log.d("ControllerInput", "🔴 BUTTON_A intercepted - shouldInterceptDpadForMenu()=true")
                if (action == KeyEvent.ACTION_DOWN) {
                        android.util.Log.d("ControllerInput", "   → Executing menuConfirmCallback")
                        // NOTE: We do NOT block A here because it does not always close the menu
                        // Blocking will be done in onMenuClosedCallback
                        executeMenuCallback(callbacks.menuConfirmCallback, lastMenuConfirmCallbackTime) {
                                lastMenuConfirmCallbackTime = it
                        }
                }
                // CRITICAL: Consume BOTH ACTION_DOWN and ACTION_UP so ACTION_UP doesn't leak to
                // the core
                return true
        }

        /**
         * Resets all debounce timestamps and the post-close grace period. Used by
         * [ControllerInput.clearPendingInputs] and [ControllerInput.clearPendingInputsPreserveHeld].
         */
        fun reset() {
                lastMenuBackCallbackTime = 0L
                lastMenuConfirmCallbackTime = 0L
                lastMenuNavigateUpCallbackTime = 0L
                lastMenuNavigateDownCallbackTime = 0L
                lastMenuNavigateLeftCallbackTime = 0L
                lastMenuNavigateRightCallbackTime = 0L
                lastStartButtonCallbackTime = 0L
                lastGamepadMenuButtonCallbackTime = 0L

                keepInterceptingUntil = 0L
                buttonThatClosedMenu = null
        }
}
