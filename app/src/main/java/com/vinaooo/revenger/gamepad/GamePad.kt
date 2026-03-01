package com.vinaooo.revenger.gamepad

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.InputDevice
import androidx.lifecycle.LifecycleCoroutineScope
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.AppConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GamePad(
        context: Context,
        private val padConfig: RadialGamePadConfig,
        private val onButtonEvent: ((event: Event) -> Boolean)? = null
) {
    val pad = RadialGamePad(padConfig, 0f, context)

    companion object {
        /** Should the user see the on-screen controls? */
        fun shouldShowGamePads(activity: Activity, appConfig: AppConfig): Boolean {
            /* Config says we shouldn't use virtual controls */
            if (!appConfig.getGamepad()) return false

            /* Devices without a touchscreen don't need a GamePad */
            val hasTouchScreen =
                    activity.packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            if (hasTouchScreen == null || !hasTouchScreen) return false

            /* Fetch the current display that the game is running on */
            val currentDisplayId = activity.display!!.displayId

            /* Are we presenting this screen on a TV or display? */
            val dm = activity.getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
            if (dm.getDisplay(currentDisplayId).flags and Display.FLAG_PRESENTATION ==
                            Display.FLAG_PRESENTATION
            )
                    return false

            /* If a GamePad is connected, we definitely don't need touch controls */
            for (id in InputDevice.getDeviceIds()) {
                InputDevice.getDevice(id)?.apply {
                    if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
                            return false
                }
            }

            return true
        }
    }

    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> {
                // Log BEFORE the callback to see all events coming from the library
                val buttonName =
                        when (event.id) {
                            android.view.KeyEvent.KEYCODE_BUTTON_A -> "A"
                            android.view.KeyEvent.KEYCODE_BUTTON_B -> "B"
                            android.view.KeyEvent.KEYCODE_BUTTON_START -> "START"
                            android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
                            else -> event.id.toString()
                        }
                val actionName =
                        if (event.action == android.view.KeyEvent.ACTION_DOWN) "DOWN" else "UP"
                android.util.Log.d(
                        "GamePad",
                        "🎮 RadialGamePad event received: $buttonName $actionName (BEFORE callback)"
                )

                // Invoke the callback and check if the event was intercepted
                val intercepted = onButtonEvent?.invoke(event) ?: false

                android.util.Log.d(
                        "GamePad",
                        "🎮 Callback returned: intercepted=$intercepted (will ${if (intercepted) "BLOCK" else "SEND"} to core)"
                )

                // Only send to the core if NOT intercepted
                if (!intercepted) {
                    retroView.sendKeyEvent(event.action, event.id)
                }
            }
            is Event.Direction -> {
                // Invoke the callback and check if the event was intercepted
                val intercepted = onButtonEvent?.invoke(event) ?: false

                // Only send to the core if NOT intercepted
                if (!intercepted) {
                    when (event.id) {
                        GLRetroView.MOTION_SOURCE_DPAD ->
                                retroView.sendMotionEvent(
                                        GLRetroView.MOTION_SOURCE_DPAD,
                                        event.xAxis,
                                        event.yAxis
                                )
                        GLRetroView.MOTION_SOURCE_ANALOG_LEFT ->
                                retroView.sendMotionEvent(
                                        GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                                        event.xAxis,
                                        event.yAxis
                                )
                        GLRetroView.MOTION_SOURCE_ANALOG_RIGHT ->
                                retroView.sendMotionEvent(
                                        GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                                        event.xAxis,
                                        event.yAxis
                                )
                    }
                }
            }
        }
    }

    /** Register input events to the RetroView using Flow */
    fun subscribe(lifecycleScope: LifecycleCoroutineScope, retroView: GLRetroView): Job {
        return lifecycleScope.launch {
            pad.events().collectLatest { event: Event -> eventHandler(event, retroView) }
        }
    }
}
