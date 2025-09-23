package com.vinaooo.revenger.gamepad

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
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
) {
    val pad = RadialGamePad(padConfig, 0f, context)
    
    // Callback for button events (will be used to notify ViewModel)
    private var buttonEventCallback: ((Int, Int) -> Unit)? = null
    
    /** Set the button event callback function */
    fun setButtonEventCallback(callback: (Int, Int) -> Unit) {
        android.util.Log.d("GamePad", "Setting button event callback")
        buttonEventCallback = callback
    }    companion object {
        /** Should the user see the on-screen controls? */
        @Suppress("DEPRECATION")
        fun shouldShowGamePads(activity: Activity): Boolean {
            /* Config says we shouldn't use virtual controls */
            if (!activity.resources.getBoolean(R.bool.config_gamepad)) return false

            /* Devices without a touchscreen don't need a GamePad */
            val hasTouchScreen =
                    activity.packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            if (hasTouchScreen == null || hasTouchScreen == false) return false

            /* Fetch the current display that the game is running on */
            val currentDisplayId =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display!!.displayId
                    else {
                        val wm =
                                activity.getSystemService(AppCompatActivity.WINDOW_SERVICE) as
                                        WindowManager
                        wm.defaultDisplay.displayId
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
            is Event.Button -> {
                // Log para debugging
                android.util.Log.d(
                        "GamePad",
                        "Button event - ID: ${event.id}, Action: ${event.action}"
                )

                // Notificar o ViewModel sobre o evento do botão
                buttonEventCallback?.invoke(event.id, event.action)

                // Enviar evento para o RetroView
                retroView.sendKeyEvent(event.action, event.id)
            }
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
            pad.events().collectLatest { event: Event -> eventHandler(event, retroView) }
        }
    }
}
