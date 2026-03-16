package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput

/**
 * ViewModel specialized in input and control management. Responsible for gamepads,
 * virtual controls, and input processing.
 */
class InputViewModel(application: Application) : AndroidViewModel(application) {

    sealed class InputEvent {
        object Idle : InputEvent()
        object HandleSelectStartCombo : InputEvent()
        data class SetupGamePads(val activity: androidx.fragment.app.FragmentActivity, val leftContainer: android.widget.FrameLayout, val rightContainer: android.widget.FrameLayout) : InputEvent()
        object ResetComboAlreadyTriggered : InputEvent()
    }

    private val _eventFlow = MutableStateFlow<InputEvent>(InputEvent.Idle)
    val eventFlow: StateFlow<InputEvent> = _eventFlow.asStateFlow()


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
            _eventFlow.value = InputEvent.HandleSelectStartCombo
            true
        }
            true
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

    fun setupGamePads(
            activity: androidx.fragment.app.FragmentActivity,
            leftContainer: android.widget.FrameLayout,
            rightContainer: android.widget.FrameLayout
    ) {
        _eventFlow.value = InputEvent.SetupGamePads(activity, leftContainer, rightContainer)
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
        _eventFlow.value = InputEvent.ResetComboAlreadyTriggered
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
