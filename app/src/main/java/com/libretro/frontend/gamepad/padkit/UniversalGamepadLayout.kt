package com.libretro.frontend.gamepad.padkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.controls.ControlAnalog
import gg.padkit.controls.ControlButton
import gg.padkit.controls.ControlCross
import gg.padkit.controls.ControlFaceButtons
import gg.padkit.ids.Id
import gg.padkit.inputstate.InputState
import gg.padkit.layouts.radial.LayoutRadial
import kotlinx.collections.immutable.toPersistentList

/**
 * Layout universal PadKit que se adapta dinamicamente às configurações do config.xml Funciona com
 * qualquer ROM/core sem configurações específicas.
 */
@Composable
fun UniversalGamepadLayout(
        configuration: UniversalConfigManager.GamepadConfiguration,
        onInputEvent: (InputState) -> Unit,
        modifier: Modifier = Modifier
) {
    PadKit(
            hapticFeedbackType =
                    if (configuration.enableHapticFeedback) HapticFeedbackType.PRESS
                    else HapticFeedbackType.NONE,
            onInputStateUpdated = onInputEvent,
            modifier = modifier.fillMaxSize()
    ) {
        Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
        ) {
            // LADO ESQUERDO - Controle de movimento dinâmico
            LeftSideLayout(
                    configuration = configuration,
                    modifier = Modifier.weight(1f).padding(16.dp)
            )

            // LADO DIREITO - Botões de ação dinâmicos
            RightSideLayout(
                    configuration = configuration,
                    modifier = Modifier.weight(1f).padding(16.dp)
            )
        }
    }
}

/** Layout do lado esquerdo - D-Pad ou Analog baseado na configuração */
@Composable
private fun gg.padkit.PadKitScope.LeftSideLayout(
        configuration: UniversalConfigManager.GamepadConfiguration,
        modifier: Modifier = Modifier
) {
    LayoutRadial(
            modifier = modifier,
            primaryDial = {
                // Controle principal baseado na configuração
                when (configuration.layoutPreferences.leftSideControl) {
                    UniversalConfigManager.ControlType.ANALOG -> {
                        ControlAnalog(
                                id = Id.ContinuousDirection(UniversalInputMapper.ANALOG_ID),
                                analogPressId = Id.Key(100), // Para detecção de toque do analog
                                modifier = Modifier.fillMaxSize()
                        )
                    }
                    UniversalConfigManager.ControlType.DPAD -> {
                        ControlCross(
                                id = Id.DiscreteDirection(UniversalInputMapper.DPAD_ID),
                                allowDiagonals = true,
                                modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            },
            secondaryDials = {
                // Botões auxiliares do lado esquerdo
                configuration.layoutPreferences.auxiliaryButtons
                        .filter { (buttonName, _) ->
                            configuration.buttonVisibility[buttonName] == true
                        }
                        .forEach { (buttonName, angle) ->
                            ControlButton(
                                    id = Id.Key(getButtonIdForName(buttonName)),
                                    modifier = Modifier.radialPosition(angle)
                            )
                        }
            }
    )
}

/** Layout do lado direito - Botões de ação principais */
@Composable
private fun gg.padkit.PadKitScope.RightSideLayout(
        configuration: UniversalConfigManager.GamepadConfiguration,
        modifier: Modifier = Modifier
) {
    LayoutRadial(
            modifier = modifier,
            primaryDial = {
                // Botões de ação principais (A, B, X, Y) - sempre dinâmico
                val visibleActionButtons =
                        listOf("button_a", "button_b", "button_x", "button_y")
                                .filter { configuration.buttonVisibility[it] == true }
                                .map { Id.Key(getButtonIdForName(it)) }
                                .toPersistentList()

                if (visibleActionButtons.isNotEmpty()) {
                    ControlFaceButtons(
                            ids = visibleActionButtons,
                            modifier = Modifier.fillMaxSize()
                    )
                }
            },
            secondaryDials = {
                // Botões auxiliares do lado direito
                configuration.layoutPreferences.rightSideButtons
                        .filter { buttonName -> configuration.buttonVisibility[buttonName] == true }
                        .forEachIndexed { index, buttonName ->
                            ControlButton(
                                    id = Id.Key(getButtonIdForName(buttonName)),
                                    modifier = Modifier.radialPosition(60f + (index * 30f))
                            )
                        }
            }
    )
}

/** Obtém ID PadKit para nome de botão - função helper */
private fun getButtonIdForName(buttonName: String): Int {
    return when (buttonName) {
        "button_a" -> UniversalInputMapper.BUTTON_A_ID
        "button_b" -> UniversalInputMapper.BUTTON_B_ID
        "button_x" -> UniversalInputMapper.BUTTON_X_ID
        "button_y" -> UniversalInputMapper.BUTTON_Y_ID
        "button_l1" -> UniversalInputMapper.BUTTON_L1_ID
        "button_r1" -> UniversalInputMapper.BUTTON_R1_ID
        "button_l2" -> UniversalInputMapper.BUTTON_L2_ID
        "button_r2" -> UniversalInputMapper.BUTTON_R2_ID
        "button_start" -> UniversalInputMapper.BUTTON_START_ID
        "button_select" -> UniversalInputMapper.BUTTON_SELECT_ID
        else -> 999 // ID padrão para botões não mapeados
    }
}
