package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput
import java.lang.ref.WeakReference

/**
 * ViewModel specialized in input and control management. Responsible for gamepads,
 * virtual controls, and input processing.
 */
class InputViewModel(application: Application) : AndroidViewModel(application) {


    private val controllerInput = ControllerInput()
    private val appConfig = RevengerApplication.appConfig

    // Held weakly: this ViewModel can outlive the Activity whose view tree owns the container
    // (lint StaticFieldLeak). The attached view tree keeps it alive while the Activity is.
    private var gamePadContainerViewRef: WeakReference<android.widget.LinearLayout>? = null
    private var gamePadContainerView: android.widget.LinearLayout?
        get() = gamePadContainerViewRef?.get()
        set(value) {
            gamePadContainerViewRef = value?.let(::WeakReference)
        }

    // ========== CONFIGURATION METHODS ==========

    fun setGamePadContainer(container: android.widget.LinearLayout) {
        gamePadContainerView = container
    }

    // ========== GAMEPAD METHODS ==========

    fun updateGamePadVisibility(shouldShow: Boolean) {
        val visibility = if (shouldShow) android.view.View.VISIBLE else android.view.View.GONE
        gamePadContainerView?.visibility = visibility
    }

    fun clearControllerInputState() {
        android.util.Log.d(
                "InputViewModel",
                "🔥 [CLEAR_INPUT_STATE] ===== clearControllerInputState() CALLED ====="
        )
        android.util.Log.d(
                "InputViewModel",
                "🔥 [CLEAR_INPUT_STATE] Timestamp: ${System.currentTimeMillis()}"
        )
        controllerInput.comboTracker.clearKeyLog()
        android.util.Log.d(
                "InputViewModel",
                "🔥 [CLEAR_INPUT_STATE] controllerInput.clearKeyLog() completed"
        )
        android.util.Log.d(
                "InputViewModel",
                "🔥 [CLEAR_INPUT_STATE] ===== clearControllerInputState() COMPLETED ====="
        )
        android.util.Log.d(
                "InputViewModel",
                "🔥 [CLEAR_INPUT_STATE] Final Timestamp: ${System.currentTimeMillis()}"
        )
    }

    // ========== GETTERS FOR COMPATIBILITY ==========

    fun getControllerInput(): ControllerInput = controllerInput

    // ========== VISIBILITY CONTROL METHODS ==========

    fun shouldShowGamePads(activity: android.app.Activity): Boolean {
        return GamePad.shouldShowGamePads(activity, appConfig)
    }

    // ========== LIFECYCLE METHODS ==========

    override fun onCleared() {
        super.onCleared()
        // Cleanup if necessary
    }
}
