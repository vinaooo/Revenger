package com.vinaooo.revenger.input

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView
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

                // Fixed threshold values for single-trigger system
                private const val DPAD_THRESHOLD: Float = 0.1f // Physical DPAD - more responsive
                private const val LEFT_ANALOG_THRESHOLD: Float = 0.7f // Left analog - less sensitive

                /** Vendor keycode reported by some physical gamepads for their menu/hamburger button */
                private const val GAMEPAD_MENU_BUTTON_KEYCODE = -6
        }

        // Single trigger system - tracks previous state to detect transitions
        private data class DirectionalState(
                var up: Boolean = false,
                var down: Boolean = false,
                var left: Boolean = false,
                var right: Boolean = false
        )

        // Track state for each input type to implement single-trigger navigation
        private val dpadState = DirectionalState()
        private val leftAnalogState = DirectionalState()

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

        /**
         * Check for single-trigger directional input
         * Returns the keycode if there's a NEW press (transition from false to true)
         * Returns null if no new input or input is being held
         */
        private fun checkSingleTrigger(
                currentUp: Boolean,
                currentDown: Boolean,
                currentLeft: Boolean = false,
                currentRight: Boolean = false,
                previousState: DirectionalState
        ): Int? {
                var triggeredKeyCode: Int? = null

                // Check transitions (false -> true)
                if (currentUp && !previousState.up) {
                        triggeredKeyCode = KeyEvent.KEYCODE_DPAD_UP
                } else if (currentDown && !previousState.down) {
                        triggeredKeyCode = KeyEvent.KEYCODE_DPAD_DOWN
                } else if (currentLeft && !previousState.left) {
                        triggeredKeyCode = KeyEvent.KEYCODE_DPAD_LEFT
                } else if (currentRight && !previousState.right) {
                        triggeredKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT
                }

                // Update previous state
                previousState.up = currentUp
                previousState.down = currentDown
                previousState.left = currentLeft
                previousState.right = currentRight

                return triggeredKeyCode
        }

        /** Controller numbers are [1, inf), we need [0, inf) */
        private fun getPort(event: InputEvent): Int =
                ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        fun processGamePadButtonEvent(keyCode: Int, action: Int): Boolean {
                val keyName =
                        when (keyCode) {
                                KeyEvent.KEYCODE_BUTTON_A -> "A"
                                KeyEvent.KEYCODE_BUTTON_B -> "B"
                                KeyEvent.KEYCODE_BUTTON_START -> "START"
                                KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
                                else -> keyCode.toString()
                        }
                val actionName = if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"

                // INTERCEPT BUTTON A for confirmation when menu is open
                // During grace period, DO NOT block A (only open menu blocks)
                if (callbackDebouncer.interceptButtonAConfirm(keyCode, action)) {
                        android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        return true // Event intercepted - don't send to core
                }

                // INTERCEPT BUTTON B to go back when menu is open
                val shouldInterceptB =
                        keyCode == KeyEvent.KEYCODE_BUTTON_B &&
                                callbackDebouncer.shouldInterceptSpecificButton(keyCode)
                if (shouldInterceptB) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🔵 BUTTON_B intercepted - action=" +
                                        "${if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"}"
                        )

                        if (action == KeyEvent.ACTION_DOWN) {
                                // Check if already added to keyLog (hold/repeat)
                                val alreadyPressed = comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_B)
                                if (alreadyPressed) {
                                        android.util.Log.d("ControllerInput", "   ↪️ B already in keyLog (hold) - consuming DOWN without callback")
                                        return true
                                }

                                // Primeira vez pressionando: adicionar ao keyLog e executar
                                // callback
                                comboTracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_B)
                                android.util.Log.d("ControllerInput", "   → First B DOWN - executing menuBackCallback")
                                callbackDebouncer.executeMenuCallback(menuBackCallback, callbackDebouncer.lastMenuBackCallbackTime) {
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

                // INTERCEPT START button when menu is open (to close menu)
                if (keyCode == KeyEvent.KEYCODE_BUTTON_START && shouldHandleStartButton()) {
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d("ControllerInput", "START (GamePad) pressed while menu open - CLOSING MENU")
                                android.util.Log.d("ControllerInput", "   keyLog BEFORE startButtonCallback: ${comboTracker.keyLog}")
                                android.util.Log.d("ControllerInput", "   comboAlreadyTriggered BEFORE: ${comboTracker.getComboAlreadyTriggered()}")
                                callbackDebouncer.executeMenuCallback(
                                        startButtonCallback,
                                        callbackDebouncer.lastStartButtonCallbackTime
                                ) { callbackDebouncer.lastStartButtonCallbackTime = it }
                                android.util.Log.d("ControllerInput", "   startButtonCallback() completed")
                        }
                        return true // Event intercepted - don't send to core
                }

                // INTERCEPT GAMEPAD MENU BUTTON (☰)
                if (keyCode == GAMEPAD_MENU_BUTTON_KEYCODE && shouldHandleGamepadMenuButton()) {
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d("ControllerInput", "GAMEPAD MENU BUTTON (☰) pressed - toggling menu")
                                callbackDebouncer.executeMenuCallback(
                                        gamepadMenuButtonCallback,
                                        callbackDebouncer.lastGamepadMenuButtonCallbackTime
                                ) { callbackDebouncer.lastGamepadMenuButtonCallbackTime = it }
                        }
                        return true // Event intercepted - don't send to core
                }

                // BLOCK COMPLETELY all controls when RetroMenu3 is open
                // EXCEPT those already handled above (A, B, START, gamepad menu button)
                val shouldBlockGamepadButton = shouldBlockAllGamepadInput()
                android.util.Log.d(
                        "ControllerInput",
                        "🎮 processGamePadButtonEvent: shouldBlockAllGamepadInput() = " +
                                "$shouldBlockGamepadButton (keyCode: $keyCode, action: $actionName)"
                )
                if (shouldBlockGamepadButton) {
                        android.util.Log.d("ControllerInput", "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)")
                        return true // Block completely, don't send to core
                }

                /* Keep track of user input events */
                if (comboTracker.trackKeyLogAndComboReset(keyCode, action)) {
                        android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        return true // Event intercepted (repeated press)
                }

                comboTracker.checkMenuKeyCombo()

                android.util.Log.d("ControllerInput", "🔍 After checkMenuKeyCombo - checking final decision...")

                // 🔧 BUGFIX: Block SELECT and START events when they're part of the combo
                // This prevents START from leaking to the core and pausing the game
                if ((keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT)
                ) {
                        // If both buttons are pressed (combo active), intercept the events
                        if (comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                                        comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)
                        ) {
                                android.util.Log.d("ControllerInput", "� Blocking $keyName (part of SELECT+START combo) - preventing leak to core")
                                android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                return true // Event intercepted - don't send to core
                        }
                }

                android.util.Log.d("ControllerInput", "🟢 Event $keyName $actionName → SENDING TO CORE (not intercepted)")
                android.util.Log.d("ControllerInput", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return false // Event not intercepted - send to core
        }
        fun processKeyEvent(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean? {
                // DEBUG: Log ALL keyCodes to detect button mappings
                if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🎮 processKeyEvent: keyCode=$keyCode action=" +
                                        "${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} " +
                                        "(BUTTON_A=96, BUTTON_B=97)"
                        )
                }

                /* Block these keys! */
                if (EXCLUDED_KEYS.contains(keyCode)) return null

                /* We're not ready yet! */
                if (retroView.frameRendered.value == false) return true

                // BLOCK START when menu is open (RetroMenu3)
                if (keyCode == KeyEvent.KEYCODE_BUTTON_START && shouldHandleStartButton()) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d("ControllerInput", "🛑 START pressed while menu open - CLOSING MENU")
                                android.util.Log.d("ControllerInput", "   keyLog BEFORE startButtonCallback: ${comboTracker.keyLog}")
                                android.util.Log.d("ControllerInput", "   comboAlreadyTriggered BEFORE: ${comboTracker.getComboAlreadyTriggered()}")
                                callbackDebouncer.executeMenuCallback(
                                        startButtonCallback,
                                        callbackDebouncer.lastStartButtonCallbackTime
                                ) { callbackDebouncer.lastStartButtonCallbackTime = it }
                                // 🔧 BUGFIX: Reset comboAlreadyTriggered when START closes menu
                                // At this point we KNOW the user is CLOSING the menu, not trying to
                                // open it
                                // So it's safe to reset the flag immediately
                                comboTracker.resetComboAlreadyTriggered()
                                android.util.Log.d("ControllerInput", "   ✅ comboAlreadyTriggered reset to false (START closed menu)")
                                android.util.Log.d("ControllerInput", "   startButtonCallback() completed")
                        }
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT BUTTON A for confirmation when menu is open
                // During grace period, DO NOT block A (only open menu blocks)
                if (callbackDebouncer.interceptButtonAConfirm(keyCode, event.action)) {
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT BUTTON B to go back when menu is open
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && callbackDebouncer.shouldInterceptSpecificButton(keyCode)
                ) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🔵 BUTTON_B intercepted in processKeyEvent - " +
                                        "action=${event.action} (DOWN=0, UP=1), shouldIntercept=true"
                        )
                        val alreadyPressed = comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_B)
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                if (alreadyPressed) {
                                        android.util.Log.d("ControllerInput", "   ↪️ B already pressed (hold) - consuming without callback")
                                        return true
                                }
                                // Track hold so submenu→main transitions keep state
                                comboTracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_B)
                                android.util.Log.d("ControllerInput", "BUTTON_B intercepted for menu back")
                                callbackDebouncer.executeMenuCallback(menuBackCallback, callbackDebouncer.lastMenuBackCallbackTime) {
                                        callbackDebouncer.lastMenuBackCallbackTime = it
                                }
                        } else {
                                android.util.Log.d("ControllerInput", "🚫 BUTTON_B ACTION_UP intercepted - blocking from reaching game")
                                comboTracker.keyLog.remove(KeyEvent.KEYCODE_BUTTON_B)
                        }
                        // CRITICAL: Bloquear TANTO ACTION_DOWN quanto ACTION_UP
                        // Isso impede que o ACTION_UP vaze para o jogo
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT DPAD (KeyEvents) for navigation when menu is open
                val isDpadNavigationKey =
                        keyCode in
                                setOf(
                                        KeyEvent.KEYCODE_DPAD_UP,
                                        KeyEvent.KEYCODE_DPAD_DOWN,
                                        KeyEvent.KEYCODE_DPAD_LEFT,
                                        KeyEvent.KEYCODE_DPAD_RIGHT
                                )

                if (shouldInterceptDpadForMenu() && isDpadNavigationKey) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                when (keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD UP (KeyEvent) intercepted for menu " +
                                                                "navigation - calling callback"
                                                )
                                                callbackDebouncer.executeMenuCallback(
                                                        menuNavigateUpCallback,
                                                        callbackDebouncer.lastMenuNavigateUpCallbackTime
                                                ) { callbackDebouncer.lastMenuNavigateUpCallbackTime = it }
                                                android.util.Log.d("ControllerInput", "DPAD UP (KeyEvent) callback completed")
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD DOWN (KeyEvent) intercepted for menu " +
                                                                "navigation - calling callback"
                                                )
                                                callbackDebouncer.executeMenuCallback(
                                                        menuNavigateDownCallback,
                                                        callbackDebouncer.lastMenuNavigateDownCallbackTime
                                                ) { callbackDebouncer.lastMenuNavigateDownCallbackTime = it }
                                                android.util.Log.d("ControllerInput", "DPAD DOWN (KeyEvent) callback completed")
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD LEFT (KeyEvent) intercepted for menu " +
                                                                "navigation - calling callback"
                                                )
                                                callbackDebouncer.executeMenuCallback(
                                                        menuNavigateLeftCallback,
                                                        callbackDebouncer.lastMenuNavigateLeftCallbackTime
                                                ) { callbackDebouncer.lastMenuNavigateLeftCallbackTime = it }
                                                android.util.Log.d("ControllerInput", "DPAD LEFT (KeyEvent) callback completed")
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                android.util.Log.d(
                                                        "ControllerInput",
                                                        "DPAD RIGHT (KeyEvent) intercepted for menu " +
                                                                "navigation - calling callback"
                                                )
                                                callbackDebouncer.executeMenuCallback(
                                                        menuNavigateRightCallback,
                                                        callbackDebouncer.lastMenuNavigateRightCallbackTime
                                                ) { callbackDebouncer.lastMenuNavigateRightCallbackTime = it }
                                                android.util.Log.d("ControllerInput", "DPAD RIGHT (KeyEvent) callback completed")
                                        }
                                }
                        }
                        return true // Consume the event, don't send to core
                }

                // BLOCK COMPLETELY all controls when RetroMenu3 is open
                // EXCEPT those already handled above (START, A, DPAD)
                val shouldBlock = shouldBlockAllGamepadInput()
                android.util.Log.d(
                        "ControllerInput",
                        "🎮 processKeyEvent: shouldBlockAllGamepadInput() = $shouldBlock " +
                                "(keyCode: $keyCode, action: ${event.action})"
                )
                if (shouldBlock) {
                        android.util.Log.d("ControllerInput", "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)")
                        return true // Block completely, don't send to core
                }

                val port = getPort(event)

                /* Keep track of user input events */
                if (comboTracker.trackKeyLogAndComboReset(keyCode, event.action)) {
                        return true // Ignore repeated event
                }

                comboTracker.checkMenuKeyCombo()

                // BLOCK START and SELECT from reaching core when combo is detected
                // Use containsAll to check if both are present
                val isComboButtonKey = keyCode in KEYCOMBO_MENU
                val comboFullyHeld = comboTracker.keyLog.containsAll(KEYCOMBO_MENU) && comboTracker.keyLog.size == 2

                if (isComboButtonKey && comboFullyHeld) {
                        android.util.Log.d("ControllerInput", "Blocking START/SELECT from reaching core - combo detected")
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
                // (Handled at the end of this function to ensure all events update single-trigger state first)
                val shouldBlock = shouldBlockAllGamepadInput()
                android.util.Log.d("ControllerInput", "🎮 processMotionEvent: shouldBlockAllGamepadInput() = $shouldBlock")

                // INTERCEPT DPAD and Left Analog for menu navigation when RetroMenu3 is open
                if (shouldInterceptDpadForMenu()) {
                        android.util.Log.d("ControllerInput", "[INTERCEPT] 🎮 ========== DPAD/ANALOG INTERCEPTION START ==========")
                        android.util.Log.d("ControllerInput", "[INTERCEPT] 📊 shouldInterceptDpadForMenu=${shouldInterceptDpadForMenu()}")

                        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
                        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                        val axisX = event.getAxisValue(MotionEvent.AXIS_X)
                        val axisY = event.getAxisValue(MotionEvent.AXIS_Y)

                        android.util.Log.d("ControllerInput", "[INTERCEPT] 📊 MotionEvent values:")
                        android.util.Log.d("ControllerInput", "[INTERCEPT]   🎯 hatX=$hatX, hatY=$hatY")
                        android.util.Log.d("ControllerInput", "[INTERCEPT]   🎯 axisX=$axisX, axisY=$axisY")

                        // Check D-PAD transitions
                        val dpadTrigger = checkSingleTrigger(
                                currentUp = hatY < -DPAD_THRESHOLD,
                                currentDown = hatY > DPAD_THRESHOLD,
                                currentLeft = hatX < -DPAD_THRESHOLD,
                                currentRight = hatX > DPAD_THRESHOLD,
                                previousState = dpadState
                        )

                        // Check Left Analog transitions
                        val analogTrigger = checkSingleTrigger(
                                currentUp = axisY < -LEFT_ANALOG_THRESHOLD,
                                currentDown = axisY > LEFT_ANALOG_THRESHOLD,
                                currentLeft = axisX < -LEFT_ANALOG_THRESHOLD,
                                currentRight = axisX > LEFT_ANALOG_THRESHOLD,
                                previousState = leftAnalogState
                        )

                        // Use DPAD trigger if available, otherwise Analog
                        val trigger = dpadTrigger ?: analogTrigger

                        when (trigger) {
                                KeyEvent.KEYCODE_DPAD_UP -> { // UP
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ⬆️ UP detected - calling menuNavigateUpCallback")
                                        callbackDebouncer.executeMenuCallback(
                                                menuNavigateUpCallback,
                                                callbackDebouncer.lastMenuNavigateUpCallbackTime
                                        ) { callbackDebouncer.lastMenuNavigateUpCallbackTime = it }
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ✅ UP callback completed - returning true")
                                        return true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> { // DOWN
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ⬇️ DOWN detected - calling menuNavigateDownCallback")
                                        callbackDebouncer.executeMenuCallback(
                                                menuNavigateDownCallback,
                                                callbackDebouncer.lastMenuNavigateDownCallbackTime
                                        ) { callbackDebouncer.lastMenuNavigateDownCallbackTime = it }
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ✅ DOWN callback completed - returning true")
                                        return true
                                }
                                KeyEvent.KEYCODE_DPAD_LEFT -> { // LEFT
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ⬅️ LEFT detected - calling menuNavigateLeftCallback")
                                        callbackDebouncer.executeMenuCallback(
                                                menuNavigateLeftCallback,
                                                callbackDebouncer.lastMenuNavigateLeftCallbackTime
                                        ) { callbackDebouncer.lastMenuNavigateLeftCallbackTime = it }
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ✅ LEFT callback completed - returning true")
                                        return true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> { // RIGHT
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ➡️ RIGHT detected - calling menuNavigateRightCallback")
                                        callbackDebouncer.executeMenuCallback(
                                                menuNavigateRightCallback,
                                                callbackDebouncer.lastMenuNavigateRightCallbackTime
                                        ) { callbackDebouncer.lastMenuNavigateRightCallbackTime = it }
                                        android.util.Log.d("ControllerInput", "[INTERCEPT] ✅ RIGHT callback completed - returning true")
                                        return true
                                }
                                else -> {
                                        // Return true if any supported axis is out of deadzone but
                                        // not triggering a new event
                                        val anySupportedAxisOutOfDeadzone =
                                                Math.abs(hatX) > DPAD_THRESHOLD ||
                                                        Math.abs(hatY) > DPAD_THRESHOLD ||
                                                        Math.abs(axisX) > LEFT_ANALOG_THRESHOLD ||
                                                        Math.abs(axisY) > LEFT_ANALOG_THRESHOLD

                                        if (anySupportedAxisOutOfDeadzone) {
                                                return true
                                        }
                                }
                        }
                } else {
                        android.util.Log.d(
                                "ControllerInput",
                                "[INTERCEPT] 🚫 DPAD interception disabled - " +
                                        "shouldInterceptDpadForMenu=${shouldInterceptDpadForMenu()}"
                        )
                }

                // Send motion events to game ONLY if not blocked
                if (!shouldBlock) {
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
                }

                return true
        }
}
