package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput

/**
 * ViewModel specialized in input and control management. Responsible for gamepads,
 * virtual controls, and input processing.
 */
class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val controllerInput = ControllerInput(application.applicationContext)
    private val appConfig = RevengerApplication.appConfig

    // References to gamepads
    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null
    private var gamePadContainerView: android.widget.LinearLayout? = null

    // ControllerInput callbacks
    private var selectStartComboCallback: (() -> Unit)? = null

    init {
        setupControllerInputCallbacks()
    }

    private fun setupControllerInputCallbacks() {
        controllerInput.shouldHandleSelectStartCombo = {
            true
        } // TODO: Implement conditional logic
        controllerInput.selectStartComboCallback = { selectStartComboCallback?.invoke() }
    }

    // ========== CONFIGURATION METHODS ==========

    fun setGamePadContainer(container: android.widget.LinearLayout) {
        gamePadContainerView = container
    }

    fun setSelectStartComboCallback(callback: () -> Unit) {
        selectStartComboCallback = callback
    }

    // ========== GAMEPAD METHODS ==========

    @Suppress("UNUSED_PARAMETER")
    fun setupGamePads(
            activity: androidx.fragment.app.FragmentActivity,
            leftContainer: android.widget.FrameLayout,
            rightContainer: android.widget.FrameLayout,
            onGamePadCreated: (GamePad, Boolean) -> Unit
    ) {
        // TODO: Implement gamepad setup
        // - Create left and right gamepads
        // - Configure layouts
        // - Apply preference settings
    }

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
        controllerInput.clearKeyLog()
        android.util.Log.d(
                "InputViewModel",
                "🔥 [CLEAR_INPUT_STATE] controllerInput.clearKeyLog() completed"
        )
        // TODO: Implement resetComboAlreadyTriggered if necessary
        // controllerInput.resetComboAlreadyTriggered()
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

    fun getLeftGamePad(): GamePad? = leftGamePad

    fun getRightGamePad(): GamePad? = rightGamePad

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
