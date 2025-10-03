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
     * Limpa o keyLog para evitar detecção de combos após fechar o menu. CRÍTICO: Deve ser chamado
     * quando o menu fecha para evitar reabertura imediata.
     */
    fun clearKeyLog() {
        keyLog.clear()
    }

    /** The callback for when the user inputs the menu key-combination */
    var menuCallback: () -> Unit = {}

    /** The callback for when the user presses START button alone */
    var pauseCallback: () -> Unit = {}

    /** The callback for when the user presses SELECT button alone */
    var selectPauseCallback: () -> Unit = {}

    /** The callback for when the user presses SELECT + START together */
    var selectStartPauseCallback: () -> Unit = {}

    /** Function to check if SELECT+START combo should trigger menu */
    var shouldHandleSelectStartCombo: () -> Boolean = { true }

    /** Function to check if START alone should trigger pause overlay */
    var shouldHandleStartPause: () -> Boolean = { true }

    /** Function to check if SELECT alone should trigger pause overlay */
    var shouldHandleSelectPause: () -> Boolean = { true }

    /** Function to check if SELECT+START should trigger pause overlay */
    var shouldHandleSelectStartPause: () -> Boolean = { true }

    /** Callback for retro menu navigation (DPAD, A, B buttons) */
    var retroMenuNavigationCallback: (Int) -> Boolean = { false }

    /** Function to check if retro menu is currently visible */
    var isRetroMenuVisible: () -> Boolean = { false }

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
        if (keyLog == KEYCOMBO_MENU && shouldHandleSelectStartCombo()) menuCallback()
    }

    /** Check if we should show the pause overlay */
    private fun checkPauseKey(): Boolean {
        val startHandleState = shouldHandleStartPause()
        val selectHandleState = shouldHandleSelectPause()
        val selectStartHandleState = shouldHandleSelectStartPause()

        // PRIORITY 1: Check for SELECT + START combo FIRST (regardless of other keys)
        // This allows "hold SELECT, then press START" pattern
        if (keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT) &&
                        selectStartHandleState
        ) {
            selectStartPauseCallback()
            return true // Block events from reaching core
        }

        // PRIORITY 2: Check for SELECT alone (ONLY if START is NOT pressed)
        if (keyLog.size == 1 && keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT) && selectHandleState
        ) {
            selectPauseCallback()
            return true // Block events from reaching core
        }

        // PRIORITY 3: Check for START alone (ONLY if SELECT is NOT pressed)
        if (keyLog.size == 1 && keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) && startHandleState
        ) {
            pauseCallback()
            return true // Block events from reaching core
        }
        return false // Don't block events
    }

    fun processGamePadButtonEvent(keyCode: Int, action: Int) {
        /* Keep track of user input events */
        when (action) {
            KeyEvent.ACTION_DOWN -> {
                keyLog.add(keyCode)
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
            }
        }

        checkMenuKeyCombo()
        checkPauseKey()
    }

    /**
     * Capture currently pressed keys when menu opens These keys will have their ACTION_UP blocked
     * after menu closes to prevent partial signals
     */
    fun captureKeysOnMenuOpen() {
        keysToBlockAfterMenuClose.clear()
        keysToBlockAfterMenuClose.addAll(keyLog)
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

        // CRITICAL FIX: Block ACTION_UP for keys that were pressed when menu opened
        // This prevents partial signals (ACTION_UP without ACTION_DOWN) from reaching the core
        if (event.action == KeyEvent.ACTION_UP && keysToBlockAfterMenuClose.contains(keyCode)) {
            keysToBlockAfterMenuClose.remove(keyCode) // Remove after blocking once

            // CRITICAL: Also remove from keyLog to prevent checkPauseKey from detecting it
            keyLog.remove(keyCode)

            return true // Block this ACTION_UP
        }

        // If retro menu is visible, intercept navigation keys (analog motion converted to DPAD)
        // CRITICAL: Block BOTH ACTION_DOWN and ACTION_UP to prevent partial signals reaching core
        if (isRetroMenuVisible()) {
            when (keyCode) {
                // Only UP/DOWN navigation for menu (LEFT/RIGHT pass through to game)
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                // Button codes for menu actions
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B -> {
                    // Only send ACTION_DOWN to menu for processing
                    // But block BOTH ACTION_DOWN and ACTION_UP from reaching the core
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (retroMenuNavigationCallback(keyCode)) {
                            return true // Menu handled the input, don't send to game
                        }
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        // CRITICAL FIX: Block ACTION_UP for menu keys to prevent pause signals
                        // CRITICAL: Also remove from keyLog to prevent checkPauseKey detection
                        keyLog.remove(keyCode)

                        return true // Block ACTION_UP from reaching core
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    // LEFT/RIGHT pass through to game even when menu is visible
                }
                else -> {}
            }
        } else {
            if (event.action == KeyEvent.ACTION_DOWN) {}
        }

        val port = getPort(event)

        /* Keep track of user input events BEFORE checking pause */
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                keyLog.add(keyCode)
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
            }
        }

        checkMenuKeyCombo()
        val shouldBlockPauseKey = checkPauseKey()

        // If pause key combo detected, block the events from reaching core
        if (shouldBlockPauseKey) {
            return true // Block completely
        }

        // Normal key, send to core
        retroView.view.sendKeyEvent(event.action, keyCode, port)

        return true
    }

    fun processMotionEvent(event: MotionEvent, retroView: RetroView): Boolean? {
        /* We're not ready yet! */
        if (retroView.frameRendered.value == false) return null

        // Get analog stick values
        val yAxis = event.getAxisValue(MotionEvent.AXIS_Y)

        // If retro menu is visible, use single-trigger navigation system
        if (isRetroMenuVisible()) {
            // Get all axis values
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val rz = event.getAxisValue(MotionEvent.AXIS_RZ)

            // 1. DPAD físico (HAT_Y only) - Prioridade máxima, apenas UP/DOWN
            val dpadUp = hatY < -dpadThreshold
            val dpadDown = hatY > dpadThreshold

            var triggeredKeyCode = checkSingleTrigger(dpadUp, dpadDown, dpadState, "DPAD")

            // 2. Analógico esquerdo (AXIS_Y only) - Se DPAD não triggered, apenas UP/DOWN
            if (triggeredKeyCode == null) {
                val leftUp = yAxis < -leftAnalogThreshold
                val leftDown = yAxis > leftAnalogThreshold

                triggeredKeyCode =
                        checkSingleTrigger(leftUp, leftDown, leftAnalogState, "LEFT_ANALOG")
            }

            // 3. Analógico direito (AXIS_RZ only) - Se outros não triggered, apenas UP/DOWN
            if (triggeredKeyCode == null) {
                val rightUp = rz < -rightAnalogThreshold
                val rightDown = rz > rightAnalogThreshold

                triggeredKeyCode =
                        checkSingleTrigger(rightUp, rightDown, rightAnalogState, "RIGHT_ANALOG")
            }

            // Se houve um trigger, enviar para o menu
            if (triggeredKeyCode != null) {
                if (retroMenuNavigationCallback(triggeredKeyCode)) {
                    return true // Menu handled the input, don't send to game
                }
            }
        }

        // Send motion events to game (normal behavior)
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
