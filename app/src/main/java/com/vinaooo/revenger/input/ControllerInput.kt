package com.vinaooo.revenger.input

import android.content.Context
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView

class ControllerInput(private val context: Context) {
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

        // Fixed threshold values for single-trigger system
        private val dpadThreshold: Float = 0.1f // Physical DPAD - more responsive
        private val leftAnalogThreshold: Float = 0.7f // Left analog - less sensitive
        private val rightAnalogThreshold: Float = 0.7f // Right analog - less sensitive

        // Single trigger system - tracks previous state to detect transitions (UP/DOWN only)
        private data class DirectionalState(var up: Boolean = false, var down: Boolean = false)

        // Track state for each input type to implement single-trigger navigation
        private val dpadState = DirectionalState()
        private val leftAnalogState = DirectionalState()
        private val rightAnalogState = DirectionalState()

        /** Set of keys currently being held by the user */
        private val keyLog = mutableSetOf<Int>()

        /**
         * Set of keys that were pressed when menu opened (to block their ACTION_UP after menu
         * closes)
         */
        private val keysToBlockAfterMenuClose = mutableSetOf<Int>()

        /**
         * Flag to prevent combo from being detected multiple times while buttons are held Reset
         * only when BOTH combo buttons are released
         */
        private var comboAlreadyTriggered = false

        /** Timestamp of last combo detection to prevent rapid re-triggers */
        private var lastComboTriggerTime = 0L
        private val COMBO_COOLDOWN_MS = 500L // 500ms cooldown between combo detections

        /** Timestamp to prevent combo detection immediately after menu closes */
        private var menuCloseDebounceTime = 0L
        private val MENU_CLOSE_DEBOUNCE_MS = 200L // 200ms debounce after menu closes

        /**
         * Clears the keyLog to avoid combo detection after closing the menu.
         *
         * IMPORTANT: Resets comboAlreadyTriggered ONLY if the menu is no longer open. If the menu
         * is still open, keeps the flag to avoid false detections.
         */
        fun clearKeyLog() {
                android.util.Log.d(
                        "ControllerInput",
                        "🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "🔥 [CLEAR_KEYLOG] ===== clearKeyLog() CALLED ====="
                )
                android.util.Log.d(
                        "ControllerInput",
                        "🔥 [CLEAR_KEYLOG] Timestamp: ${System.currentTimeMillis()}"
                )
                android.util.Log.d("ControllerInput", "")
                android.util.Log.d("ControllerInput", "🧹 clearKeyLog() CALLED")
                android.util.Log.d(
                        "ControllerInput",
                        "   BEFORE: keyLog=$keyLog, comboAlreadyTriggered=$comboAlreadyTriggered"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "   BEFORE: isRetroMenu3Open=${isRetroMenu3Open?.invoke() ?: false}"
                )
                keyLog.clear()

                // 🔧 FIX: Reset comboAlreadyTriggered only if menu is not open
                // This prevents false detections when menu is closed, but avoids
                // resetting the flag if menu is still open (ex: during operations)
                if (!isRetroMenu3Open?.invoke()!!) {
                        android.util.Log.d(
                                "ControllerInput",
                                "   ✅ Menu is closed, resetting comboAlreadyTriggered to prevent double-press bug"
                        )
                        comboAlreadyTriggered = false
                } else {
                        android.util.Log.d(
                                "ControllerInput",
                                "   ⏳ Menu still open, keeping comboAlreadyTriggered=true to prevent false detections"
                        )
                }

                lastComboTriggerTime = 0L // Reset cooldown timer to allow immediate combo detection
                menuCloseDebounceTime = System.currentTimeMillis() // Set debounce timestamp
                android.util.Log.d(
                        "ControllerInput",
                        "   AFTER: keyLog=$keyLog, comboAlreadyTriggered=$comboAlreadyTriggered, menuCloseDebounceTime=$menuCloseDebounceTime"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "🔥 [CLEAR_KEYLOG] ===== clearKeyLog() COMPLETED ====="
                )
                android.util.Log.d(
                        "ControllerInput",
                        "🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥 🔥"
                )
        }

        /** The callback for when the user inputs the menu key-combination */
        var menuCallback: () -> Unit = {}

        /** The callback for when the user inputs the SELECT+START combo (RetroMenu3) */
        var selectStartComboCallback: () -> Unit = {}

        /** The callback for when the user presses START alone (to close RetroMenu3) */
        var startButtonCallback: () -> Unit = {}

        /** Function to check if SELECT+START combo should trigger menu */
        var shouldHandleSelectStartCombo: () -> Boolean = { true }

        /** Function to check if START button alone should trigger callback */
        var shouldHandleStartButton: () -> Boolean = { false }

        /** Function to check if devemos bloquear TODOS os inputs do gamepad */
        var shouldBlockAllGamepadInput: () -> Boolean = { false }

        /** Function to check if RetroMenu3 is currently open */
        var isRetroMenu3Open: () -> Boolean = { false }

        /** Callbacks for RetroMenu3 navigation */
        var menuNavigateUpCallback: () -> Unit = {}
        var menuNavigateDownCallback: () -> Unit = {}
        var menuConfirmCallback: () -> Unit = {}
        var menuBackCallback: () -> Unit = {}

        /** Getter for comboAlreadyTriggered (for debugging) */
        fun getComboAlreadyTriggered(): Boolean = comboAlreadyTriggered

        /** Function to check if devemos interceptar DPAD para menu */
        var shouldInterceptDpadForMenu: () -> Boolean = { false }

        /**
         * Check for single-trigger directional input (UP/DOWN only) Returns the keycode if there's
         * a NEW press (transition from false to true) Returns null if no new input or input is
         * being held
         */
        private fun checkSingleTrigger(
                currentUp: Boolean,
                currentDown: Boolean,
                previousState: DirectionalState,
                inputName: String
        ): Int? {
                var triggeredKeyCode: Int? = null

                // Check UP transition (false -> true)
                if (currentUp && !previousState.up) {
                        triggeredKeyCode = KeyEvent.KEYCODE_DPAD_UP
                }
                // Check DOWN transition (false -> true)
                else if (currentDown && !previousState.down) {
                        triggeredKeyCode = KeyEvent.KEYCODE_DPAD_DOWN
                }

                // Update previous state (UP/DOWN only)
                previousState.up = currentUp
                previousState.down = currentDown

                return triggeredKeyCode
        }

        /** Controller numbers are [1, inf), we need [0, inf) */
        private fun getPort(event: InputEvent): Int =
                ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        /** Check if we should be showing the user the menu */
        private fun checkMenuKeyCombo() {
                android.util.Log.d(
                        "ControllerInput",
                        "🔍 checkMenuKeyCombo() - INÍCIO DA VERIFICAÇÃO"
                )
                android.util.Log.d("ControllerInput", "   keyLog atual: $keyLog")
                android.util.Log.d(
                        "ControllerInput",
                        "   comboAlreadyTriggered: $comboAlreadyTriggered"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "   shouldHandleSelectStartCombo(): ${shouldHandleSelectStartCombo()}"
                )

                // Check if we have exactly the two buttons pressed
                val hasSelectAndStart = keyLog.containsAll(KEYCOMBO_MENU) && keyLog.size == 2

                // Check cooldown to avoid very fast detections
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTrigger = currentTime - lastComboTriggerTime

                // Log para debug - DETALHADO
                android.util.Log.d(
                        "ControllerInput",
                        "┌─────────────────────────────────────────────────────────"
                )
                android.util.Log.d("ControllerInput", "│ checkMenuKeyCombo CALLED")
                android.util.Log.d("ControllerInput", "│ keyLog: $keyLog")
                android.util.Log.d("ControllerInput", "│ hasSelectAndStart: $hasSelectAndStart")
                android.util.Log.d(
                        "ControllerInput",
                        "│ comboAlreadyTriggered: $comboAlreadyTriggered"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "│ timeSinceLastTrigger: ${timeSinceLastTrigger}ms (cooldown: ${COMBO_COOLDOWN_MS}ms)"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "│ shouldHandleSelectStartCombo(): ${shouldHandleSelectStartCombo()}"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "│ SELECT pressed: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)}"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "│ START pressed: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)}"
                )
                val timeSinceMenuClose = currentTime - menuCloseDebounceTime
                android.util.Log.d(
                        "ControllerInput",
                        "│ timeSinceMenuClose: ${timeSinceMenuClose}ms (debounce: ${MENU_CLOSE_DEBOUNCE_MS}ms)"
                )

                if (hasSelectAndStart &&
                                !comboAlreadyTriggered &&
                                shouldHandleSelectStartCombo() &&
                                timeSinceLastTrigger > COMBO_COOLDOWN_MS &&
                                timeSinceMenuClose > MENU_CLOSE_DEBOUNCE_MS
                ) {

                        android.util.Log.d(
                                "ControllerInput",
                                "│ ✅ ALL CONDITIONS MET - COMBO DETECTED!"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "│    - hasSelectAndStart: $hasSelectAndStart"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "│    - comboAlreadyTriggered: $comboAlreadyTriggered"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "│    - shouldHandleSelectStartCombo(): ${shouldHandleSelectStartCombo()}"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "│    - timeSinceLastTrigger: ${timeSinceLastTrigger}ms > ${COMBO_COOLDOWN_MS}ms"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "│    - timeSinceMenuClose: ${timeSinceMenuClose}ms > ${MENU_CLOSE_DEBOUNCE_MS}ms"
                        )
                        comboAlreadyTriggered = true // Mark combo as triggered
                        lastComboTriggerTime = currentTime
                        android.util.Log.d(
                                "ControllerInput",
                                "│ 🔵 comboAlreadyTriggered SET TO TRUE at timestamp: $lastComboTriggerTime"
                        )
                        selectStartComboCallback()
                        android.util.Log.d(
                                "ControllerInput",
                                "│ comboAlreadyTriggered NOW: true, timestamp: $lastComboTriggerTime"
                        )
                } else {
                        android.util.Log.d(
                                "ControllerInput",
                                "❌ COMBO REJEITADO - Verificando condições faltantes:"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "   hasSelectAndStart: $hasSelectAndStart (precisa ser true)"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "   comboAlreadyTriggered: $comboAlreadyTriggered (precisa ser false)"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "   shouldHandleSelectStartCombo(): ${shouldHandleSelectStartCombo()} (precisa ser true)"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "   timeSinceLastTrigger: ${timeSinceLastTrigger}ms (precisa ser > ${COMBO_COOLDOWN_MS}ms)"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "   timeSinceMenuClose: ${timeSinceMenuClose}ms (precisa ser > ${MENU_CLOSE_DEBOUNCE_MS}ms)"
                        )

                        if (!hasSelectAndStart) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - hasSelectAndStart = false"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - SELECT in keyLog: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)}"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - START in keyLog: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)}"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - keyLog contents: $keyLog"
                                )
                        }
                        if (comboAlreadyTriggered) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - comboAlreadyTriggered = true (already triggered)"
                                )
                                // 🔍 DEBUGGING: Only log as RARE BUG if menu is NOT open
                                // If shouldHandleSelectStartCombo() = true, menu is closed (should
                                // have been reset)
                                // If shouldHandleSelectStartCombo() = false, menu is open (expected
                                // behavior)
                                if (shouldHandleSelectStartCombo()) {
                                        // Menu is CLOSED but flag is still true - this is the real
                                        // bug!
                                        android.util.Log.w(
                                                "ControllerInput",
                                                "│    ⚠️ REAL BUG: Menu closed but comboAlreadyTriggered still true!"
                                        )
                                        android.util.Log.w(
                                                "ControllerInput",
                                                "│       lastComboTriggerTime: $lastComboTriggerTime (${timeSinceLastTrigger}ms ago)"
                                        )
                                        android.util.Log.w(
                                                "ControllerInput",
                                                "│       Flag should have been reset when menu closed"
                                        )
                                } else {
                                        // Menu is OPEN - this is expected, not a bug
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "│    ℹ️  Menu is open, flag=true is correct (not a bug)"
                                        )
                                }
                        }
                        if (!shouldHandleSelectStartCombo()) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - shouldHandleSelectStartCombo() = false (menu already open?)"
                                )
                                android.util.Log.w(
                                        "ControllerInput",
                                        "│    ⚠️ Menu is already open? This is expected behavior."
                                )
                        }
                        if (timeSinceLastTrigger <= COMBO_COOLDOWN_MS) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - cooldown active (${timeSinceLastTrigger}ms < ${COMBO_COOLDOWN_MS}ms)"
                                )
                                android.util.Log.w(
                                        "ControllerInput",
                                        "│    ⚠️ User pressing too fast! Wait ${COMBO_COOLDOWN_MS - timeSinceLastTrigger}ms more"
                                )
                        }
                        if (timeSinceMenuClose <= MENU_CLOSE_DEBOUNCE_MS) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    - menu close debounce active (${timeSinceMenuClose}ms < ${MENU_CLOSE_DEBOUNCE_MS}ms)"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "│    ℹ️ Menu just closed, preventing immediate re-open"
                                )
                        }
                }
                android.util.Log.d("ControllerInput", "🔚 checkMenuKeyCombo() - FIM DA VERIFICAÇÃO")
                android.util.Log.d(
                        "ControllerInput",
                        "   Estado final - comboAlreadyTriggered: $comboAlreadyTriggered"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "└─────────────────────────────────────────────────────────"
                )
        }

        fun processGamePadButtonEvent(keyCode: Int, action: Int) {
                // INTERCEPT BUTTON A for confirmation when menu is open
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A && shouldInterceptDpadForMenu()) {
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "BUTTON_A (GamePad) intercepted for menu confirmation"
                                )
                                menuConfirmCallback()
                        }
                        return // Consume the event, don't process further
                }

                // INTERCEPT BUTTON B to go back when menu is open
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && shouldInterceptDpadForMenu()) {
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "BUTTON_B (GamePad) intercepted for menu back"
                                )
                                menuBackCallback()
                        }
                        return // Consume the event, don't process further
                }

                // INTERCEPT START button when menu is open (to close menu)
                if (keyCode == KeyEvent.KEYCODE_BUTTON_START && shouldHandleStartButton()) {
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "START (GamePad) pressed while menu open - CLOSING MENU"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   keyLog BEFORE startButtonCallback: $keyLog"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   comboAlreadyTriggered BEFORE: $comboAlreadyTriggered"
                                )
                                startButtonCallback()
                                // Reset comboAlreadyTriggered when START closes menu
                                comboAlreadyTriggered = false
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   ✅ comboAlreadyTriggered reset to false (START closed menu)"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   startButtonCallback() completed"
                                )
                        }
                        return // Consume the event, don't process further
                }

                /* Keep track of user input events */
                when (action) {
                        KeyEvent.ACTION_DOWN -> {
                                val wasAlreadyPressed = keyLog.contains(keyCode)
                                keyLog.add(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "GamePad ACTION_DOWN: $keyCode, wasAlreadyPressed: $wasAlreadyPressed, keyLog: $keyLog"
                                )

                                // If the button was already pressed, don't check combo again
                                if (wasAlreadyPressed) {
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "Ignoring repeated GamePad ACTION_DOWN for $keyCode"
                                        )
                                        return // Ignore repeated event
                                }
                        }
                        KeyEvent.ACTION_UP -> {
                                keyLog.remove(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "GamePad ACTION_UP: $keyCode, keyLog: $keyLog"
                                )

                                // Reset combo flag ONLY when BOTH combo buttons are released
                                if ((keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                                                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) &&
                                                !keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                                                !keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)
                                ) {

                                        if (comboAlreadyTriggered) {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "BOTH combo buttons released (GamePad), resetting comboAlreadyTriggered"
                                                )
                                        }
                                        comboAlreadyTriggered = false
                                }
                        }
                }

                checkMenuKeyCombo()
        }

        /**
         * Capture currently pressed keys when menu opens These keys will have their ACTION_UP
         * blocked after menu closes to prevent partial signals
         */
        fun captureKeysOnMenuOpen() {
                android.util.Log.d(
                        "ControllerInput",
                        "📸 captureKeysOnMenuOpen() - capturing current keyLog: $keyLog"
                )
                keysToBlockAfterMenuClose.clear()
                keysToBlockAfterMenuClose.addAll(keyLog)
                android.util.Log.d(
                        "ControllerInput",
                        "   keysToBlockAfterMenuClose: $keysToBlockAfterMenuClose"
                )
        }

        /**
         * Clear blocked keys after a delay (give time for ACTION_UP events to be processed and
         * blocked)
         */
        fun clearBlockedKeysDelayed() {
                android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(
                                { keysToBlockAfterMenuClose.clear() },
                                500
                        ) // 500ms delay to ensure all pending ACTION_UP events are blocked
        }

        fun processKeyEvent(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean? {
                /* Block these keys! */
                if (EXCLUDED_KEYS.contains(keyCode)) return null

                /* We're not ready yet! */
                if (retroView.frameRendered.value == false) return true

                // BLOCK START when menu is open (RetroMenu3)
                if (keyCode == KeyEvent.KEYCODE_BUTTON_START && shouldHandleStartButton()) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "🛑 START pressed while menu open - CLOSING MENU"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   keyLog BEFORE startButtonCallback: $keyLog"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   comboAlreadyTriggered BEFORE: $comboAlreadyTriggered"
                                )
                                startButtonCallback()
                                // 🔧 BUGFIX: Reset comboAlreadyTriggered when START closes menu
                                // At this point we KNOW the user is CLOSING the menu, not trying to
                                // open it
                                // So it's safe to reset the flag immediately
                                comboAlreadyTriggered = false
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   ✅ comboAlreadyTriggered reset to false (START closed menu)"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   startButtonCallback() completed"
                                )
                        }
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT BUTTON A for confirmation when menu is open
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A && shouldInterceptDpadForMenu()) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "BUTTON_A intercepted for menu confirmation"
                                )
                                menuConfirmCallback()
                        }
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT BUTTON B to go back when menu is open
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && shouldInterceptDpadForMenu()) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "BUTTON_B intercepted for menu back"
                                )
                                menuBackCallback()
                        }
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT DPAD (KeyEvents) for navigation when menu is open
                if (shouldInterceptDpadForMenu() &&
                                (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                                        keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                                        keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
                ) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                when (keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD UP (KeyEvent) intercepted for menu navigation - calling callback"
                                                )
                                                menuNavigateUpCallback()
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD UP (KeyEvent) callback completed"
                                                )
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD DOWN (KeyEvent) intercepted for menu navigation - calling callback"
                                                )
                                                menuNavigateDownCallback()
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD DOWN (KeyEvent) callback completed"
                                                )
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD LEFT (KeyEvent) intercepted"
                                                )
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD RIGHT (KeyEvent) intercepted"
                                                )
                                        }
                                }
                        }
                        return true // Consume the event, don't send to core
                }

                // BLOCK COMPLETELY all controls when RetroMenu3 is open
                // EXCEPT those already handled above (START, A, DPAD)
                if (shouldBlockAllGamepadInput()) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)"
                        )
                        return true // Block completely, don't send to core
                }

                // CRITICAL FIX: Block ACTION_UP for keys that were pressed when menu opened
                // This prevents partial signals (ACTION_UP without ACTION_DOWN) from reaching the
                // core
                if (event.action == KeyEvent.ACTION_UP &&
                                keysToBlockAfterMenuClose.contains(keyCode)
                ) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🚫 BLOCKING ACTION_UP for keyCode=$keyCode (was in keysToBlockAfterMenuClose)"
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "   keysToBlockAfterMenuClose BEFORE: $keysToBlockAfterMenuClose"
                        )
                        keysToBlockAfterMenuClose.remove(keyCode) // Remove after blocking once
                        keyLog.remove(keyCode)
                        android.util.Log.d(
                                "ControllerInput",
                                "   keysToBlockAfterMenuClose AFTER: $keysToBlockAfterMenuClose"
                        )
                        android.util.Log.d("ControllerInput", "   keyLog AFTER: $keyLog")

                        // BUGFIX: Reset combo flag if BOTH combo buttons are released (even when
                        // blocked)
                        // This fixes the issue where user needs to press SELECT+START twice after
                        // closing menu
                        val isComboButton =
                                (keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                                        keyCode == KeyEvent.KEYCODE_BUTTON_SELECT)
                        val startNotPressed = !keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)
                        val selectNotPressed = !keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)
                        val bothReleased = startNotPressed && selectNotPressed

                        android.util.Log.d(
                                "ControllerInput",
                                "   Checking combo reset: isComboButton=$isComboButton, bothReleased=$bothReleased"
                        )

                        if (isComboButton && bothReleased) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   ✅ BOTH combo buttons released! comboAlreadyTriggered: $comboAlreadyTriggered -> false"
                                )
                                comboAlreadyTriggered = false
                        } else {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   ⏳ Waiting for other button (START pressed: ${!startNotPressed}, SELECT pressed: ${!selectNotPressed})"
                                )
                        }

                        return true // Block this ACTION_UP
                }

                val port = getPort(event)

                /* Keep track of user input events */
                when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                                val wasAlreadyPressed = keyLog.contains(keyCode)
                                val keyName =
                                        when (keyCode) {
                                                KeyEvent.KEYCODE_BUTTON_START -> "START"
                                                KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
                                                else -> keyCode.toString()
                                        }
                                keyLog.add(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "⬇️ ACTION_DOWN: $keyName ($keyCode), wasAlreadyPressed: $wasAlreadyPressed, keyLog: $keyLog"
                                )

                                // If the button was already pressed, don't check combo again
                                if (wasAlreadyPressed) {
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "   ⚠️ Ignoring repeated ACTION_DOWN for $keyName"
                                        )
                                        return true // Ignore repeated event
                                }
                        }
                        KeyEvent.ACTION_UP -> {
                                val keyName =
                                        when (keyCode) {
                                                KeyEvent.KEYCODE_BUTTON_START -> "START"
                                                KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
                                                else -> keyCode.toString()
                                        }
                                keyLog.remove(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "⬆️ ACTION_UP: $keyName ($keyCode), keyLog: $keyLog"
                                )

                                // Reset combo flag ONLY when BOTH combo buttons are released
                                if ((keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                                                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) &&
                                                !keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                                                !keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)
                                ) {

                                        if (comboAlreadyTriggered) {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "   ✅ BOTH combo buttons released (normal flow), resetting comboAlreadyTriggered"
                                                )
                                        }
                                        comboAlreadyTriggered = false
                                }
                        }
                }

                checkMenuKeyCombo()

                // BLOCK START and SELECT from reaching core when combo is detected
                // Use containsAll to check if both are present
                if ((keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) &&
                                keyLog.containsAll(KEYCOMBO_MENU) &&
                                keyLog.size == 2
                ) {
                        android.util.Log.d(
                                "ControllerInput",
                                "Blocking START/SELECT from reaching core - combo detected"
                        )
                        return true // Consume the event, don't send to core
                }

                // Normal key, send to core
                retroView.view.sendKeyEvent(event.action, keyCode, port)

                return true
        }

        fun processMotionEvent(event: MotionEvent, retroView: RetroView): Boolean? {
                /* We're not ready yet! */
                if (retroView.frameRendered.value == false) return null

                // COMPLETELY BLOCK all controls when RetroMenu3 is open
                // EXCEPT DPAD (AXIS_HAT) which is used for menu navigation
                if (shouldBlockAllGamepadInput()) {
                        // Allow only DPAD (hat axes) events when menu is open
                        val isDpadEvent =
                                event.getAxisValue(MotionEvent.AXIS_HAT_X) != 0f ||
                                        event.getAxisValue(MotionEvent.AXIS_HAT_Y) != 0f

                        if (!isDpadEvent) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "🛑 BLOCKING GAMEPAD MOTION INPUT - RetroMenu3 is open (non-DPAD)"
                                )
                                return true // Block motion events non-DPAD when menu is open
                        }
                        // If it's DPAD event, let it pass to navigation logic below
                }

                // INTERCEPT DPAD for menu navigation when RetroMenu3 is open
                if (shouldInterceptDpadForMenu()) {
                        android.util.Log.d(
                                "ControllerInput",
                                "[INTERCEPT] 🎮 ========== DPAD INTERCEPTION START =========="
                        )
                        android.util.Log.d(
                                "ControllerInput",
                                "[INTERCEPT] 📊 shouldInterceptDpadForMenu=${shouldInterceptDpadForMenu()}"
                        )

                        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
                        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

                        android.util.Log.d("ControllerInput", "[INTERCEPT] 📊 MotionEvent values:")
                        android.util.Log.d("ControllerInput", "[INTERCEPT]   🎯 hatX=$hatX")
                        android.util.Log.d("ControllerInput", "[INTERCEPT]   🎯 hatY=$hatY")

                        // Simplified logic: detect current DPAD direction
                        when {
                                hatY < -0.5f -> { // DPAD UP
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ⬆️ DPAD UP detected - calling menuNavigateUpCallback"
                                        )
                                        menuNavigateUpCallback()
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ✅ DPAD UP callback completed - returning true"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] 🎮 ========== DPAD INTERCEPTION END (UP) =========="
                                        )
                                        return true
                                }
                                hatY > 0.5f -> { // DPAD DOWN
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ⬇️ DPAD DOWN detected - calling menuNavigateDownCallback"
                                        )
                                        menuNavigateDownCallback()
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ✅ DPAD DOWN callback completed - returning true"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] 🎮 ========== DPAD INTERCEPTION END (DOWN) =========="
                                        )
                                        return true
                                }
                                hatX < -0.5f -> { // DPAD LEFT (if needed in the future)
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ⬅️ DPAD LEFT detected - blocking"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] 🎮 ========== DPAD INTERCEPTION END (LEFT) =========="
                                        )
                                        return true
                                }
                                hatX > 0.5f -> { // DPAD RIGHT (if needed in the future)
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ➡️ DPAD RIGHT detected - blocking"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] 🎮 ========== DPAD INTERCEPTION END (RIGHT) =========="
                                        )
                                        return true
                                }
                                else -> {
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] ⭕ No DPAD direction detected - values too small"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "[INTERCEPT] 🎮 ========== DPAD INTERCEPTION END (NONE) =========="
                                        )
                                        // No direction detected, let the event pass through
                                }
                        }
                } else {
                        android.util.Log.d(
                                "ControllerInput",
                                "[INTERCEPT] 🚫 DPAD interception disabled - shouldInterceptDpadForMenu=${shouldInterceptDpadForMenu()}"
                        )
                }

                // Send motion events to game
                val port = getPort(event)
                retroView.view.apply {
                        sendMotionEvent(
                                GLRetroView.MOTION_SOURCE_DPAD,
                                event.getAxisValue(MotionEvent.AXIS_HAT_X),
                                event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                                port
                        )
                        sendMotionEvent(
                                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                                event.getAxisValue(MotionEvent.AXIS_X),
                                event.getAxisValue(MotionEvent.AXIS_Y),
                                port
                        )
                        sendMotionEvent(
                                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                                event.getAxisValue(MotionEvent.AXIS_Z),
                                event.getAxisValue(MotionEvent.AXIS_RZ),
                                port
                        )
                }

                return true
        }
}
