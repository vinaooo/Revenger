package com.vinaooo.revenger.input

import android.content.Context
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView

class ControllerInput(private val context: Context) {
    companion object {
        private const val TAG = "ControllerInput"

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
            Log.d(TAG, "🔥 $inputName UP - NEW TRIGGER detected!")
            triggeredKeyCode = KeyEvent.KEYCODE_DPAD_UP
        }
        // Check DOWN transition (false -> true)
        else if (currentDown && !previousState.down) {
            Log.d(TAG, "🔥 $inputName DOWN - NEW TRIGGER detected!")
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
        Log.d(TAG, "🔍 checkPauseKey CALLED - keyLog.size: ${keyLog.size}, keyLog: $keyLog")

        // Log callback states for debugging
        val startHandleState = shouldHandleStartPause()
        val selectHandleState = shouldHandleSelectPause()
        val selectStartHandleState = shouldHandleSelectStartPause()
        Log.d(
                TAG,
                "🔍 Callback states - START: $startHandleState, SELECT: $selectHandleState, SELECT+START: $selectStartHandleState"
        )

        // PRIORITY 1: Check for SELECT + START combo FIRST (regardless of other keys)
        // This allows "hold SELECT, then press START" pattern
        if (keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT) &&
                        selectStartHandleState
        ) {
            Log.w(TAG, "🚨 SELECT+START COMBO DETECTED! (keyLog.size=${keyLog.size})")
            selectStartPauseCallback()
            return true // Block events from reaching core
        }

        // PRIORITY 2: Check for SELECT alone (ONLY if START is NOT pressed)
        if (keyLog.size == 1 && keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT) && selectHandleState
        ) {
            Log.w(TAG, "🚨 SELECT ALONE DETECTED!")
            selectPauseCallback()
            return true // Block events from reaching core
        }

        // PRIORITY 3: Check for START alone (ONLY if SELECT is NOT pressed)
        if (keyLog.size == 1 && keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) && startHandleState
        ) {
            Log.w(TAG, "🚨 START ALONE DETECTED!")
            pauseCallback()
            return true // Block events from reaching core
        }

        Log.d(TAG, "✅ No pause conditions met - keyLog ignored")
        return false // Don't block events
    }

    fun processGamePadButtonEvent(keyCode: Int, action: Int) {
        Log.d(
                TAG,
                "processGamePadButtonEvent - keyCode: $keyCode, action: $action (${KeyEvent.keyCodeToString(keyCode)})"
        )

        /* Keep track of user input events */
        when (action) {
            KeyEvent.ACTION_DOWN -> {
                keyLog.add(keyCode)
                Log.d(
                        TAG,
                        "🎮 GamePad Key DOWN: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog"
                )
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
                Log.d(
                        TAG,
                        "🎮 GamePad Key UP: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog"
                )
            }
        }

        Log.d(TAG, "🔄 Calling checkMenuKeyCombo and checkPauseKey from processGamePadButtonEvent")
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
        Log.w(
                TAG,
                "📸 Captured ${keysToBlockAfterMenuClose.size} pressed keys when menu opened: $keysToBlockAfterMenuClose"
        )
    }

    /**
     * Clear blocked keys after a delay (give time for ACTION_UP events to be processed and blocked)
     */
    fun clearBlockedKeysDelayed() {
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            Log.w(
                                    TAG,
                                    "🧹 Clearing blocked keys list (was: $keysToBlockAfterMenuClose)"
                            )
                            keysToBlockAfterMenuClose.clear()
                        },
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
            Log.w(TAG, "🚫 BLOCKING ACTION_UP for $keyCode - key was pressed when menu opened")
            keysToBlockAfterMenuClose.remove(keyCode) // Remove after blocking once

            // CRITICAL: Also remove from keyLog to prevent checkPauseKey from detecting it
            keyLog.remove(keyCode)
            Log.w(
                    TAG,
                    "🧹 Removed $keyCode from keyLog to prevent menu reopen, keyLog now: $keyLog"
            )

            return true // Block this ACTION_UP
        }

        // Log ALL key events for debugging DPAD issues - capture EVERY keycode
        val actionString =
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> "DOWN"
                    KeyEvent.ACTION_UP -> "UP"
                    else -> "OTHER(${event.action})"
                }
        Log.d(
                TAG,
                "🎮 ALL KEYS: keyCode=$keyCode (${KeyEvent.keyCodeToString(keyCode)}), action=$actionString, retroMenuVisible=${isRetroMenuVisible()}"
        )

        // Log ALL keycodes when menu visible to find the real DPAD codes
        if (isRetroMenuVisible()) {
            Log.d(
                    TAG,
                    "📍 MENU ACTIVE - CAPTURING KEY: $keyCode (${KeyEvent.keyCodeToString(keyCode)}) - action: $actionString"
            )
        }

        // If retro menu is visible, intercept navigation keys (analog motion converted to DPAD)
        // CRITICAL: Block BOTH ACTION_DOWN and ACTION_UP to prevent partial signals reaching core
        if (isRetroMenuVisible()) {
            Log.d(
                    TAG,
                    "Retro menu is visible, checking navigation key: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), action=$actionString"
            )
            when (keyCode) {
                // Only UP/DOWN navigation for menu (LEFT/RIGHT pass through to game)
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                // Button codes for menu actions
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B -> {
                    Log.d(
                            TAG,
                            "Intercepting UP/DOWN/A/B key for retro menu: $keyCode, action=$actionString"
                    )

                    // Only send ACTION_DOWN to menu for processing
                    // But block BOTH ACTION_DOWN and ACTION_UP from reaching the core
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (retroMenuNavigationCallback(keyCode)) {
                            Log.d(
                                    TAG,
                                    "Menu navigation key handled by retro menu, blocking ACTION_DOWN from game"
                            )
                            return true // Menu handled the input, don't send to game
                        } else {
                            Log.d(
                                    TAG,
                                    "Menu navigation key NOT handled by retro menu, sending ACTION_DOWN to game"
                            )
                        }
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        // CRITICAL FIX: Block ACTION_UP for menu keys to prevent pause signals
                        Log.d(TAG, "Blocking ACTION_UP for menu key $keyCode to prevent core pause")

                        // CRITICAL: Also remove from keyLog to prevent checkPauseKey detection
                        keyLog.remove(keyCode)
                        Log.d(TAG, "🧹 Removed menu key $keyCode from keyLog, now: $keyLog")

                        return true // Block ACTION_UP from reaching core
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    Log.d(
                            TAG,
                            "LEFT/RIGHT key while menu visible - passing through to game: $keyCode"
                    )
                    // LEFT/RIGHT pass through to game even when menu is visible
                }
                else -> {
                    Log.d(TAG, "Other key while menu visible: $keyCode, sending to game")
                }
            }
        } else {
            if (event.action == KeyEvent.ACTION_DOWN) {
                Log.d(
                        TAG,
                        "Retro menu not visible (${isRetroMenuVisible()}), sending key to game: $keyCode"
                )
            }
        }

        val port = getPort(event)
        
        /* Keep track of user input events BEFORE checking pause */
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                keyLog.add(keyCode)
                Log.d(
                        TAG,
                        "⌨️ Key DOWN: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog"
                )
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
                Log.d(
                        TAG,
                        "⌨️ Key UP: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog"
                )
            }
        }
        
        // Check for menu combo and pause key BEFORE sending to core
        Log.d(TAG, "🔄 Calling checkMenuKeyCombo and checkPauseKey from processKeyEvent")
        checkMenuKeyCombo()
        val shouldBlockPauseKey = checkPauseKey()
        
        // If pause key combo detected, block the events from reaching core
        if (shouldBlockPauseKey) {
            Log.w(TAG, "🚫 BLOCKING pause key combo from reaching core (SELECT/START/SELECT+START)")
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
        val xAxis = event.getAxisValue(MotionEvent.AXIS_X)
        val yAxis = event.getAxisValue(MotionEvent.AXIS_Y)

        Log.d(TAG, "🕹️ MOTION EVENT: X=$xAxis, Y=$yAxis, retroMenuVisible=${isRetroMenuVisible()}")

        // If retro menu is visible, use single-trigger navigation system
        if (isRetroMenuVisible()) {
            Log.d(TAG, "📍 Retro menu visible - processing SINGLE-TRIGGER navigation inputs")

            // Get all axis values
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val z = event.getAxisValue(MotionEvent.AXIS_Z)
            val rz = event.getAxisValue(MotionEvent.AXIS_RZ)

            Log.d(
                    TAG,
                    "🔍 RAW VALUES: DPAD(HAT_X=$hatX, HAT_Y=$hatY) | LEFT_ANALOG(X=$xAxis, Y=$yAxis) | RIGHT_ANALOG(Z=$z, RZ=$rz)"
            )

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
                Log.d(
                        TAG,
                        "🚀 SINGLE TRIGGER activated: ${KeyEvent.keyCodeToString(triggeredKeyCode)}"
                )
                if (retroMenuNavigationCallback(triggeredKeyCode)) {
                    Log.d(TAG, "✅ Single trigger handled by retro menu, blocking game input")
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
