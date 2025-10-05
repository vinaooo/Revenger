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
        val KEYCOMBO_MENU = setOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT)

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
    private val dpadThreshold: Float = 0.1f // DPAD físico - mais responsivo
    private val leftAnalogThreshold: Float = 0.7f // Analógico esquerdo - menos sensível
    private val rightAnalogThreshold: Float = 0.7f // Analógico direito - menos sensível

    // Single trigger system - tracks previous state to detect transitions (UP/DOWN only)
    private data class DirectionalState(var up: Boolean = false, var down: Boolean = false)

    // Track state for each input type to implement single-trigger navigation
    private val dpadState = DirectionalState()
    private val leftAnalogState = DirectionalState()
    private val rightAnalogState = DirectionalState()

    /** Set of keys currently being held by the user */
    private val keyLog = mutableSetOf<Int>()

    /**
     * Set of keys that were pressed when menu opened (to block their ACTION_UP after menu closes)
     */
    private val keysToBlockAfterMenuClose = mutableSetOf<Int>()

    /**
     * Flag to prevent combo from being detected multiple times while buttons are held Reset only
     * when BOTH combo buttons are released
     */
    private var comboAlreadyTriggered = false

    /** Timestamp of last combo detection to prevent rapid re-triggers */
    private var lastComboTriggerTime = 0L
    private val COMBO_COOLDOWN_MS = 500L // 500ms cooldown between combo detections

    /**
     * Limpa o keyLog para evitar detecção de combos após fechar o menu.
     *
     * IMPORTANTE: Reseta comboAlreadyTriggered APENAS se o menu não estiver mais aberto. Se o menu
     * ainda estiver aberto, mantém o flag para evitar detecções falsas.
     */
    fun clearKeyLog() {
        android.util.Log.d("ControllerInput", "")
        android.util.Log.d("ControllerInput", "🧹 clearKeyLog() CALLED")
        android.util.Log.d(
                "ControllerInput",
                "   BEFORE: keyLog=$keyLog, comboAlreadyTriggered=$comboAlreadyTriggered"
        )
        keyLog.clear()

        // 🔧 FIX: Reset comboAlreadyTriggered apenas se o menu não estiver aberto
        // Isso previne detecções falsas quando o menu é fechado, mas evita
        // resetar o flag se o menu ainda estiver aberto (ex: durante operações)
        if (!isRetroMenu3Open()) {
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
        android.util.Log.d(
                "ControllerInput",
                "   AFTER: keyLog=$keyLog, comboAlreadyTriggered=$comboAlreadyTriggered"
        )
        android.util.Log.d("ControllerInput", "")
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

    /** Callbacks para navegação no RetroMenu3 */
    var menuNavigateUpCallback: () -> Unit = {}
    var menuNavigateDownCallback: () -> Unit = {}
    var menuConfirmCallback: () -> Unit = {}
    var menuBackCallback: () -> Unit = {}

    /** Function to check if devemos interceptar DPAD para menu */
    var shouldInterceptDpadForMenu: () -> Boolean = { false }

    /**
     * Check for single-trigger directional input (UP/DOWN only) Returns the keycode if there's a
     * NEW press (transition from false to true) Returns null if no new input or input is being held
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
        // Verificar se temos exatamente os dois botões pressionados
        val hasSelectAndStart = keyLog.containsAll(KEYCOMBO_MENU) && keyLog.size == 2

        // Verificar cooldown para evitar detecções muito rápidas
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
        android.util.Log.d("ControllerInput", "│ comboAlreadyTriggered: $comboAlreadyTriggered")
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
        android.util.Log.d(
                "ControllerInput",
                "│ keyLog.size: ${keyLog.size} (should be 2 for combo)"
        )

        if (hasSelectAndStart &&
                        !comboAlreadyTriggered &&
                        shouldHandleSelectStartCombo() &&
                        timeSinceLastTrigger > COMBO_COOLDOWN_MS
        ) {

            android.util.Log.d("ControllerInput", "│ ✅ ALL CONDITIONS MET - COMBO DETECTED!")
            android.util.Log.d("ControllerInput", "│    - hasSelectAndStart: $hasSelectAndStart")
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
            android.util.Log.d("ControllerInput", "│ ❌ COMBO NOT TRIGGERED - Missing condition:")
            if (!hasSelectAndStart) {
                android.util.Log.d("ControllerInput", "│    - hasSelectAndStart = false")
                android.util.Log.d(
                        "ControllerInput",
                        "│    - SELECT in keyLog: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT)}"
                )
                android.util.Log.d(
                        "ControllerInput",
                        "│    - START in keyLog: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)}"
                )
                android.util.Log.d("ControllerInput", "│    - keyLog contents: $keyLog")
            }
            if (comboAlreadyTriggered) {
                android.util.Log.d(
                        "ControllerInput",
                        "│    - comboAlreadyTriggered = true (already triggered)"
                )
                // 🔍 DEBUGGING: Only log as RARE BUG if menu is NOT open
                // If shouldHandleSelectStartCombo() = true, menu is closed (should have been reset)
                // If shouldHandleSelectStartCombo() = false, menu is open (expected behavior)
                if (shouldHandleSelectStartCombo()) {
                    // Menu is CLOSED but flag is still true - this is the real bug!
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
        }
        android.util.Log.d(
                "ControllerInput",
                "└─────────────────────────────────────────────────────────"
        )
    }

    fun processGamePadButtonEvent(keyCode: Int, action: Int) {
        /* Keep track of user input events */
        when (action) {
            KeyEvent.ACTION_DOWN -> {
                val wasAlreadyPressed = keyLog.contains(keyCode)
                keyLog.add(keyCode)
                android.util.Log.d(
                        "ControllerInput",
                        "GamePad ACTION_DOWN: $keyCode, wasAlreadyPressed: $wasAlreadyPressed, keyLog: $keyLog"
                )

                // Se o botão já estava pressionado, não verificar combo novamente
                if (wasAlreadyPressed) {
                    android.util.Log.d(
                            "ControllerInput",
                            "Ignoring repeated GamePad ACTION_DOWN for $keyCode"
                    )
                    return // Ignorar evento repetido
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
     * Capture currently pressed keys when menu opens These keys will have their ACTION_UP blocked
     * after menu closes to prevent partial signals
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
     * Clear blocked keys after a delay (give time for ACTION_UP events to be processed and blocked)
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

        // BLOQUEAR START quando menu está aberto (RetroMenu3)
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
                // At this point we KNOW the user is CLOSING the menu, not trying to open it
                // So it's safe to reset the flag immediately
                comboAlreadyTriggered = false
                android.util.Log.d(
                        "ControllerInput",
                        "   ✅ comboAlreadyTriggered reset to false (START closed menu)"
                )
                android.util.Log.d("ControllerInput", "   startButtonCallback() completed")
            }
            return true // Consumir o evento, não enviar ao core
        }

        // INTERCEPTAR BOTÃO A para confirmação quando menu estiver aberto
        if (keyCode == KeyEvent.KEYCODE_BUTTON_A && shouldInterceptDpadForMenu()) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                android.util.Log.d("ControllerInput", "BUTTON_A intercepted for menu confirmation")
                menuConfirmCallback()
            }
            return true // Consumir o evento, não enviar ao core
        }

        // INTERCEPTAR BOTÃO B para voltar quando menu estiver aberto
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B && shouldInterceptDpadForMenu()) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                android.util.Log.d("ControllerInput", "BUTTON_B intercepted for menu back")
                menuBackCallback()
            }
            return true // Consumir o evento, não enviar ao core
        }

        // INTERCEPTAR DPAD (KeyEvents) para navegação quando menu estiver aberto
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
                                "DPAD UP (KeyEvent) intercepted for menu navigation"
                        )
                        menuNavigateUpCallback()
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        android.util.Log.d(
                                "ControllerInput",
                                "DPAD DOWN (KeyEvent) intercepted for menu navigation"
                        )
                        menuNavigateDownCallback()
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        android.util.Log.d("ControllerInput", "DPAD LEFT (KeyEvent) intercepted")
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        android.util.Log.d("ControllerInput", "DPAD RIGHT (KeyEvent) intercepted")
                    }
                }
            }
            return true // Consumir o evento, não enviar ao core
        }

        // BLOQUEAR COMPLETAMENTE todos os controles quando RetroMenu3 estiver aberto
        // EXCETO os que já foram tratados acima (START, A, DPAD)
        if (shouldBlockAllGamepadInput()) {
            android.util.Log.d(
                    "ControllerInput",
                    "🛑 BLOCKING GAMEPAD INPUT - RetroMenu3 is open (keyCode: $keyCode)"
            )
            return true // Bloquear completamente, não enviar ao core
        }

        // CRITICAL FIX: Block ACTION_UP for keys that were pressed when menu opened
        // This prevents partial signals (ACTION_UP without ACTION_DOWN) from reaching the core
        if (event.action == KeyEvent.ACTION_UP && keysToBlockAfterMenuClose.contains(keyCode)) {
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

            // BUGFIX: Reset combo flag if BOTH combo buttons are released (even when blocked)
            // This fixes the issue where user needs to press SELECT+START twice after closing menu
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

                // Se o botão já estava pressionado, não verificar combo novamente
                if (wasAlreadyPressed) {
                    android.util.Log.d(
                            "ControllerInput",
                            "   ⚠️ Ignoring repeated ACTION_DOWN for $keyName"
                    )
                    return true // Ignorar evento repetido
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

        // BLOQUEAR START e SELECT de chegarem ao core quando o combo é detectado
        // Usar containsAll para verificar se ambos estão presentes
        if ((keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) &&
                        keyLog.containsAll(KEYCOMBO_MENU) &&
                        keyLog.size == 2
        ) {
            android.util.Log.d(
                    "ControllerInput",
                    "Blocking START/SELECT from reaching core - combo detected"
            )
            return true // Consumir o evento, não enviar ao core
        }

        // Normal key, send to core
        retroView.view.sendKeyEvent(event.action, keyCode, port)

        return true
    }

    fun processMotionEvent(event: MotionEvent, retroView: RetroView): Boolean? {
        /* We're not ready yet! */
        if (retroView.frameRendered.value == false) return null

        // BLOQUEAR COMPLETAMENTE todos os controles quando RetroMenu3 estiver aberto
        // EXCETO DPAD (AXIS_HAT) que é usado para navegação no menu
        if (shouldBlockAllGamepadInput()) {
            // Permitir apenas eventos de DPAD (hat axes) quando menu está aberto
            val isDpadEvent =
                    event.getAxisValue(MotionEvent.AXIS_HAT_X) != 0f ||
                            event.getAxisValue(MotionEvent.AXIS_HAT_Y) != 0f

            if (!isDpadEvent) {
                android.util.Log.d(
                        "ControllerInput",
                        "🛑 BLOCKING GAMEPAD MOTION INPUT - RetroMenu3 is open (non-DPAD)"
                )
                return true // Bloquear motion events não-DPAD quando menu está aberto
            }
            // Se é DPAD event, deixar passar para a lógica de navegação abaixo
        }

        // INTERCEPTAR DPAD para navegação no menu quando RetroMenu3 estiver aberto
        if (shouldInterceptDpadForMenu()) {
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

            // Lógica simplificada: detectar direção atual do DPAD
            when {
                hatY < -0.5f -> { // DPAD UP
                    android.util.Log.d("ControllerInput", "DPAD UP pressed for menu navigation")
                    menuNavigateUpCallback()
                    return true
                }
                hatY > 0.5f -> { // DPAD DOWN
                    android.util.Log.d("ControllerInput", "DPAD DOWN pressed for menu navigation")
                    menuNavigateDownCallback()
                    return true
                }
                hatX < -0.5f -> { // DPAD LEFT (se necessário no futuro)
                    android.util.Log.d("ControllerInput", "DPAD LEFT pressed")
                    return true
                }
                hatX > 0.5f -> { // DPAD RIGHT (se necessário no futuro)
                    android.util.Log.d("ControllerInput", "DPAD RIGHT pressed")
                    return true
                }
            }
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
