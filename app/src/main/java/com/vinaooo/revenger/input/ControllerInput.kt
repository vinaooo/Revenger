package com.vinaooo.revenger.input

import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.vinaooo.revenger.retroview.RetroView
import com.swordfish.libretrodroid.GLRetroView

class ControllerInput {
    companion object {
        private const val TAG = "ControllerInput"

        /**
         * Combination to open the menu
         */
        val KEYCOMBO_MENU = setOf(
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT
        )

        /**
         * Any of these keys will not be piped to the RetroView
         */
        val EXCLUDED_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_POWER
        )
    }
    /**
     * Set of keys currently being held by the user
     */
    private val keyLog = mutableSetOf<Int>()

    /**
     * The callback for when the user inputs the menu key-combination
     */
    var menuCallback: () -> Unit = {}

    /**
     * The callback for when the user presses START button alone
     */
    var pauseCallback: () -> Unit = {}

    /**
     * Function to check if SELECT+START combo should trigger menu
     */
    var shouldHandleSelectStartCombo: () -> Boolean = { true }

    /**
     * Function to check if START alone should trigger pause overlay
     */
    var shouldHandleStartPause: () -> Boolean = { true }

    /**
     *  Controller numbers are [1, inf), we need [0, inf)
     */
    private fun getPort(event: InputEvent): Int =
        ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

    /**
     * Check if we should be showing the user the menu
     */
    private fun checkMenuKeyCombo() {
        if (keyLog == KEYCOMBO_MENU && shouldHandleSelectStartCombo())
            menuCallback()
    }

    /**
     * Check if we should show the pause overlay (START pressed alone)
     */
    private fun checkPauseKey() {
        Log.d(TAG, "checkPauseKey - keyLog.size: ${keyLog.size}, keyLog: $keyLog")
        Log.d(TAG, "checkPauseKey - contains START: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)}")
        Log.d(TAG, "checkPauseKey - shouldHandleStartPause: ${shouldHandleStartPause()}")

        // Debug: Check for alternative START button codes that might be used
        val alternativeStartCodes = listOf(
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_NUMPAD_ENTER
        )

        val hasAnyStartCode = alternativeStartCodes.any { keyLog.contains(it) }
        Log.d(TAG, "checkPauseKey - hasAnyStartCode: $hasAnyStartCode")

        if (keyLog.size == 1 &&
            keyLog.contains(KeyEvent.KEYCODE_BUTTON_START) &&
            shouldHandleStartPause()) {
            Log.d(TAG, "PAUSE CALLBACK TRIGGERED! Calling pauseCallback()")
            pauseCallback()
        } else {
            Log.d(TAG, "Pause conditions not met - size: ${keyLog.size}, hasStart: ${keyLog.contains(KeyEvent.KEYCODE_BUTTON_START)}, shouldHandle: ${shouldHandleStartPause()}")
        }
    }

    fun processGamePadButtonEvent(keyCode: Int, action: Int) {
        Log.d(TAG, "processGamePadButtonEvent - keyCode: $keyCode, action: $action (${KeyEvent.keyCodeToString(keyCode)})")

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
        if (EXCLUDED_KEYS.contains(keyCode))
            return null

        /* We're not ready yet! */
        if (retroView.frameRendered.value == false)
            return true

        val port = getPort(event)
        retroView.view.sendKeyEvent(event.action, keyCode, port)

        /* Keep track of user input events */
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                keyLog.add(keyCode)
                Log.d(TAG, "Key DOWN: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog")
            }
            KeyEvent.ACTION_UP -> {
                keyLog.remove(keyCode)
                Log.d(TAG, "Key UP: $keyCode (${KeyEvent.keyCodeToString(keyCode)}), keyLog: $keyLog")
            }
        }

        checkMenuKeyCombo()
        checkPauseKey()

        return true
    }

    fun processMotionEvent(event: MotionEvent, retroView: RetroView): Boolean? {
        /* We're not ready yet! */
        if (retroView.frameRendered.value == false)
            return null

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