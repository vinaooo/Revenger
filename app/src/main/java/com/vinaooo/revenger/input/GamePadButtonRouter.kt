package com.vinaooo.revenger.input

import android.view.KeyEvent

/**
 * Owns [ControllerInput.processGamePadButtonEvent]'s interception chain (BUTTON_A confirm,
 * BUTTON_B back, START, the gamepad menu button, the RetroMenu3 block-all gate, and the
 * SELECT+START combo/leak-block), extracted to keep that function within detekt's
 * `LongMethod`/`CyclomaticComplexMethod`/`ReturnCount` thresholds. [ControllerInput] keeps a
 * public `processGamePadButtonEvent` delegating to [process], so no external call site changes.
 *
 * This is a deliberately SEPARATE class from [KeyEventRouter] -- the two paths have small,
 * intentional behavioral differences (see the `DIVERGENCE` tests in `ControllerInput_test`)
 * that must never be unified into shared logic.
 */
class GamePadButtonRouter(
        private val callbacksProvider: () -> ControllerInputCallbacks,
        private val comboTrackerProvider: () -> ComboKeyLogTracker,
        private val callbackDebouncerProvider: () -> MenuCallbackDebouncer
) {

        companion object {
                /** Vendor keycode reported by some physical gamepads for their menu/hamburger button */
                private const val GAMEPAD_MENU_BUTTON_KEYCODE = -6
        }

        private val callbacks get() = callbacksProvider()
        private val comboTracker get() = comboTrackerProvider()
        private val callbackDebouncer get() = callbackDebouncerProvider()

        private fun keyName(keyCode: Int): String =
                when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A -> "A"
                        KeyEvent.KEYCODE_BUTTON_B -> "B"
                        KeyEvent.KEYCODE_BUTTON_START -> "START"
                        KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
                        else -> keyCode.toString()
                }

        private fun actionName(action: Int): String =
                if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"

        /** INTERCEPT BUTTON A for confirmation when menu is open. */
        private fun interceptButtonAConfirm(keyCode: Int, action: Int): Boolean {
                if (!callbackDebouncer.interceptButtonAConfirm(keyCode, action)) return false

                android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return true
        }

        /** INTERCEPT BUTTON B to go back when menu is open. */
        private fun interceptButtonB(keyCode: Int, action: Int): Boolean {
                if (keyCode != KeyEvent.KEYCODE_BUTTON_B ||
                                !callbackDebouncer.shouldInterceptSpecificButton(keyCode)
                ) {
                        return false
                }

                android.util.Log.d(
                        "ControllerInput",
                        "🔵 BUTTON_B intercepted - action=${actionName(action)}"
                )

                if (action == KeyEvent.ACTION_DOWN) {
                        val alreadyPressed = comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_B)
                        if (alreadyPressed) {
                                android.util.Log.d("ControllerInput", "   ↪️ B already in keyLog (hold) - consuming DOWN without callback")
                                return true
                        }

                        // Primeira vez pressionando: adicionar ao keyLog e executar callback
                        comboTracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_B)
                        android.util.Log.d("ControllerInput", "   → First B DOWN - executing menuBackCallback")
                        callbackDebouncer.executeMenuCallback(callbacks.menuBackCallback, callbackDebouncer.lastMenuBackCallbackTime) {
                                callbackDebouncer.lastMenuBackCallbackTime = it
                        }
                } else if (action == KeyEvent.ACTION_UP) {
                        // ACTION_UP: remove from keyLog to allow new interaction
                        val wasPressed = comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_B)
                        comboTracker.keyLog.remove(KeyEvent.KEYCODE_BUTTON_B)

                        if (wasPressed) {
                                android.util.Log.d("ControllerInput", "   → B ACTION_UP - cleared from keyLog")
                        } else {
                                android.util.Log.w("ControllerInput", "   ⚠️ B ACTION_UP without prior DOWN (orphan)")
                        }
                }
                // Consumir evento
                return true
        }

        /** INTERCEPT START button when menu is open (to close menu). */
        private fun interceptStartButton(keyCode: Int, action: Int): Boolean {
                if (keyCode != KeyEvent.KEYCODE_BUTTON_START || !callbacks.shouldHandleStartButton()) {
                        return false
                }

                if (action == KeyEvent.ACTION_DOWN) {
                        android.util.Log.d("ControllerInput", "START (GamePad) pressed while menu open - CLOSING MENU")
                        android.util.Log.d("ControllerInput", "   keyLog BEFORE startButtonCallback: ${comboTracker.keyLog}")
                        android.util.Log.d("ControllerInput", "   comboAlreadyTriggered BEFORE: ${comboTracker.getComboAlreadyTriggered()}")
                        callbackDebouncer.executeMenuCallback(
                                callbacks.startButtonCallback,
                                callbackDebouncer.lastStartButtonCallbackTime
                        ) { callbackDebouncer.lastStartButtonCallbackTime = it }
                        android.util.Log.d("ControllerInput", "   startButtonCallback() completed")
                }
                return true // Event intercepted - don't send to core
        }

        /** INTERCEPT GAMEPAD MENU BUTTON (☰). */
        private fun interceptGamepadMenuButton(keyCode: Int, action: Int): Boolean {
                if (keyCode != GAMEPAD_MENU_BUTTON_KEYCODE || !callbacks.shouldHandleGamepadMenuButton()) {
                        return false
                }

                if (action == KeyEvent.ACTION_DOWN) {
                        android.util.Log.d("ControllerInput", "GAMEPAD MENU BUTTON (☰) pressed - toggling menu")
                        callbackDebouncer.executeMenuCallback(
                                callbacks.gamepadMenuButtonCallback,
                                callbackDebouncer.lastGamepadMenuButtonCallbackTime
                        ) { callbackDebouncer.lastGamepadMenuButtonCallbackTime = it }
                }
                return true // Event intercepted - don't send to core
        }

        /**
         * BLOCK COMPLETELY all controls when RetroMenu3 is open, except those already handled
         * above (A, B, START, gamepad menu button).
         */
        private fun blockAllGamepadInput(keyCode: Int, action: Int): Boolean {
                val shouldBlock = callbacks.shouldBlockAllGamepadInput()
                android.util.Log.d(
                        "ControllerInput",
                        "🎮 processGamePadButtonEvent: shouldBlockAllGamepadInput() = " +
                                "$shouldBlock (keyCode: $keyCode, action: ${actionName(action)})"
                )
                if (!shouldBlock) return false

                android.util.Log.d("ControllerInput", "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)")
                return true // Block completely, don't send to core
        }

        /** Keep track of user input events; intercepts repeated presses. */
        private fun trackAndIntercept(keyCode: Int, action: Int): Boolean {
                if (!comboTracker.trackKeyLogAndComboReset(keyCode, action)) return false

                android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return true // Event intercepted (repeated press)
        }

        /**
         * Runs the SELECT+START combo check and blocks SELECT/START from leaking to the core
         * while the combo is fully held. Unlike [KeyEventRouter]'s equivalent, this only checks
         * `contains(START) && contains(SELECT)` -- a third button held does not prevent the
         * block (see `DIVERGENCE` test).
         */
        private fun checkComboAndBlockLeak(keyCode: Int): Boolean {
                comboTracker.checkMenuKeyCombo()
                android.util.Log.d("ControllerInput", "🔍 After checkMenuKeyCombo - checking final decision...")

                val isComboButtonKey =
                        keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
                val comboFullyHeld =
                        comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                                comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)

                if (!isComboButtonKey || !comboFullyHeld) return false

                android.util.Log.d("ControllerInput", "� Blocking ${keyName(keyCode)} (part of SELECT+START combo) - preventing leak to core")
                android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return true // Event intercepted - don't send to core
        }

        fun process(keyCode: Int, action: Int): Boolean {
                val intercepted =
                        interceptButtonAConfirm(keyCode, action) ||
                                interceptButtonB(keyCode, action) ||
                                interceptStartButton(keyCode, action) ||
                                interceptGamepadMenuButton(keyCode, action) ||
                                blockAllGamepadInput(keyCode, action) ||
                                trackAndIntercept(keyCode, action) ||
                                checkComboAndBlockLeak(keyCode)

                if (!intercepted) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🟢 Event ${keyName(keyCode)} ${actionName(action)} → SENDING TO CORE (not intercepted)"
                        )
                        android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

                return intercepted // false = event not intercepted, send to core
        }
}
