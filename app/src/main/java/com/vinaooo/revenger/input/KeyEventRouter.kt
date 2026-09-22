package com.vinaooo.revenger.input

import android.view.InputEvent
import android.view.KeyEvent
import com.vinaooo.revenger.retroview.RetroView

/**
 * Owns [ControllerInput.processKeyEvent]'s interception chain (START-closes-menu, BUTTON_A
 * confirm, BUTTON_B back, DPAD menu navigation, the RetroMenu3 block-all gate, and the
 * SELECT+START combo leak-block) plus the pass-through `sendKeyEvent` call to the core,
 * extracted to keep that function within detekt's `LongMethod`/`CyclomaticComplexMethod`/
 * `ReturnCount` thresholds. [ControllerInput] keeps a public `processKeyEvent` delegating to
 * [process], so no external call site changes.
 *
 * This is a deliberately SEPARATE class from [GamePadButtonRouter] -- the two paths have small,
 * intentional behavioral differences (see the `DIVERGENCE` tests in `ControllerInput_test`)
 * that must never be unified into shared logic. In particular:
 * - Only this class's START interceptor resets [ComboKeyLogTracker.resetComboAlreadyTriggered]
 *   immediately (the GamePad path relies on the generic keyLog bookkeeping instead).
 * - Only this class's combo leak-block additionally requires `keyLog.size == 2`.
 * - The BUTTON_B handler's non-DOWN branch is a bare `else` here, vs `else if (ACTION_UP)` on
 *   the GamePad path.
 * - Only this path has DPAD-as-KeyEvent menu navigation and the frame-not-ready gate.
 */
class KeyEventRouter(
        private val callbacksProvider: () -> ControllerInputCallbacks,
        private val comboTrackerProvider: () -> ComboKeyLogTracker,
        private val callbackDebouncerProvider: () -> MenuCallbackDebouncer
) {

        private val callbacks get() = callbacksProvider()
        private val comboTracker get() = comboTrackerProvider()
        private val callbackDebouncer get() = callbackDebouncerProvider()

        /** Controller numbers are [1, inf), we need [0, inf) */
        private fun getPort(event: InputEvent): Int =
                ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        /** BLOCK START when menu is open (RetroMenu3), closing it. */
        private fun interceptStartButton(keyCode: Int, event: KeyEvent): Boolean {
                if (keyCode != KeyEvent.KEYCODE_BUTTON_START || !callbacks.shouldHandleStartButton()) {
                        return false
                }

                if (event.action == KeyEvent.ACTION_DOWN) {
                        android.util.Log.d("ControllerInput", "🛑 START pressed while menu open - CLOSING MENU")
                        android.util.Log.d("ControllerInput", "   keyLog BEFORE startButtonCallback: ${comboTracker.keyLog}")
                        android.util.Log.d("ControllerInput", "   comboAlreadyTriggered BEFORE: ${comboTracker.getComboAlreadyTriggered()}")
                        callbackDebouncer.executeMenuCallback(
                                callbacks.startButtonCallback,
                                callbackDebouncer.lastStartButtonCallbackTime
                        ) { callbackDebouncer.lastStartButtonCallbackTime = it }
                        // 🔧 BUGFIX: Reset comboAlreadyTriggered when START closes menu
                        // At this point we KNOW the user is CLOSING the menu, not trying to
                        // open it, so it's safe to reset the flag immediately
                        comboTracker.resetComboAlreadyTriggered()
                        android.util.Log.d("ControllerInput", "   ✅ comboAlreadyTriggered reset to false (START closed menu)")
                        android.util.Log.d("ControllerInput", "   startButtonCallback() completed")
                }
                return true // Consume the event, don't send to core
        }

        /** INTERCEPT BUTTON B to go back when menu is open. */
        private fun interceptButtonB(keyCode: Int, event: KeyEvent): Boolean {
                if (keyCode != KeyEvent.KEYCODE_BUTTON_B ||
                                !callbackDebouncer.shouldInterceptSpecificButton(keyCode)
                ) {
                        return false
                }

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
                        callbackDebouncer.executeMenuCallback(callbacks.menuBackCallback, callbackDebouncer.lastMenuBackCallbackTime) {
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

        /** Fires the debounced menu-navigate callback matching [keyCode]'s DPAD direction. */
        private fun fireDpadCallback(keyCode: Int) {
                when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP ->
                                callbackDebouncer.executeMenuCallback(
                                        callbacks.menuNavigateUpCallback,
                                        callbackDebouncer.lastMenuNavigateUpCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateUpCallbackTime = it }
                        KeyEvent.KEYCODE_DPAD_DOWN ->
                                callbackDebouncer.executeMenuCallback(
                                        callbacks.menuNavigateDownCallback,
                                        callbackDebouncer.lastMenuNavigateDownCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateDownCallbackTime = it }
                        KeyEvent.KEYCODE_DPAD_LEFT ->
                                callbackDebouncer.executeMenuCallback(
                                        callbacks.menuNavigateLeftCallback,
                                        callbackDebouncer.lastMenuNavigateLeftCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateLeftCallbackTime = it }
                        KeyEvent.KEYCODE_DPAD_RIGHT ->
                                callbackDebouncer.executeMenuCallback(
                                        callbacks.menuNavigateRightCallback,
                                        callbackDebouncer.lastMenuNavigateRightCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateRightCallbackTime = it }
                }
        }

        /** INTERCEPT DPAD (KeyEvents) for navigation when menu is open. */
        private fun interceptDpadNavigation(keyCode: Int, event: KeyEvent): Boolean {
                val isDpadNavigationKey =
                        keyCode in
                                setOf(
                                        KeyEvent.KEYCODE_DPAD_UP,
                                        KeyEvent.KEYCODE_DPAD_DOWN,
                                        KeyEvent.KEYCODE_DPAD_LEFT,
                                        KeyEvent.KEYCODE_DPAD_RIGHT
                                )
                if (!callbacks.shouldInterceptDpadForMenu() || !isDpadNavigationKey) return false

                if (event.action == KeyEvent.ACTION_DOWN) {
                        fireDpadCallback(keyCode)
                }
                return true // Consume the event, don't send to core
        }

        /**
         * BLOCK COMPLETELY all controls when RetroMenu3 is open, except those already handled
         * above (START, A, B, DPAD).
         */
        private fun blockAllGamepadInput(keyCode: Int, event: KeyEvent): Boolean {
                val shouldBlock = callbacks.shouldBlockAllGamepadInput()
                android.util.Log.d(
                        "ControllerInput",
                        "🎮 processKeyEvent: shouldBlockAllGamepadInput() = $shouldBlock " +
                                "(keyCode: $keyCode, action: ${event.action})"
                )
                if (!shouldBlock) return false

                android.util.Log.d("ControllerInput", "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)")
                return true // Block completely, don't send to core
        }

        /**
         * Tracks the key for combo detection and blocks START/SELECT from reaching the core
         * while the combo is fully held. Unlike [GamePadButtonRouter]'s equivalent, this
         * ADDITIONALLY requires `keyLog.size == 2` -- holding a third button lets the event
         * leak to the core here (see `DIVERGENCE` test).
         */
        private fun trackAndCheckComboLeak(keyCode: Int, event: KeyEvent): Boolean {
                if (comboTracker.trackKeyLogAndComboReset(keyCode, event.action)) return true // Ignore repeated event

                comboTracker.checkMenuKeyCombo()

                val isComboButtonKey = keyCode in ControllerInput.KEYCOMBO_MENU
                val comboFullyHeld =
                        comboTracker.keyLog.containsAll(ControllerInput.KEYCOMBO_MENU) &&
                                comboTracker.keyLog.size == 2

                if (!isComboButtonKey || !comboFullyHeld) return false

                android.util.Log.d("ControllerInput", "Blocking START/SELECT from reaching core - combo detected")
                return true // Consume the event, don't send to core
        }

        fun process(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean? {
                // DEBUG: Log ALL keyCodes to detect button mappings
                if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🎮 processKeyEvent: keyCode=$keyCode action=" +
                                        "${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} " +
                                        "(BUTTON_A=96, BUTTON_B=97)"
                        )
                }

                return when {
                        /* Block these keys! */
                        ControllerInput.EXCLUDED_KEYS.contains(keyCode) -> null
                        /* We're not ready yet! */
                        retroView.frameRendered.value == false -> true
                        else -> {
                                val intercepted =
                                        interceptStartButton(keyCode, event) ||
                                                callbackDebouncer.interceptButtonAConfirm(keyCode, event.action) ||
                                                interceptButtonB(keyCode, event) ||
                                                interceptDpadNavigation(keyCode, event) ||
                                                blockAllGamepadInput(keyCode, event) ||
                                                trackAndCheckComboLeak(keyCode, event)

                                if (!intercepted) {
                                        // Normal key, send to core
                                        retroView.view.sendKeyEvent(event.action, keyCode, getPort(event))
                                }
                                true
                        }
                }
        }
}
