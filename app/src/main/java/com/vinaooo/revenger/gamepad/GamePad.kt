package com.vinaooo.revenger.gamepad

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.InputDevice
import androidx.lifecycle.LifecycleCoroutineScope
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GamePad(
        context: Context,
        padConfig: RadialGamePadConfig,
) {
    val pad = RadialGamePad(padConfig, 0f, context)

    companion object {
        /** Should the user see the on-screen controls? Modernized to avoid deprecated APIs */
        fun shouldShowGamePads(activity: Activity): Boolean {
            /* Config says we shouldn't use virtual controls */
            if (!activity.resources.getBoolean(R.bool.config_gamepad)) return false

            /* Devices without a touchscreen don't need a GamePad */
            val hasTouchScreen =
                    activity.packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            if (hasTouchScreen == null || hasTouchScreen == false) return false

            /* Fetch the current display that the game is running on - fully modernized approach */
            val currentDisplayId =
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            activity.display?.displayId ?: 0
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                            // Use DisplayManager for API 23+
                            val dm =
                                    activity.getSystemService(Service.DISPLAY_SERVICE) as?
                                            DisplayManager
                            dm?.displays?.firstOrNull()?.displayId ?: 0
                        }
                        else -> {
                            // Fallback for older versions - use context display
                            0 // Default display
                        }
                    }

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

    /** Send inputs to the RetroView */
    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> retroView.sendKeyEvent(event.action, event.id)
            is Event.Direction ->
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

    /** Register input events to the RetroView using Flow */
    fun subscribe(lifecycleScope: LifecycleCoroutineScope, retroView: GLRetroView): Job {
        return lifecycleScope.launch {
            pad.events().collect { event: Event -> eventHandler(event, retroView) }
        }
    }
}
