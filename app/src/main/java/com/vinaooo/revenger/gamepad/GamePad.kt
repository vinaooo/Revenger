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
import com.vinaooo.revenger.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GamePad(
        context: Context,
        padConfig: RadialGamePadConfig,
        private val onButtonEvent: ((event: Event) -> Unit)? = null
) {
        private val padConfig: RadialGamePadConfig = padConfig
        val pad = RadialGamePad(padConfig, 0f, context)

        companion object {
                /** Should the user see the on-screen controls? */
                @Suppress("DEPRECATION")
                fun shouldShowGamePads(activity: Activity): Boolean {
                        /* Config says we shouldn't use virtual controls */
                        if (!activity.resources.getBoolean(R.bool.config_gamepad)) return false

                        /* Devices without a touchscreen don't need a GamePad */
                        val hasTouchScreen =
                                activity.packageManager?.hasSystemFeature(
                                        PackageManager.FEATURE_TOUCHSCREEN
                                )
                        if (hasTouchScreen == null || hasTouchScreen == false) return false

                        /* Fetch the current display that the game is running on */
                        val currentDisplayId = activity.display!!.displayId

                        /* Are we presenting this screen on a TV or display? */
                        val dm =
                                activity.getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
                        if (dm.getDisplay(currentDisplayId).flags and Display.FLAG_PRESENTATION ==
                                        Display.FLAG_PRESENTATION
                        )
                                return false

                        /* If a GamePad is connected, we definitely don't need touch controls */
                        for (id in InputDevice.getDeviceIds()) {
                                InputDevice.getDevice(id)?.apply {
                                        if (sources and InputDevice.SOURCE_GAMEPAD ==
                                                        InputDevice.SOURCE_GAMEPAD
                                        )
                                                return false
                                }
                        }

                        return true
                }
        }

        private fun eventHandler(event: Event, retroView: GLRetroView) {
                when (event) {
                        is Event.Button -> {
                                // Always send to ControllerInput for processing (it will decide if
                                // menu is open)
                                onButtonEvent?.invoke(event)
                                // Also send to core if not handled by menu
                                retroView.sendKeyEvent(event.action, event.id)
                        }
                        is Event.Direction -> {
                                // Always send to ControllerInput for processing (it will decide if
                                // menu is open)
                                onButtonEvent?.invoke(event)
                                // Also send to core if not handled by menu
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

        /** Register input events to the RetroView using Flow */
        fun subscribe(lifecycleScope: LifecycleCoroutineScope, retroView: GLRetroView): Job {
                return lifecycleScope.launch {
                        pad.events().collectLatest { event: Event ->
                                eventHandler(event, retroView)
                        }
                }
        }
}
