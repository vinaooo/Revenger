package com.vinaooo.revenger.controllers

import android.hardware.input.InputManager

/**
 * Watches input devices (controllers) being connected, disconnected or changed, and calls
 * [onDevicesChanged] for every one of those events. Extracted out of `GameActivity`, which used to
 * register an anonymous [InputManager.InputDeviceListener] and never unregister it: since
 * [InputManager] is process-wide, every destroyed `GameActivity` stayed reachable through that
 * listener and kept receiving device events.
 *
 * Call [register] once from `GameActivity.onCreate()` and [dispose] from `onDestroy()`.
 */
class InputDeviceWatcher(
        private val inputManager: InputManager,
        private val onDevicesChanged: () -> Unit
) {
        private var listener: InputManager.InputDeviceListener? = null

        /** Starts listening. Calling it again while already registered does nothing. */
        fun register() {
                if (listener != null) return
                val newListener =
                        object : InputManager.InputDeviceListener {
                                override fun onInputDeviceAdded(deviceId: Int) = onDevicesChanged()
                                override fun onInputDeviceRemoved(deviceId: Int) =
                                        onDevicesChanged()
                                override fun onInputDeviceChanged(deviceId: Int) =
                                        onDevicesChanged()
                        }
                inputManager.registerInputDeviceListener(newListener, null)
                listener = newListener
        }

        /** Stops listening and releases the callback. Safe to call more than once. */
        fun dispose() {
                listener?.let { inputManager.unregisterInputDeviceListener(it) }
                listener = null
        }
}
