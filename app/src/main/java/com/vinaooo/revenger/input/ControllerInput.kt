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
                keyLog.clear()

                // 🔧 FIX: Always reset comboAlreadyTriggered when clearing key log
                // This ensures combo detection works properly after menu dismissal
                // regardless of timing or menu state during the clear operation
                comboAlreadyTriggered = false

                lastComboTriggerTime = 0L // Reset cooldown timer to allow immediate combo detection
                menuCloseDebounceTime = System.currentTimeMillis() // Set debounce timestamp
        }

        /**
         * Limpa apenas os botões de ação do menu (A, B, D-PAD) do keyLog NÃO limpa START/SELECT
         * para não causar problemas no combo Deve ser chamado quando o menu fecha para evitar
         * "wasAlreadyPressed" false positives
         */
        fun clearMenuActionButtons() {
                // Remove apenas botões de menu (A, B, D-PAD), mantém START/SELECT
                keyLog.removeIf { keyCode ->
                        keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                                keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                }
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

        /** Function to check if gamepad menu button should trigger menu */
        var shouldHandleGamepadMenuButton: () -> Boolean = { false }

        /** The callback for when the user presses the gamepad menu button */
        var gamepadMenuButtonCallback: () -> Unit = {}

        /** Function to check if devemos bloquear TODOS os inputs do gamepad */
        var shouldBlockAllGamepadInput: () -> Boolean = { false }

        /** Function to check if RetroMenu3 is currently open */
        var isRetroMenu3Open: () -> Boolean = { false }

        /** Callbacks for RetroMenu3 navigation */
        var menuNavigateUpCallback: () -> Unit = {}
        var menuNavigateDownCallback: () -> Unit = {}
        var menuConfirmCallback: () -> Unit = {}
        var menuBackCallback: () -> Unit = {}

        // Debouncing timestamps for menu callbacks to prevent rapid successive calls
        private var lastMenuBackCallbackTime: Long = 0
        private var lastMenuConfirmCallbackTime: Long = 0
        private var lastMenuNavigateUpCallbackTime: Long = 0
        private var lastMenuNavigateDownCallbackTime: Long = 0
        private var lastStartButtonCallbackTime: Long = 0
        private var lastGamepadMenuButtonCallbackTime: Long = 0

        // Minimum time between menu callback calls (in milliseconds)
        private val MENU_CALLBACK_DEBOUNCE_MS = 150L

        /** Helper function to execute menu callbacks with debouncing protection */
        private fun executeMenuCallback(
                callback: () -> Unit,
                lastExecutionTime: Long,
                updateTime: (Long) -> Unit
        ): Boolean {
                // First check if it's safe to execute menu operations
                if (!isMenuOperationSafe()) {
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

        /** Getter for comboAlreadyTriggered (for debugging) */
        fun getComboAlreadyTriggered(): Boolean = comboAlreadyTriggered

        /** Reset comboAlreadyTriggered flag (used when menu is being dismissed) */
        fun resetComboAlreadyTriggered() {
                comboAlreadyTriggered = false
        }

        fun updateMenuCloseDebounceTime() {
                menuCloseDebounceTime = System.currentTimeMillis()
        }

        /** Function to check if devemos interceptar DPAD para menu */
        var shouldInterceptDpadForMenu: () -> Boolean = { false }

        /**
         * Flag para manter interceptação ativa por um período após menu fechar Isso previne que
         * ACTION_UP vaze para o jogo
         */
        private var keepInterceptingUntil: Long = 0L

        /** Which button closed the menu (to block only that button during grace period) */
        private var buttonThatClosedMenu: Int? = null

        /** Ativa interceptação temporária por X ms após menu fechar */
        fun keepInterceptingButtons(durationMs: Long = 500, closingButton: Int? = null) {
                keepInterceptingUntil = System.currentTimeMillis() + durationMs
                buttonThatClosedMenu = closingButton
        }

        /** Verifica se devemos continuar interceptando (menu aberto OU período de graça ativo) */
        private fun shouldInterceptButtons(): Boolean {
                val menuActive = shouldInterceptDpadForMenu()
                val gracePeriodActive = System.currentTimeMillis() < keepInterceptingUntil
                val result = menuActive || gracePeriodActive

                return result
        }

        /**
         * Verifica se um botão ESPECÍFICO deve ser bloqueado durante grace period Bloqueia apenas o
         * botão que fechou o menu durante o grace period NÃO bloqueia outros botões mesmo se menu
         * estiver "tecnicamente ativo"
         */
        private fun shouldInterceptSpecificButton(keyCode: Int): Boolean {
                val keyName =
                        when (keyCode) {
                                KeyEvent.KEYCODE_BUTTON_A -> "A"
                                KeyEvent.KEYCODE_BUTTON_B -> "B"
                                else -> keyCode.toString()
                        }

                // Durante grace period, bloquear APENAS o botão que fechou o menu
                val now = System.currentTimeMillis()
                val gracePeriodActive = now < keepInterceptingUntil
                val timeRemaining = if (gracePeriodActive) keepInterceptingUntil - now else 0

                if (gracePeriodActive && keyCode == buttonThatClosedMenu) {
                        return true
                }

                // Se menu está aberto de verdade, verificar através de shouldInterceptDpadForMenu
                val menuActive = shouldInterceptDpadForMenu()

                if (menuActive) {
                        return true
                }

                // Botão pode passar
                return false
        }

        /**
         * Function to check if it's safe to execute menu callbacks (no critical operations in
         * progress)
         */
        var isMenuOperationSafe: () -> Boolean = { true }

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
                // Check if we have exactly the two buttons pressed
                val hasSelectAndStart = keyLog.containsAll(KEYCOMBO_MENU) && keyLog.size == 2

                // Check cooldown to avoid very fast detections
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTrigger = currentTime - lastComboTriggerTime

                // ENABLED FOR DEBUG: Combo detection logs
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

                        // ENABLED FOR DEBUG: Combo detection success logs
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
                        // ENABLED FOR DEBUG: Combo rejection logs
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

                        if (!hasSelectAndStart) {}
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
                                if (shouldHandleSelectStartCombo()) {
                                        // Menu is CLOSED but flag is still true - this may happen
                                        // due to timing
                                        // The flag will be reset by onMenuClosedCallback, but this
                                        // check happens first
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "│    ℹ️  Menu closed, comboAlreadyTriggered=true (will be reset by callback)"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "│       lastComboTriggerTime: $lastComboTriggerTime (${timeSinceLastTrigger}ms ago)"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "│       This is expected timing - callback will reset flag"
                                        )
                                } else {
                                        // Menu is OPEN - this is expected, not a bug
                                }
                        }
                        if (!shouldHandleSelectStartCombo()) {}
                        if (timeSinceLastTrigger <= COMBO_COOLDOWN_MS) {}
                        if (timeSinceMenuClose <= MENU_CLOSE_DEBOUNCE_MS) {}
                }
        }

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
                val timestamp =
                        android.text.format.DateFormat.format(
                                "HH:mm:ss.SSS",
                                System.currentTimeMillis()
                        )

                // INTERCEPT BUTTON A for confirmation when menu is open
                // Durante grace period, NÃO bloquear A (só menu aberto bloqueia)
                val menuActiveForA = shouldInterceptDpadForMenu()
                val shouldInterceptA = keyCode == KeyEvent.KEYCODE_BUTTON_A && menuActiveForA
                if (shouldInterceptA) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🔴 BUTTON_A intercepted - shouldInterceptDpadForMenu()=true"
                        )
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   → Executing menuConfirmCallback"
                                )
                                // NOTE: NÃO bloqueamos A aqui porque ele nem sempre fecha o menu
                                // O bloqueio será feito no callback onMenuClosedCallback
                                executeMenuCallback(
                                        menuConfirmCallback,
                                        lastMenuConfirmCallbackTime
                                ) { lastMenuConfirmCallbackTime = it }
                        }
                        android.util.Log.d(
                                "ControllerInput",
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                        )
                        // CRITICAL: Consumir TANTO ACTION_DOWN quanto ACTION_UP
                        return true // Event intercepted - don't send to core
                }

                // INTERCEPT BUTTON B to go back when menu is open
                val shouldInterceptB =
                        keyCode == KeyEvent.KEYCODE_BUTTON_B &&
                                shouldInterceptSpecificButton(keyCode)
                if (shouldInterceptB) {
                        android.util.Log.d(
                                "ControllerInput",
                                "� BUTTON_B intercepted - shouldInterceptSpecificButton()=true"
                        )
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   → Executing menuBackCallback"
                                )
                                // NOTE: Bloqueio será feito no callback onMenuClosedCallback
                                // quando o menu realmente fechar
                                executeMenuCallback(menuBackCallback, lastMenuBackCallbackTime) {
                                        lastMenuBackCallbackTime = it
                                }
                        } else {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "🚫 BUTTON_B ACTION_UP in processGamePadButtonEvent - blocking"
                                )
                        }
                        // CRITICAL: Consumir TANTO ACTION_DOWN quanto ACTION_UP
                        return true // Event intercepted - don't send to core
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
                                executeMenuCallback(
                                        startButtonCallback,
                                        lastStartButtonCallbackTime
                                ) { lastStartButtonCallbackTime = it }
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   startButtonCallback() completed"
                                )
                        }
                        return true // Event intercepted - don't send to core
                }

                // INTERCEPT GAMEPAD MENU BUTTON (☰)
                if (keyCode == -6 && shouldHandleGamepadMenuButton()) {
                        if (action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "GAMEPAD MENU BUTTON (☰) pressed - toggling menu"
                                )
                                executeMenuCallback(
                                        gamepadMenuButtonCallback,
                                        lastGamepadMenuButtonCallbackTime
                                ) { lastGamepadMenuButtonCallbackTime = it }
                        }
                        return true // Event intercepted - don't send to core
                }

                /* Keep track of user input events */
                when (action) {
                        KeyEvent.ACTION_DOWN -> {
                                val wasAlreadyPressed = keyLog.contains(keyCode)
                                keyLog.add(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "⬇️  ACTION_DOWN: keyCode=$keyCode ($keyName)"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "   wasAlreadyPressed: $wasAlreadyPressed"
                                )
                                android.util.Log.d("ControllerInput", "   keyLog BEFORE: $keyLog")

                                // If the button was already pressed, don't check combo again
                                if (wasAlreadyPressed) {
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "   🔴 BLOCKED - Button was already pressed (repeat)"
                                        )
                                        android.util.Log.d(
                                                "ControllerInput",
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                                        )
                                        return true // Event intercepted (repeated press)
                                }
                                android.util.Log.d("ControllerInput", "   ✅ Button added to keyLog")
                        }
                        KeyEvent.ACTION_UP -> {
                                keyLog.remove(keyCode)
                                android.util.Log.d(
                                        "ControllerInput",
                                        "⬆️  ACTION_UP: keyCode=$keyCode ($keyName), keyLog AFTER: $keyLog"
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

                android.util.Log.d(
                        "ControllerInput",
                        "🔍 After checkMenuKeyCombo - checking final decision..."
                )

                // 🔧 BUGFIX: Block SELECT and START events when they're part of the combo
                // This prevents START from leaking to the core and pausing the game
                if ((keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT)
                ) {
                        // If both buttons are pressed (combo active), intercept the events
                        if (keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)
                        ) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "� Blocking $keyName (part of SELECT+START combo) - preventing leak to core"
                                )
                                android.util.Log.d(
                                        "ControllerInput",
                                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                                )
                                return true // Event intercepted - don't send to core
                        }
                }

                android.util.Log.d(
                        "ControllerInput",
                        "🟢 Event $keyName $actionName → SENDING TO CORE (not intercepted)"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                )
                return false // Event not intercepted - send to core
        }
        fun processKeyEvent(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean? {
                // DEBUG: Log ALL keyCodes to detect button mappings
                if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🎮 processKeyEvent: keyCode=$keyCode action=${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} (BUTTON_A=96, BUTTON_B=97)"
                        )
                }

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
                                executeMenuCallback(
                                        startButtonCallback,
                                        lastStartButtonCallbackTime
                                ) { lastStartButtonCallbackTime = it }
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
                // Durante grace period, NÃO bloquear A (só menu aberto bloqueia)
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A && shouldInterceptDpadForMenu()) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "BUTTON_A intercepted for menu confirmation"
                                )
                                executeMenuCallback(
                                        menuConfirmCallback,
                                        lastMenuConfirmCallbackTime
                                ) { lastMenuConfirmCallbackTime = it }
                        }
                        // CRITICAL: Bloquear TANTO ACTION_DOWN quanto ACTION_UP
                        // Isso impede que o ACTION_UP vaze para o jogo
                        return true // Consume the event, don't send to core
                }

                // INTERCEPT BUTTON B to go back when menu is open
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && shouldInterceptSpecificButton(keyCode)
                ) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🔵 BUTTON_B intercepted in processKeyEvent - action=${event.action} (DOWN=0, UP=1), shouldIntercept=true"
                        )
                        if (event.action == KeyEvent.ACTION_DOWN) {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "BUTTON_B intercepted for menu back"
                                )
                                executeMenuCallback(menuBackCallback, lastMenuBackCallbackTime) {
                                        lastMenuBackCallbackTime = it
                                }
                        } else {
                                android.util.Log.d(
                                        "ControllerInput",
                                        "🚫 BUTTON_B ACTION_UP intercepted - blocking from reaching game"
                                )
                        }
                        // CRITICAL: Bloquear TANTO ACTION_DOWN quanto ACTION_UP
                        // Isso impede que o ACTION_UP vaze para o jogo
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
                                                executeMenuCallback(
                                                        menuNavigateUpCallback,
                                                        lastMenuNavigateUpCallbackTime
                                                ) { lastMenuNavigateUpCallbackTime = it }
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
                                                executeMenuCallback(
                                                        menuNavigateDownCallback,
                                                        lastMenuNavigateDownCallbackTime
                                                ) { lastMenuNavigateDownCallbackTime = it }
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
                val shouldBlock = shouldBlockAllGamepadInput()
                android.util.Log.d(
                        "ControllerInput",
                        "🎮 processKeyEvent: shouldBlockAllGamepadInput() = $shouldBlock (keyCode: $keyCode, action: ${event.action})"
                )
                if (shouldBlock) {
                        android.util.Log.d(
                                "ControllerInput",
                                "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)"
                        )
                        return true // Block completely, don't send to core
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
                val shouldBlock = shouldBlockAllGamepadInput()
                android.util.Log.d(
                        "ControllerInput",
                        "🎮 processMotionEvent: shouldBlockAllGamepadInput() = $shouldBlock (DPAD_X: ${event.getAxisValue(MotionEvent.AXIS_HAT_X)}, DPAD_Y: ${event.getAxisValue(MotionEvent.AXIS_HAT_Y)})"
                )
                if (shouldBlock) {
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
                                        executeMenuCallback(
                                                menuNavigateUpCallback,
                                                lastMenuNavigateUpCallbackTime
                                        ) { lastMenuNavigateUpCallbackTime = it }
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
                                        executeMenuCallback(
                                                menuNavigateDownCallback,
                                                lastMenuNavigateDownCallbackTime
                                        ) { lastMenuNavigateDownCallbackTime = it }
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
