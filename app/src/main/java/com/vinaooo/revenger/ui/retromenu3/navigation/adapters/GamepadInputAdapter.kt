package com.vinaooo.revenger.ui.retromenu3.navigation.adapters

import android.view.InputDevice
import android.view.KeyEvent
import com.vinaooo.revenger.ui.retromenu3.navigation.Direction
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.InputTranslator
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent

/**
 * Adaptador para entrada de gamepad (físico e emulado).
 *
 * Traduz eventos de gamepad (KeyEvents com keycodes de gamepad) em NavigationEvents unificados.
 *
 * Suporta:
 * - Gamepad físico (Bluetooth, USB)
 * - Gamepad emulado (botões virtuais na tela via RadialGamePad)
 *
 * Mapeamento:
 * - DPAD_UP → Navigate(UP)
 * - DPAD_DOWN → Navigate(DOWN)
 * - BUTTON_A → ActivateSelected
 * - BUTTON_B → NavigateBack
 * - BUTTON_START → OpenMenu (quando em jogo)
 */
class GamepadInputAdapter : InputTranslator {

    override fun translate(input: Any): List<NavigationEvent> {
        if (input !is KeyEvent) {
            return emptyList()
        }

        // Só processa ACTION_DOWN (não repete em ACTION_UP)
        if (input.action != KeyEvent.ACTION_DOWN) {
            return emptyList()
        }

        // Detecta se é gamepad físico ou emulado baseado no source
        val inputSource =
                if (isPhysicalGamepad(input)) {
                    InputSource.PHYSICAL_GAMEPAD
                } else {
                    InputSource.EMULATED_GAMEPAD
                }

        val event =
                when (input.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        NavigationEvent.Navigate(
                                direction = Direction.UP,
                                inputSource = inputSource
                        )
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        NavigationEvent.Navigate(
                                direction = Direction.DOWN,
                                inputSource = inputSource
                        )
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        NavigationEvent.Navigate(
                                direction = Direction.LEFT,
                                inputSource = inputSource
                        )
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        NavigationEvent.Navigate(
                                direction = Direction.RIGHT,
                                inputSource = inputSource
                        )
                    }
                    KeyEvent.KEYCODE_BUTTON_A,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_CENTER -> {
                        NavigationEvent.ActivateSelected(inputSource = inputSource)
                    }
                    KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> {
                        NavigationEvent.NavigateBack(inputSource = inputSource)
                    }
                    KeyEvent.KEYCODE_BUTTON_START -> {
                        NavigationEvent.OpenMenu(inputSource = inputSource)
                    }
                    else -> null
                }

        return if (event != null) listOf(event) else emptyList()
    }

    /**
     * Detecta se o KeyEvent vem de um gamepad físico.
     *
     * Gamepad físico tem source = SOURCE_GAMEPAD ou SOURCE_JOYSTICK Gamepad emulado vem com source
     * = SOURCE_KEYBOARD ou SOURCE_UNKNOWN
     */
    private fun isPhysicalGamepad(event: KeyEvent): Boolean {
        val source = event.source
        return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }
}
