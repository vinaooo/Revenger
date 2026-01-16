package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput

/**
 * ViewModel especializado para gerenciamento de input e controles. Responsável por gamepads,
 * controles virtuais e processamento de input.
 */
class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val controllerInput = ControllerInput(application.applicationContext)

    // Referências aos gamepads
    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null
    private var gamePadContainerView: android.widget.LinearLayout? = null

    // Callbacks do ControllerInput
    private var selectStartComboCallback: (() -> Unit)? = null

    init {
        setupControllerInputCallbacks()
    }

    private fun setupControllerInputCallbacks() {
        controllerInput.shouldHandleSelectStartCombo = {
            true
        } // TODO: Implementar lógica condicional
        controllerInput.selectStartComboCallback = { selectStartComboCallback?.invoke() }
    }

    // ========== MÉTODOS DE CONFIGURAÇÃO ==========

    fun setGamePadContainer(container: android.widget.LinearLayout) {
        gamePadContainerView = container
    }

    fun setSelectStartComboCallback(callback: () -> Unit) {
        selectStartComboCallback = callback
    }

    // ========== MÉTODOS DE GAMEPAD ==========

    @Suppress("UNUSED_PARAMETER")
    fun setupGamePads(
            activity: androidx.fragment.app.FragmentActivity,
            leftContainer: android.widget.FrameLayout,
            rightContainer: android.widget.FrameLayout,
            onGamePadCreated: (GamePad, Boolean) -> Unit
    ) {
        // TODO: Implementar configuração dos gamepads
        // - Criar gamepads esquerdo e direito
        // - Configurar layouts
        // - Aplicar configurações de preferências
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
        // TODO: Implementar resetComboAlreadyTriggered se necessário
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

    // ========== GETTERS PARA COMPATIBILIDADE ==========

    fun getLeftGamePad(): GamePad? = leftGamePad

    fun getRightGamePad(): GamePad? = rightGamePad

    fun getControllerInput(): ControllerInput = controllerInput

    // ========== MÉTODOS DE CONTROLE DE VISIBILIDADE ==========

    fun shouldShowGamePads(activity: android.app.Activity): Boolean {
        return GamePad.shouldShowGamePads(activity)
    }

    // ========== MÉTODOS DE LIFECYCLE ==========

    override fun onCleared() {
        super.onCleared()
        // Cleanup se necessário
    }
}
