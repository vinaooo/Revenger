package com.libretro.frontend.gamepad.padkit

import android.util.Log
import android.view.KeyEvent
import com.swordfish.libretrodroid.GLRetroView
import gg.padkit.ids.Id
import gg.padkit.inputstate.InputState

/**
 * Mapeador universal que converte InputState do PadKit para comandos do LibretroDroid sem
 * dependência de ROM específica.
 */
class UniversalInputMapper {

    companion object {
        private const val TAG = "UniversalInputMapper"
        
        // Constantes de IDs do PadKit
        const val DPAD_ID = 0
        const val ANALOG_ID = 0
        const val BUTTON_A_ID = 0
        const val BUTTON_B_ID = 1
        const val BUTTON_X_ID = 2
        const val BUTTON_Y_ID = 3
        const val BUTTON_L1_ID = 10
        const val BUTTON_R1_ID = 11
        const val BUTTON_L2_ID = 12
        const val BUTTON_R2_ID = 13
        const val BUTTON_START_ID = 20
        const val BUTTON_SELECT_ID = 21

        // Port padrão para LibretroDroid (jogador 1 = port 0)
        const val DEFAULT_PORT = 0
    }

    /** Estado anterior dos inputs para detectar mudanças */
    private var previousInputState: InputState? = null

    /** Mapeia InputState do PadKit para comandos do LibretroDroid */
    fun mapToLibretro(
            inputState: InputState,
            configuration: UniversalConfigManager.GamepadConfiguration,
            retroView: GLRetroView,
            port: Int = DEFAULT_PORT
    ) {
        val previous = previousInputState

        // Mapear controles de movimento
        mapMovementControls(inputState, configuration, retroView, port, previous)

        // Mapear botões digitais
        mapDigitalButtons(inputState, configuration, retroView, port, previous)

        // Atualizar estado anterior
        previousInputState = inputState
    }

    /** Mapeia controles de movimento (D-Pad ou Analog) */
    private fun mapMovementControls(
            inputState: InputState,
            configuration: UniversalConfigManager.GamepadConfiguration,
            retroView: GLRetroView,
            port: Int,
            previous: InputState?
    ) {
        Log.d(TAG, "mapMovementControls: useAnalogStick = ${configuration.useAnalogStick}")
        
        if (configuration.useAnalogStick) {
            // Usar joystick analógico
            val analogInput = inputState.getContinuousDirection(Id.ContinuousDirection(ANALOG_ID))
            val previousAnalog = previous?.getContinuousDirection(Id.ContinuousDirection(ANALOG_ID))

            Log.d(TAG, "Analog input: x=${analogInput.x}, y=${analogInput.y}")

            // Filtrar valores NaN ou inválidos
            if (analogInput.x.isNaN() || analogInput.y.isNaN()) {
                Log.w(TAG, "Skipping NaN analog input values")
                return
            }

            // Enviar apenas se mudou
            if (previousAnalog == null || analogInput != previousAnalog) {
                Log.d(TAG, "Sending analog motion event to LibretroDroid: x=${analogInput.x}, y=${-analogInput.y}")
                val result = retroView.sendMotionEvent(
                        GLRetroView.MOTION_SOURCE_DPAD, // Tentar com DPAD source para compatibilidade
                        analogInput.x,
                        -analogInput.y, // Inverter Y para LibretroDroid
                        port
                )
                Log.d(TAG, "LibretroDroid sendMotionEvent result: $result")
            }
        } else {
            // Usar D-Pad digital
            val dpadInput = inputState.getDiscreteDirection(Id.DiscreteDirection(DPAD_ID))
            val previousDpad = previous?.getDiscreteDirection(Id.DiscreteDirection(DPAD_ID))

            // Enviar apenas se mudou
            if (previousDpad == null || dpadInput != previousDpad) {
                retroView.sendMotionEvent(
                        GLRetroView.MOTION_SOURCE_DPAD,
                        dpadInput.x,
                        -dpadInput.y, // Inverter Y para LibretroDroid
                        port
                )
            }
        }
    }

    /** Mapeia botões digitais baseado na configuração */
    private fun mapDigitalButtons(
            inputState: InputState,
            configuration: UniversalConfigManager.GamepadConfiguration,
            retroView: GLRetroView,
            port: Int,
            previous: InputState?
    ) {
        // Lista de mapeamentos botão -> KeyEvent
        val buttonMappings =
                listOf(
                        Triple("button_a", BUTTON_A_ID, KeyEvent.KEYCODE_BUTTON_A),
                        Triple("button_b", BUTTON_B_ID, KeyEvent.KEYCODE_BUTTON_B),
                        Triple("button_x", BUTTON_X_ID, KeyEvent.KEYCODE_BUTTON_X),
                        Triple("button_y", BUTTON_Y_ID, KeyEvent.KEYCODE_BUTTON_Y),
                        Triple("button_l1", BUTTON_L1_ID, KeyEvent.KEYCODE_BUTTON_L1),
                        Triple("button_r1", BUTTON_R1_ID, KeyEvent.KEYCODE_BUTTON_R1),
                        Triple("button_l2", BUTTON_L2_ID, KeyEvent.KEYCODE_BUTTON_L2),
                        Triple("button_r2", BUTTON_R2_ID, KeyEvent.KEYCODE_BUTTON_R2),
                        Triple("button_start", BUTTON_START_ID, KeyEvent.KEYCODE_BUTTON_START),
                        Triple("button_select", BUTTON_SELECT_ID, KeyEvent.KEYCODE_BUTTON_SELECT)
                )

        // Processar cada botão configurado
        buttonMappings.forEach { (buttonName, padkitId, keyCode) ->
            if (configuration.buttonVisibility[buttonName] == true) {
                val currentPressed = inputState.getDigitalKey(Id.Key(padkitId))
                val previousPressed = previous?.getDigitalKey(Id.Key(padkitId)) ?: false

                // Enviar evento apenas se o estado mudou
                if (currentPressed != previousPressed) {
                    val action = if (currentPressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
                    retroView.sendKeyEvent(action, keyCode, port)
                }
            }
        }
    }

    /** Limpa estado interno (útil para reiniciar) */
    fun reset() {
        previousInputState = null
    }

    /** Obtém ID PadKit para um nome de botão */
    fun getButtonId(buttonName: String): Int {
        return when (buttonName) {
            "button_a" -> BUTTON_A_ID
            "button_b" -> BUTTON_B_ID
            "button_x" -> BUTTON_X_ID
            "button_y" -> BUTTON_Y_ID
            "button_l1" -> BUTTON_L1_ID
            "button_r1" -> BUTTON_R1_ID
            "button_l2" -> BUTTON_L2_ID
            "button_r2" -> BUTTON_R2_ID
            "button_start" -> BUTTON_START_ID
            "button_select" -> BUTTON_SELECT_ID
            else -> 999 // ID padrão para botões não mapeados
        }
    }

    /** Obtém KeyCode do Android para um nome de botão */
    fun getKeyCode(buttonName: String): Int {
        return when (buttonName) {
            "button_a" -> KeyEvent.KEYCODE_BUTTON_A
            "button_b" -> KeyEvent.KEYCODE_BUTTON_B
            "button_x" -> KeyEvent.KEYCODE_BUTTON_X
            "button_y" -> KeyEvent.KEYCODE_BUTTON_Y
            "button_l1" -> KeyEvent.KEYCODE_BUTTON_L1
            "button_r1" -> KeyEvent.KEYCODE_BUTTON_R1
            "button_l2" -> KeyEvent.KEYCODE_BUTTON_L2
            "button_r2" -> KeyEvent.KEYCODE_BUTTON_R2
            "button_start" -> KeyEvent.KEYCODE_BUTTON_START
            "button_select" -> KeyEvent.KEYCODE_BUTTON_SELECT
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
}
