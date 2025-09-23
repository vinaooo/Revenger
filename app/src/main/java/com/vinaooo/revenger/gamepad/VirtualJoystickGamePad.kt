package com.vinaooo.revenger.gamepad

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wrapper para implementação customizada de joystick virtual Substitui o RadialGamePad mantendo
 * compatibilidade da API
 * @author vinaooo
 * @date 22 de Setembro de 2025
 */
class VirtualJoystickGamePad(
        private val context: Context,
        private val lifecycleOwner: LifecycleOwner
) {

    companion object {
        private const val TAG = "VirtualJoystickGamePad"
    }

    // StateFlows para manter compatibilidade com a implementação anterior
    private val _events = MutableStateFlow<GamePadEvent?>(null)
    val events: StateFlow<GamePadEvent?> = _events.asStateFlow()

    private val _visibility = MutableStateFlow(true)
    val visibility: StateFlow<Boolean> = _visibility.asStateFlow()

    // Views personalizadas do joystick usando CustomJoystickView
    private var leftJoystickView: CustomJoystickView? = null
    private var rightJoystickView: CustomJoystickView? = null

    // Configuração atual
    private var config: VirtualJoystickConfig = VirtualJoystickConfig.defaultConfig()

    /** Configura o gamepad com as configurações fornecidas */
    fun configure(newConfig: VirtualJoystickConfig) {
        this.config = newConfig

        // Aplica a configuração às views existentes
        leftJoystickView?.applyConfig(newConfig)
        rightJoystickView?.applyConfig(newConfig)

        Log.d(TAG, "Configuração aplicada: ${config.name}")
    }

    /** Conecta o gamepad às views customizadas do joystick */
    fun attachToJoysticks(leftJoystick: CustomJoystickView?, rightJoystick: CustomJoystickView?) {
        this.leftJoystickView = leftJoystick
        this.rightJoystickView = rightJoystick

        setupJoystickListeners()
        Log.d(TAG, "CustomJoystickViews conectadas")
    }

    /** Configura os listeners dos joysticks customizados */
    private fun setupJoystickListeners() {
        // Configura listener do joystick esquerdo
        leftJoystickView?.setOnMoveListener(
                object : CustomJoystickView.OnMoveListener {
                    override fun onMove(angle: Int, strength: Int, xAxis: Float, yAxis: Float) {
                        lifecycleOwner.lifecycleScope.launch {
                            val event =
                                    GamePadEvent.AnalogStickMove(
                                            stick = AnalogStick.LEFT,
                                            xAxis = xAxis * config.sensitivity,
                                            yAxis = yAxis * config.sensitivity
                                    )
                            _events.value = event
                            Log.v(
                                    TAG,
                                    "Left stick: angle=$angle, strength=$strength, x=$xAxis, y=$yAxis"
                            )
                        }
                    }
                }
        )

        // Configura listener do joystick direito
        rightJoystickView?.setOnMoveListener(
                object : CustomJoystickView.OnMoveListener {
                    override fun onMove(angle: Int, strength: Int, xAxis: Float, yAxis: Float) {
                        lifecycleOwner.lifecycleScope.launch {
                            val event =
                                    GamePadEvent.AnalogStickMove(
                                            stick = AnalogStick.RIGHT,
                                            xAxis = xAxis * config.sensitivity,
                                            yAxis = yAxis * config.sensitivity
                                    )
                            _events.value = event
                            Log.v(
                                    TAG,
                                    "Right stick: angle=$angle, strength=$strength, x=$xAxis, y=$yAxis"
                            )
                        }
                    }
                }
        )

        Log.d(TAG, "Listeners dos joysticks customizados configurados")
    }

    /** Simula evento de movimento do joystick (para testes) */
    fun simulateJoystickMove(stick: AnalogStick, xAxis: Float, yAxis: Float) {
        lifecycleOwner.lifecycleScope.launch {
            val event = GamePadEvent.AnalogStickMove(stick = stick, xAxis = xAxis, yAxis = yAxis)
            _events.value = event
            Log.d(TAG, "Movimento simulado: $stick X=$xAxis Y=$yAxis")
        }
    }

    /** Define a visibilidade do gamepad */
    fun setVisibility(visible: Boolean) {
        _visibility.value = visible
        val visibility = if (visible) View.VISIBLE else View.GONE

        leftJoystickView?.visibility = visibility
        rightJoystickView?.visibility = visibility

        Log.d(TAG, "Visibilidade alterada para: $visible")
    }

    /** Limpa os recursos */
    fun cleanup() {
        leftJoystickView = null
        rightJoystickView = null
        _events.value = null
        Log.d(TAG, "Recursos limpos")
    }
}

/** Enums e classes de eventos para manter compatibilidade */
enum class AnalogStick {
    LEFT,
    RIGHT
}

sealed class GamePadEvent {
    data class AnalogStickMove(val stick: AnalogStick, val xAxis: Float, val yAxis: Float) :
            GamePadEvent()

    data class ButtonPress(val button: String, val pressed: Boolean) : GamePadEvent()
}
