package com.vinaooo.revenger.input

import android.view.KeyEvent
import android.view.MotionEvent
import com.vinaooo.revenger.retroview.RetroView

class ControllerInput {
        companion object {
                /** Combination to open the menu */
                val KEYCOMBO_MENU =
                        setOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT)

                /** Any of these keys will not be piped to the RetroView */
                val EXCLUDED_KEYS =
                        setOf(
                                KeyEvent.KEYCODE_VOLUME_DOWN,
                                KeyEvent.KEYCODE_VOLUME_UP,
                                KeyEvent.KEYCODE_BACK,
                                KeyEvent.KEYCODE_POWER
                        )
        }

        /** Keys that have already triggered an action and should remain blocked until they receive ACTION_UP */
        private val blockedUntilKeyUp = mutableSetOf<Int>()

        /**
         * Bundles all of this class's external callbacks/predicates (see
         * [ControllerInputCallbacks]). Each field below is exposed as a delegate
         * property reading/writing through this instance, so existing external
         * assignment call sites (`controllerInput.someCallback = { ... }`) keep
         * compiling and behaving unchanged.
         */
        var callbacks: ControllerInputCallbacks = ControllerInputCallbacks()

        /**
         * SELECT+START combo detection and key-log state, extracted to keep this class's
         * function count within detekt's `TooManyFunctions` threshold.
         */
        val comboTracker = ComboKeyLogTracker { callbacks }

        /**
         * Debounced menu-callback invocation and the post-menu-close grace period, extracted for
         * the same reason as [comboTracker].
         */
        val callbackDebouncer = MenuCallbackDebouncer { callbacks }

        /**
         * DPAD/left-analog single-trigger menu navigation and pass-through to the core for
         * `processMotionEvent`, extracted for the same reason as [comboTracker].
         */
        private val motionEventRouter = MotionEventRouter({ callbacks }, { callbackDebouncer })

        /**
         * BUTTON_A/B/START/gamepad-menu-button interception and the SELECT+START combo leak
         * block for `processGamePadButtonEvent`, extracted for the same reason as [comboTracker].
         * Kept as a separate class from the equivalent KeyEvent-path router -- see its KDoc for
         * why the two must not be merged.
         */
        private val gamePadButtonRouter =
                GamePadButtonRouter({ callbacks }, { comboTracker }, { callbackDebouncer })

        /**
         * START-closes-menu/BUTTON_A/B/DPAD interception, the RetroMenu3 block-all gate, and the
         * SELECT+START combo leak block for `processKeyEvent`, extracted for the same reason as
         * [comboTracker]. Kept as a separate class from [gamePadButtonRouter] -- see its KDoc for
         * why the two must not be merged.
         */
        private val keyEventRouter =
                KeyEventRouter({ callbacks }, { comboTracker }, { callbackDebouncer })

        /** The callback for when the user inputs the SELECT+START combo (RetroMenu3) */
        var selectStartComboCallback: () -> Unit
                get() = callbacks.selectStartComboCallback
                set(value) { callbacks = callbacks.copy(selectStartComboCallback = value) }

        /** The callback for when the user presses START alone (to close RetroMenu3) */
        var startButtonCallback: () -> Unit
                get() = callbacks.startButtonCallback
                set(value) { callbacks = callbacks.copy(startButtonCallback = value) }

        /** Function to check if SELECT+START combo should trigger menu */
        var shouldHandleSelectStartCombo: () -> Boolean
                get() = callbacks.shouldHandleSelectStartCombo
                set(value) { callbacks = callbacks.copy(shouldHandleSelectStartCombo = value) }

        /** Function to check if START button alone should trigger callback */
        var shouldHandleStartButton: () -> Boolean
                get() = callbacks.shouldHandleStartButton
                set(value) { callbacks = callbacks.copy(shouldHandleStartButton = value) }

        /** Function to check if gamepad menu button should trigger menu */
        var shouldHandleGamepadMenuButton: () -> Boolean
                get() = callbacks.shouldHandleGamepadMenuButton
                set(value) { callbacks = callbacks.copy(shouldHandleGamepadMenuButton = value) }

        /** The callback for when the user presses the gamepad menu button */
        var gamepadMenuButtonCallback: () -> Unit
                get() = callbacks.gamepadMenuButtonCallback
                set(value) { callbacks = callbacks.copy(gamepadMenuButtonCallback = value) }

        /** Function to check if devemos bloquear TODOS os inputs do gamepad */
        var shouldBlockAllGamepadInput: () -> Boolean
                get() = callbacks.shouldBlockAllGamepadInput
                set(value) { callbacks = callbacks.copy(shouldBlockAllGamepadInput = value) }

        /** Function to check if RetroMenu3 is currently open */
        var isRetroMenu3Open: () -> Boolean
                get() = callbacks.isRetroMenu3Open
                set(value) { callbacks = callbacks.copy(isRetroMenu3Open = value) }

        /** Callbacks for RetroMenu3 navigation */
        var menuNavigateUpCallback: () -> Unit
                get() = callbacks.menuNavigateUpCallback
                set(value) { callbacks = callbacks.copy(menuNavigateUpCallback = value) }
        var menuNavigateDownCallback: () -> Unit
                get() = callbacks.menuNavigateDownCallback
                set(value) { callbacks = callbacks.copy(menuNavigateDownCallback = value) }
        var menuNavigateLeftCallback: () -> Unit
                get() = callbacks.menuNavigateLeftCallback
                set(value) { callbacks = callbacks.copy(menuNavigateLeftCallback = value) }
        var menuNavigateRightCallback: () -> Unit
                get() = callbacks.menuNavigateRightCallback
                set(value) { callbacks = callbacks.copy(menuNavigateRightCallback = value) }
        var menuConfirmCallback: () -> Unit
                get() = callbacks.menuConfirmCallback
                set(value) { callbacks = callbacks.copy(menuConfirmCallback = value) }
        var menuBackCallback: () -> Unit
                get() = callbacks.menuBackCallback
                set(value) { callbacks = callbacks.copy(menuBackCallback = value) }

        /**
         * Clears input state to prevent event leakage during transitions. Resets
         * debounces, keyLog, combo, and KEY_DOWN/UP tracking.
         */
        fun clearPendingInputs() {
                callbackDebouncer.reset()

                comboTracker.keyLog.clear()
                comboTracker.resetComboAlreadyTriggered()

                blockedUntilKeyUp.clear()

                android.util.Log.d("ControllerInput", "[CLEAR_STATE] clearPendingInputs() applied: debounces+keyLog+combo+tracking reset")
        }

        /**
         * Version that preserves currently held keys (e.g., B held when returning from
         * submenu). Resets only debounces and combo/grace flags, keeping keyLog +
         * blockedUntilKeyUp so the corresponding ACTION_UP is consumed in the next fragment
         * without reprocessing DOWN.
         */
        fun clearPendingInputsPreserveHeld() {
                callbackDebouncer.reset()

                comboTracker.resetComboAlreadyTriggered()

                android.util.Log.d("ControllerInput", "[CLEAR_STATE] partial clear preserving held keys (debounces+combo reset)")
        }

        /** Function to check if we should intercept DPAD for menu */
        var shouldInterceptDpadForMenu: () -> Boolean
                get() = callbacks.shouldInterceptDpadForMenu
                set(value) { callbacks = callbacks.copy(shouldInterceptDpadForMenu = value) }

        /**
         * Function to check if it's safe to execute menu callbacks (no critical operations in
         * progress)
         */
        var isMenuOperationSafe: () -> Boolean
                get() = callbacks.isMenuOperationSafe
                set(value) { callbacks = callbacks.copy(isMenuOperationSafe = value) }

        fun processGamePadButtonEvent(keyCode: Int, action: Int): Boolean =
                gamePadButtonRouter.process(keyCode, action)
        fun processKeyEvent(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean? =
                keyEventRouter.process(keyCode, event, retroView)

        fun processMotionEvent(event: MotionEvent, retroView: RetroView): Boolean? =
                motionEventRouter.process(event, retroView)
}
