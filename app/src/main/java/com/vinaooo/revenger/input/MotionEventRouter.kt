package com.vinaooo.revenger.input

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView

/**
 * Owns [ControllerInput.processMotionEvent]'s single-trigger DPAD/analog-stick menu-navigation
 * interception and its pass-through to the emulator core, extracted to keep that class's
 * `processMotionEvent` within detekt's `LongMethod`/`CyclomaticComplexMethod`/`ReturnCount`
 * thresholds. [ControllerInput] keeps a public `processMotionEvent` delegating to [process], so
 * no external call site changes.
 */
class MotionEventRouter(
        private val callbacksProvider: () -> ControllerInputCallbacks,
        private val callbackDebouncer: MenuCallbackDebouncer
) {

        companion object {
                // Fixed threshold values for single-trigger system
                private const val DPAD_THRESHOLD: Float = 0.1f // Physical DPAD - more responsive
                private const val LEFT_ANALOG_THRESHOLD: Float = 0.7f // Left analog - less sensitive
        }

        private val callbacks get() = callbacksProvider()

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

        /** Logs the raw hat/analog axis values read from [event], for interception diagnostics. */
        private fun logMotionAxes(hatX: Float, hatY: Float, axisX: Float, axisY: Float) {
                android.util.Log.d("ControllerInput", "[INTERCEPT] 🎮 ========== DPAD/ANALOG INTERCEPTION START ==========")
                android.util.Log.d("ControllerInput", "[INTERCEPT] 📊 shouldInterceptDpadForMenu=${callbacks.shouldInterceptDpadForMenu()}")
                android.util.Log.d("ControllerInput", "[INTERCEPT] 📊 MotionEvent values:")
                android.util.Log.d("ControllerInput", "[INTERCEPT]   🎯 hatX=$hatX, hatY=$hatY")
                android.util.Log.d("ControllerInput", "[INTERCEPT]   🎯 axisX=$axisX, axisY=$axisY")
        }

        /** Single-trigger DPAD hat direction, falling back to the left analog stick. */
        private fun computeDirectionTrigger(hatX: Float, hatY: Float, axisX: Float, axisY: Float): Int? {
                val dpadTrigger = checkSingleTrigger(
                        currentUp = hatY < -DPAD_THRESHOLD,
                        currentDown = hatY > DPAD_THRESHOLD,
                        currentLeft = hatX < -DPAD_THRESHOLD,
                        currentRight = hatX > DPAD_THRESHOLD,
                        previousState = dpadState
                )
                val analogTrigger = checkSingleTrigger(
                        currentUp = axisY < -LEFT_ANALOG_THRESHOLD,
                        currentDown = axisY > LEFT_ANALOG_THRESHOLD,
                        currentLeft = axisX < -LEFT_ANALOG_THRESHOLD,
                        currentRight = axisX > LEFT_ANALOG_THRESHOLD,
                        previousState = leftAnalogState
                )
                return dpadTrigger ?: analogTrigger
        }

        /** Fires the debounced menu-navigate callback for [directionName], logging as it goes. */
        private fun fireMenuNavigate(
                directionName: String,
                icon: String,
                callback: () -> Unit,
                lastExecutionTime: Long,
                updateTime: (Long) -> Unit
        ): Boolean {
                android.util.Log.d("ControllerInput", "[INTERCEPT] $icon $directionName detected - calling menuNavigate${directionName}Callback")
                callbackDebouncer.executeMenuCallback(callback, lastExecutionTime, updateTime)
                android.util.Log.d("ControllerInput", "[INTERCEPT] ✅ $directionName callback completed - returning true")
                return true
        }

        private fun fireTriggerCallback(trigger: Int): Boolean =
                when (trigger) {
                        KeyEvent.KEYCODE_DPAD_UP ->
                                fireMenuNavigate(
                                        "Up",
                                        "⬆️",
                                        callbacks.menuNavigateUpCallback,
                                        callbackDebouncer.lastMenuNavigateUpCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateUpCallbackTime = it }
                        KeyEvent.KEYCODE_DPAD_DOWN ->
                                fireMenuNavigate(
                                        "Down",
                                        "⬇️",
                                        callbacks.menuNavigateDownCallback,
                                        callbackDebouncer.lastMenuNavigateDownCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateDownCallbackTime = it }
                        KeyEvent.KEYCODE_DPAD_LEFT ->
                                fireMenuNavigate(
                                        "Left",
                                        "⬅️",
                                        callbacks.menuNavigateLeftCallback,
                                        callbackDebouncer.lastMenuNavigateLeftCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateLeftCallbackTime = it }
                        else ->
                                fireMenuNavigate(
                                        "Right",
                                        "➡️",
                                        callbacks.menuNavigateRightCallback,
                                        callbackDebouncer.lastMenuNavigateRightCallbackTime
                                ) { callbackDebouncer.lastMenuNavigateRightCallbackTime = it }
                }

        /** True if any supported axis is out of deadzone, regardless of a new single-trigger. */
        private fun isAnyAxisOutOfDeadzone(hatX: Float, hatY: Float, axisX: Float, axisY: Float): Boolean =
                Math.abs(hatX) > DPAD_THRESHOLD ||
                        Math.abs(hatY) > DPAD_THRESHOLD ||
                        Math.abs(axisX) > LEFT_ANALOG_THRESHOLD ||
                        Math.abs(axisY) > LEFT_ANALOG_THRESHOLD

        /**
         * Checks the DPAD hat and left analog stick for a single-trigger direction and, if found,
         * fires the matching debounced menu-navigate callback. Returns `true` if the event should
         * be considered consumed (a direction fired, or any supported axis is simply held past
         * its deadzone without a new trigger), `null` if nothing here applies and the caller
         * should fall through to sending the event to the core.
         */
        private fun interceptDpadForMenu(event: MotionEvent): Boolean? {
                val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
                val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                val axisX = event.getAxisValue(MotionEvent.AXIS_X)
                val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
                logMotionAxes(hatX, hatY, axisX, axisY)

                val trigger = computeDirectionTrigger(hatX, hatY, axisX, axisY)
                if (trigger != null) {
                        return fireTriggerCallback(trigger)
                }

                return if (isAnyAxisOutOfDeadzone(hatX, hatY, axisX, axisY)) true else null
        }

        /** Sends the raw DPAD/left-analog/right-analog axes for [event] straight to the core. */
        private fun sendMotionToCore(event: MotionEvent, retroView: RetroView) {
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

        fun process(event: MotionEvent, retroView: RetroView): Boolean? {
                /* We're not ready yet! */
                if (retroView.frameRendered.value == false) return null

                // COMPLETELY BLOCK all controls when RetroMenu3 is open
                // (Handled at the end of this function to ensure all events update single-trigger state first)
                val shouldBlock = callbacks.shouldBlockAllGamepadInput()
                android.util.Log.d("ControllerInput", "🎮 processMotionEvent: shouldBlockAllGamepadInput() = $shouldBlock")

                // INTERCEPT DPAD and Left Analog for menu navigation when RetroMenu3 is open
                if (callbacks.shouldInterceptDpadForMenu()) {
                        val intercepted = interceptDpadForMenu(event)
                        if (intercepted != null) {
                                return intercepted
                        }
                } else {
                        android.util.Log.d(
                                "ControllerInput",
                                "[INTERCEPT] 🚫 DPAD interception disabled - " +
                                        "shouldInterceptDpadForMenu=${callbacks.shouldInterceptDpadForMenu()}"
                        )
                }

                // Send motion events to game ONLY if not blocked
                if (!shouldBlock) {
                        sendMotionToCore(event, retroView)
                }

                return true
        }
}
