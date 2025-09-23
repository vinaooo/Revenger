package com.libretro.frontend.gamepad.padkit

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.swordfish.libretrodroid.GLRetroView
import gg.padkit.ids.Id
import gg.padkit.inputstate.InputState

/**
 * Sistema principal PadKit que integra ConfigManager e InputMapper, fornecendo interface única para
 * qualquer gamepad universal.
 */
class PadKitGamepadSystem(private val context: Context) {

    private val configManager = UniversalConfigManager(context)
    private val inputMapper = UniversalInputMapper()

    /** Interface para callback de inputs do gamepad */
    interface GamepadInputListener {
        fun onInputReceived()
        fun onMenuRequested()
    }

    /** Cria gamepad universal PadKit configurado via config.xml */
    @Composable
    fun CreateUniversalGamepad(
            retroView: GLRetroView,
            inputListener: GamepadInputListener? = null,
            port: Int = UniversalInputMapper.DEFAULT_PORT,
            modifier: Modifier = Modifier
    ) {
        // Carregar configuração do config.xml
        val configuration = remember { configManager.loadConfiguration() }

        // Layout universal PadKit real
        UniversalGamepadLayout(
                configuration = configuration,
                onInputEvent = { inputState ->
                    // Mapear inputs do PadKit para LibretroDroid
                    inputMapper.mapToLibretro(inputState, configuration, retroView, port)

                    // Notificar listener
                    inputListener?.onInputReceived()

                    // Verificar combinação de menu (Start + Select)
                    checkMenuCombo(inputState, inputListener)
                },
                modifier = modifier
        )
    }

    /** Verifica se o usuário pressionou a combinação de menu */
    private fun checkMenuCombo(inputState: InputState, listener: GamepadInputListener?) {
        val startPressed = inputState.getDigitalKey(Id.Key(UniversalInputMapper.BUTTON_START_ID))
        val selectPressed = inputState.getDigitalKey(Id.Key(UniversalInputMapper.BUTTON_SELECT_ID))

        if (startPressed && selectPressed) {
            listener?.onMenuRequested()
        }
    }

    /** Obtém configuração atual do gamepad */
    fun getCurrentConfiguration(): UniversalConfigManager.GamepadConfiguration {
        return configManager.loadConfiguration()
    }

    /** Recarrega configuração (útil se config.xml mudou) */
    fun reloadConfiguration(): UniversalConfigManager.GamepadConfiguration {
        inputMapper.reset()
        return configManager.loadConfiguration()
    }

    /** Reseta estado do sistema (útil para reiniciar jogo) */
    fun reset() {
        inputMapper.reset()
    }
}
