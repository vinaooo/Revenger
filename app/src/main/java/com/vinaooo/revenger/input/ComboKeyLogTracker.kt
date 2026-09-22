package com.vinaooo.revenger.input

import android.view.KeyEvent

/**
 * Owns the SELECT+START menu-combo detection state and the physical key log it is derived from,
 * extracted from [ControllerInput] to keep that class's function count within detekt's
 * `TooManyFunctions` threshold. [ControllerInput]'s three event dispatchers keep reading/writing
 * [keyLog] directly and call [checkMenuKeyCombo] / [trackKeyLogAndComboReset] at the same points
 * in their logic as before this extraction -- only the receiver changed.
 */
class ComboKeyLogTracker(private val callbacksProvider: () -> ControllerInputCallbacks) {

        companion object {
                private const val COMBO_COOLDOWN_MS = 500L // 500ms cooldown between combo detections
                private const val MENU_CLOSE_DEBOUNCE_MS = 200L // 200ms debounce after menu closes
        }

        private val callbacks get() = callbacksProvider()

        /** Set of keys currently being held by the user. Read/written directly by [ControllerInput]'s dispatchers. */
        val keyLog = mutableSetOf<Int>()

        /**
         * Flag to prevent combo from being detected multiple times while buttons are held Reset
         * only when BOTH combo buttons are released
         */
        private var comboAlreadyTriggered = false

        /** Timestamp of last combo detection to prevent rapid re-triggers */
        private var lastComboTriggerTime = 0L

        /** Timestamp to prevent combo detection immediately after menu closes */
        private var menuCloseDebounceTime = 0L

        /**
         * Clears the keyLog to avoid combo detection after closing the menu.
         *
         * IMPORTANT: Resets comboAlreadyTriggered ONLY if the menu is no longer open. If the menu
         * is still open, keeps the flag to avoid false detections.
         */
        fun clearKeyLog() {
                keyLog.clear()

                // 🔧 FIX: Always reset comboAlreadyTriggered when clearing key log
                // This ensures combo detection works properly after menu dismissal
                // regardless of timing or menu state during the clear operation
                comboAlreadyTriggered = false

                lastComboTriggerTime = 0L // Reset cooldown timer to allow immediate combo detection
                menuCloseDebounceTime = System.currentTimeMillis() // Set debounce timestamp
        }

        /**
         * Clears only the menu action buttons (A, B, D-PAD) from the keyLog. Does NOT clear
         * START/SELECT to avoid combo issues. Should be called when the menu closes to prevent
         * "wasAlreadyPressed" false positives.
         */
        fun clearMenuActionButtons() {
                // Remove only menu buttons (A, B, D-PAD), keep START/SELECT
                keyLog.removeIf { keyCode ->
                        keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                                keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                }
        }

        /** Getter for comboAlreadyTriggered (for debugging) */
        fun getComboAlreadyTriggered(): Boolean = comboAlreadyTriggered

        /** Reset comboAlreadyTriggered flag (used when menu is being dismissed) */
        fun resetComboAlreadyTriggered() {
                comboAlreadyTriggered = false
        }

        fun updateMenuCloseDebounceTime() {
                menuCloseDebounceTime = System.currentTimeMillis()
        }

        /**
         * Tracks [keyLog] membership and resets [comboAlreadyTriggered] once both combo buttons
         * are released, shared by [ControllerInput.processGamePadButtonEvent] and
         * [ControllerInput.processKeyEvent]. Returns true if this was a repeated ACTION_DOWN that
         * should be swallowed (caller should return true immediately without further processing
         * this key).
         */
        fun trackKeyLogAndComboReset(keyCode: Int, action: Int): Boolean {
                when (action) {
                        KeyEvent.ACTION_DOWN -> {
                                val wasAlreadyPressed = keyLog.contains(keyCode)
                                keyLog.add(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "⬇️  ACTION_DOWN: keyCode=$keyCode, " +
                                                "wasAlreadyPressed=$wasAlreadyPressed, keyLog=$keyLog"
                                )

                                // If the button was already pressed, don't check combo again
                                if (wasAlreadyPressed) {
                                        android.util.Log.d("ControllerInput", "   🔴 BLOCKED - Button was already pressed (repeat)")
                                        return true
                                }
                        }
                        KeyEvent.ACTION_UP -> {
                                keyLog.remove(keyCode)
                                android.util.Log.d("ControllerInput", "⬆️  ACTION_UP: keyCode=$keyCode, keyLog=$keyLog")

                                // Reset combo flag ONLY when BOTH combo buttons are released
                                val isComboButtonKey = keyCode in ControllerInput.KEYCOMBO_MENU
                                val bothComboButtonsReleased =
                                        !keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                                                !keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)

                                if (isComboButtonKey && bothComboButtonsReleased) {
                                        if (comboAlreadyTriggered) {
                                                android.util.Log.d("ControllerInput", "BOTH combo buttons released, resetting comboAlreadyTriggered")
                                        }
                                        comboAlreadyTriggered = false
                                }
                        }
                }
                return false
        }

        /** Diagnostic dump of the inputs [checkMenuKeyCombo] bases its decision on. */
        private fun logComboCheckState(hasSelectAndStart: Boolean, timeSinceLastTrigger: Long, timeSinceMenuClose: Long) {
                android.util.Log.d("ControllerInput", "┌─────────────────────────────────────────────────────────")
                android.util.Log.d("ControllerInput", "│ checkMenuKeyCombo CALLED")
                android.util.Log.d("ControllerInput", "│ keyLog: $keyLog")
                android.util.Log.d("ControllerInput", "│ hasSelectAndStart: $hasSelectAndStart")
                android.util.Log.d("ControllerInput", "│ comboAlreadyTriggered: $comboAlreadyTriggered")
                android.util.Log.d("ControllerInput", "│ timeSinceLastTrigger: ${timeSinceLastTrigger}ms (cooldown: ${COMBO_COOLDOWN_MS}ms)")
                android.util.Log.d("ControllerInput", "│ shouldHandleSelectStartCombo(): ${callbacks.shouldHandleSelectStartCombo()}")
                android.util.Log.d("ControllerInput", "│ SELECT pressed: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)}")
                android.util.Log.d("ControllerInput", "│ START pressed: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)}")
                android.util.Log.d("ControllerInput", "│ timeSinceMenuClose: ${timeSinceMenuClose}ms (debounce: ${MENU_CLOSE_DEBOUNCE_MS}ms)")
        }

        /** Diagnostic dump explaining why [checkMenuKeyCombo] did NOT fire the combo callback. */
        private fun logComboRejection(hasSelectAndStart: Boolean, timeSinceLastTrigger: Long, timeSinceMenuClose: Long) {
                android.util.Log.d("ControllerInput", "❌ COMBO REJECTED - Checking missing conditions:")
                android.util.Log.d("ControllerInput", "   hasSelectAndStart: $hasSelectAndStart (precisa ser true)")
                android.util.Log.d("ControllerInput", "   comboAlreadyTriggered: $comboAlreadyTriggered (precisa ser false)")
                android.util.Log.d(
                        "ControllerInput",
                        "   shouldHandleSelectStartCombo(): " +
                                "${callbacks.shouldHandleSelectStartCombo()} (precisa ser true)"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "   timeSinceLastTrigger: ${timeSinceLastTrigger}ms " +
                                "(precisa ser > ${COMBO_COOLDOWN_MS}ms)"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "   timeSinceMenuClose: ${timeSinceMenuClose}ms " +
                                "(precisa ser > ${MENU_CLOSE_DEBOUNCE_MS}ms)"
                )

                if (comboAlreadyTriggered) {
                        // DISABLED: Phase 5.1f - Performance optimization
                        // android.util.Log.d(
                        //         "ControllerInput",
                        //         "│    - comboAlreadyTriggered = true (already triggered)"
                        // )
                        // 🔍 DEBUGGING: Only log as RARE BUG if menu is NOT open
                        // If shouldHandleSelectStartCombo() = true, menu is closed (should
                        // have been reset)
                        // If shouldHandleSelectStartCombo() = false, menu is open (expected
                        // behavior)
                        if (callbacks.shouldHandleSelectStartCombo()) {
                                // Menu is CLOSED but flag is still true - this may happen
                                // due to timing
                                // The flag will be reset by onMenuClosedCallback, but this
                                // check happens first
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    ℹ️  Menu closed, comboAlreadyTriggered=true " +
                                                "(will be reset by callback)"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│       lastComboTriggerTime: $lastComboTriggerTime " +
                                                "(${timeSinceLastTrigger}ms ago)"
                                )
                                android.util.Log.d("ControllerInput", "│       This is expected timing - callback will reset flag")
                        } else {
                                // Menu is OPEN - this is expected, not a bug
                        }
                }
        }

        /** Check if we should be showing the user the menu */
        fun checkMenuKeyCombo() {
                // Check if we have exactly the two buttons pressed
                val hasSelectAndStart = keyLog.containsAll(ControllerInput.KEYCOMBO_MENU) && keyLog.size == 2

                // Check cooldown to avoid very fast detections
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTrigger = currentTime - lastComboTriggerTime
                val timeSinceMenuClose = currentTime - menuCloseDebounceTime

                // ENABLED FOR DEBUG: Combo detection logs
                logComboCheckState(hasSelectAndStart, timeSinceLastTrigger, timeSinceMenuClose)

                val comboEligible = !comboAlreadyTriggered && callbacks.shouldHandleSelectStartCombo()
                val cooldownAndDebounceElapsed =
                        timeSinceLastTrigger > COMBO_COOLDOWN_MS &&
                                timeSinceMenuClose > MENU_CLOSE_DEBOUNCE_MS

                if (hasSelectAndStart && comboEligible && cooldownAndDebounceElapsed) {
                        // ENABLED FOR DEBUG: Combo detection success logs
                        android.util.Log.d("ControllerInput", "│ ✅ ALL CONDITIONS MET - COMBO DETECTED!")
                        android.util.Log.d("ControllerInput", "│    - hasSelectAndStart: $hasSelectAndStart")
                        android.util.Log.d("ControllerInput", "│    - comboAlreadyTriggered: $comboAlreadyTriggered")
                        android.util.Log.d("ControllerInput", "│    - shouldHandleSelectStartCombo(): ${callbacks.shouldHandleSelectStartCombo()}")
                        android.util.Log.d("ControllerInput", "│    - timeSinceLastTrigger: ${timeSinceLastTrigger}ms > ${COMBO_COOLDOWN_MS}ms")
                        android.util.Log.d("ControllerInput", "│    - timeSinceMenuClose: ${timeSinceMenuClose}ms > ${MENU_CLOSE_DEBOUNCE_MS}ms")
                        comboAlreadyTriggered = true // Mark combo as triggered
                        lastComboTriggerTime = currentTime
                        android.util.Log.d("ControllerInput", "│ 🔵 comboAlreadyTriggered SET TO TRUE at timestamp: $lastComboTriggerTime")
                        callbacks.selectStartComboCallback()
                        android.util.Log.d("ControllerInput", "│ comboAlreadyTriggered NOW: true, timestamp: $lastComboTriggerTime")
                } else {
                        logComboRejection(hasSelectAndStart, timeSinceLastTrigger, timeSinceMenuClose)
                }
        }
}
