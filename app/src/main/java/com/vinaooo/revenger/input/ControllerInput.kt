package com.vinaooo.revenger.input

import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView

class ControllerInput {
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
    /** Set of keys currently being held by the user */
    private val keyLog = mutableSetOf<Int>()

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

    /** Controller numbers are [1, inf), we need [0, inf) */
    private fun getPort(event: InputEvent): Int =
            ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

    /** Check if we should be showing the user the menu */
    private fun checkMenuKeyCombo() {
        if (keyLog == KEYCOMBO_MENU && shouldHandleSelectStartCombo()) menuCallback()
    }

    /** Check if we should show the pause overlay */
    private fun checkPauseKey() {
        Log.d(TAG, "checkPauseKey - keyLog.size: ${keyLog.size}, keyLog: $keyLog")

        // Check for SELECT + START combo (mode 3)
        if (keyLog.size == 2 &&
                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT) &&
                        shouldHandleSelectStartPause()
        ) {
            Log.d(TAG, "SELECT+START PAUSE CALLBACK TRIGGERED!")
            selectStartPauseCallback()
            return
        }

        // Check for SELECT alone (mode 2)
        if (keyLog.size == 1 &&
                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_SELECT) &&
                        shouldHandleSelectPause()
        ) {
            Log.d(TAG, "SELECT PAUSE CALLBACK TRIGGERED!")
            selectPauseCallback()
            return
        }

        // Check for START alone (mode 1)
        if (keyLog.size == 1 &&
                        keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
                        shouldHandleStartPause()
        ) {
            Log.d(TAG, "START PAUSE CALLBACK TRIGGERED!")
            pauseCallback()
            return
        }

        Log.d(TAG, "No pause conditions met")
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
                Log.d(TAG, "GamePad Key DOWN: $keyCode, keyLog: $keyLog")
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
                Log.d(TAG, "GamePad Key UP: $keyCode, keyLog: $keyLog")
            }
        }

        checkMenuKeyCombo()
        checkPauseKey()
    }

    fun processKeyEvent(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean? {
        /* Block these keys! */
        if (EXCLUDED_KEYS.contains(keyCode)) return null

        /* We're not ready yet! */
        if (retroView.frameRendered.value == false) return true

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
        if (isRetroMenuVisible() && event.action == KeyEvent.ACTION_DOWN) {
            Log.d(
                    TAG,
                    "Retro menu is visible, checking navigation key: $keyCode (${KeyEvent.keyCodeToString(keyCode)})"
            )
            when (keyCode) {
                // Standard DPAD codes (converted from analog motion)
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                // Button codes
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B -> {
                    Log.d(TAG, "Intercepting navigation key for retro menu: $keyCode")
                    // Send to retro menu for navigation
                    if (retroMenuNavigationCallback(keyCode)) {
                        Log.d(TAG, "Navigation key handled by retro menu, blocking game input")
                        return true // Menu handled the input, don't send to game
                    } else {
                        Log.d(TAG, "Navigation key NOT handled by retro menu, sending to game")
                    }
                }
                else -> {
                    Log.d(TAG, "Non-navigation key while menu visible: $keyCode, sending to game")
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
        retroView.view.sendKeyEvent(event.action, keyCode, port)

        /* Keep track of user input events */
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                keyLog.add(keyCode)
                Log.d(
                        TAG,
                        "Key DOWN: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog"
                )
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
                Log.d(
                        TAG,
                        "Key UP: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog"
                )
            }
        }

        checkMenuKeyCombo()
        checkPauseKey()

        return true
    }

    fun processMotionEvent(event: MotionEvent, retroView: RetroView): Boolean? {
        /* We're not ready yet! */
        if (retroView.frameRendered.value == false) return null

        // Get analog stick values
        val xAxis = event.getAxisValue(MotionEvent.AXIS_X)
        val yAxis = event.getAxisValue(MotionEvent.AXIS_Y)

        Log.d(TAG, "🕹️ MOTION EVENT: X=$xAxis, Y=$yAxis, retroMenuVisible=${isRetroMenuVisible()}")

        // If retro menu is visible, intercept analog stick movements for navigation
        if (isRetroMenuVisible()) {
            Log.d(TAG, "📍 Retro menu visible - processing analog navigation: X=$xAxis, Y=$yAxis")

            // Try multiple axis sources for GameSir-G8 compatibility
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val z = event.getAxisValue(MotionEvent.AXIS_Z)
            val rz = event.getAxisValue(MotionEvent.AXIS_RZ)

            Log.d(
                    TAG,
                    "🔍 ALL AXIS VALUES: X=$xAxis, Y=$yAxis, HAT_X=$hatX, HAT_Y=$hatY, Z=$z, RZ=$rz"
            )

            // Very low threshold to catch any movement
            val threshold = 0.1f

            var handled = false

            // Try all possible axis combinations for GameSir-G8
            val allXValues = listOf(xAxis, hatX, z)
            val allYValues = listOf(yAxis, hatY, rz)

            // Check Y-axis (UP/DOWN navigation) - try all Y sources
            for (yValue in allYValues) {
                if (yValue < -threshold) {
                    Log.d(TAG, "🔼 Analog UP detected (Y=$yValue), sending DPAD_UP navigation")
                    if (retroMenuNavigationCallback(KeyEvent.KEYCODE_DPAD_UP)) {
                        handled = true
                        break
                    }
                } else if (yValue > threshold) {
                    Log.d(TAG, "🔽 Analog DOWN detected (Y=$yValue), sending DPAD_DOWN navigation")
                    if (retroMenuNavigationCallback(KeyEvent.KEYCODE_DPAD_DOWN)) {
                        handled = true
                        break
                    }
                }
            }

            // Check X-axis (LEFT/RIGHT navigation) - try all X sources
            if (!handled) {
                for (xValue in allXValues) {
                    if (xValue < -threshold) {
                        Log.d(
                                TAG,
                                "◀️ Analog LEFT detected (X=$xValue), sending DPAD_LEFT navigation"
                        )
                        if (retroMenuNavigationCallback(KeyEvent.KEYCODE_DPAD_LEFT)) {
                            handled = true
                            break
                        }
                    } else if (xValue > threshold) {
                        Log.d(
                                TAG,
                                "▶️ Analog RIGHT detected (X=$xValue), sending DPAD_RIGHT navigation"
                        )
                        if (retroMenuNavigationCallback(KeyEvent.KEYCODE_DPAD_RIGHT)) {
                            handled = true
                            break
                        }
                    }
                }
            }

            if (handled) {
                Log.d(TAG, "✅ Analog navigation handled by retro menu, blocking game input")
                return true // Menu handled the input, don't send to game
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
