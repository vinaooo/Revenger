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
     * IMPORTANTE: NÃO reseta comboAlreadyTriggered aqui! O reset de comboAlreadyTriggered deve
     * acontecer APENAS quando AMBOS os botões do combo (START e SELECT) são fisicamente SOLTOS pelo
     * usuário (ACTION_UP).
     *
     * Isso previne o bug onde:
     * 1. Menu abre com SELECT+START (comboAlreadyTriggered = true)
     * 2. START fecha o menu e chama clearKeyLog()
     * 3. Se resetássemos comboAlreadyTriggered aqui, e o usuário ainda estivesse
     * ```
     *    segurando os botões, o ACTION_UP subsequente poderia causar detecções fantasmas
     * ```
     * 4. Resultado: usuário precisa apertar SELECT+START DUAS VEZES para reabrir o menu
     *
     * A lógica correta de reset está no processKeyEvent() (linhas 293-303)
     */
    fun clearKeyLog() {
        android.util.Log.d("ControllerInput", "")
        android.util.Log.d("ControllerInput", "🧹 clearKeyLog() CALLED")
        android.util.Log.d(
                "ControllerInput",
                "   BEFORE: keyLog=$keyLog, comboAlreadyTriggered=$comboAlreadyTriggered"
        )
        keyLog.clear()
        // NÃO resetar comboAlreadyTriggered aqui - deixar o reset natural acontecer
        // quando AMBOS os botões forem fisicamente soltos pelo usuário
        lastComboTriggerTime = 0L // Reset cooldown timer to allow immediate combo detection
        android.util.Log.d(
                "ControllerInput",
                "   AFTER: keyLog=$keyLog, comboAlreadyTriggered=$comboAlreadyTriggered (kept as-is)"
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

    /** Callbacks para navegação no RetroMenu3 */
    var menuNavigateUpCallback: () -> Unit = {}
    var menuNavigateDownCallback: () -> Unit = {}
    var menuConfirmCallback: () -> Unit = {}

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

        // Log para debug - DETALHADO
        android.util.Log.d(
                "ControllerInput",
                "┌─────────────────────────────────────────────────────────"
        )
        android.util.Log.d("ControllerInput", "│ checkMenuKeyCombo CALLED")
        android.util.Log.d("ControllerInput", "│ keyLog: $keyLog")
        android.util.Log.d("ControllerInput", "│ hasSelectAndStart: $hasSelectAndStart")
        android.util.Log.d("ControllerInput", "│ comboAlreadyTriggered: $comboAlreadyTriggered")

        // Verificar cooldown para evitar detecções muito rápidas
        val currentTime = System.currentTimeMillis()
        val timeSinceLastTrigger = currentTime - lastComboTriggerTime

        android.util.Log.d(
                "ControllerInput",
                "│ timeSinceLastTrigger: ${timeSinceLastTrigger}ms (cooldown: ${COMBO_COOLDOWN_MS}ms)"
        )
        android.util.Log.d(
                "ControllerInput",
                "│ shouldHandleSelectStartCombo(): ${shouldHandleSelectStartCombo()}"
        )

        if (hasSelectAndStart &&
                        !comboAlreadyTriggered &&
                        shouldHandleSelectStartCombo() &&
                        timeSinceLastTrigger > COMBO_COOLDOWN_MS
        ) {

            android.util.Log.d("ControllerInput", "│ ✅ ALL CONDITIONS MET - COMBO DETECTED!")
            comboAlreadyTriggered = true // Mark combo as triggered
            lastComboTriggerTime = currentTime
            selectStartComboCallback()
            android.util.Log.d(
                    "ControllerInput",
                    "│ comboAlreadyTriggered NOW: true, timestamp: $lastComboTriggerTime"
            )
        } else {
            android.util.Log.d("ControllerInput", "│ ❌ COMBO NOT TRIGGERED - Missing condition:")
            if (!hasSelectAndStart) {
                android.util.Log.d("ControllerInput", "│    - hasSelectAndStart = false")
            }
            if (comboAlreadyTriggered) {
                android.util.Log.d(
                        "ControllerInput",
                        "│    - comboAlreadyTriggered = true (already triggered)"
                )
            }
            if (!shouldHandleSelectStartCombo()) {
                android.util.Log.d(
                        "ControllerInput",
                        "│    - shouldHandleSelectStartCombo() = false"
                )
            }
            if (timeSinceLastTrigger <= COMBO_COOLDOWN_MS) {
                android.util.Log.d(
                        "ControllerInput",
                        "│    - cooldown active (${timeSinceLastTrigger}ms < ${COMBO_COOLDOWN_MS}ms)"
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

        // INTERCEPTAR DPAD para navegação no menu quando RetroMenu3 estiver aberto
        if (shouldInterceptDpadForMenu()) {
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

            // Detectar mudanças no DPAD vertical
            // Valores: -1 = UP, 0 = CENTER, 1 = DOWN
            when {
                hatY == -1.0f && dpadState.up == false -> {
                    // DPAD UP pressionado (transição de false para true)
                    dpadState.up = true
                    android.util.Log.d("ControllerInput", "DPAD UP detected for menu navigation")
                    menuNavigateUpCallback()
                    return true // Consumir evento
                }
                hatY == 1.0f && dpadState.down == false -> {
                    // DPAD DOWN pressionado (transição de false para true)
                    dpadState.down = true
                    android.util.Log.d("ControllerInput", "DPAD DOWN detected for menu navigation")
                    menuNavigateDownCallback()
                    return true // Consumir evento
                }
                hatY == 0.0f -> {
                    // DPAD liberado (CENTER)
                    if (dpadState.up) {
                        dpadState.up = false
                        android.util.Log.d("ControllerInput", "DPAD UP released")
                    }
                    if (dpadState.down) {
                        dpadState.down = false
                        android.util.Log.d("ControllerInput", "DPAD DOWN released")
                    }
                    return true // Consumir evento
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
