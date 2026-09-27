package com.vinaooo.revenger.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [GamePadButtonRouter] (reached through [ControllerInput.processGamePadButtonEvent], the
 * on-screen and physical gamepad buttons): B-button back handling, START and the gamepad menu
 * button releasing, and repeated presses. `process` returns true when the event is kept from the
 * core. The B handling differs on purpose from [KeyEventRouter]'s; the `pinned` test keeps this
 * path's behavior (only UP releases B).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GamePadButtonRouter_test {

    private lateinit var input: ControllerInput
    private var menuOpen = true
    private val fired = mutableListOf<String>()

    @Before
    fun setUp() {
        input = ControllerInput()
        input.shouldInterceptDpadForMenu = { menuOpen }
        input.menuBackCallback = { fired += "back" }
        input.startButtonCallback = { fired += "start" }
        input.gamepadMenuButtonCallback = { fired += "menu" }
    }

    private fun press(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN) =
            input.processGamePadButtonEvent(keyCode, action)

    private val keyLog get() = input.comboTracker.keyLog

    @Test
    fun `B com o menu aberto volta uma vez por toque e nunca chega ao jogo`() {
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_B))
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_B)) // held
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP))

        assertEquals(listOf("back"), fired)
        assertFalse(keyLog.contains(KeyEvent.KEYCODE_BUTTON_B))
    }

    @Test
    fun `B solto sem ter sido pressionado e consumido sem voltar`() {
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP))

        assertTrue(fired.isEmpty())
    }

    @Test
    fun `pinned - neste caminho so o UP solta o B`() {
        press(KeyEvent.KEYCODE_BUTTON_B)

        assertTrue(press(KeyEvent.KEYCODE_BUTTON_B, OTHER_ACTION))

        assertTrue(keyLog.contains(KeyEvent.KEYCODE_BUTTON_B))
    }

    @Test
    fun `B com o menu fechado vai para o jogo`() {
        menuOpen = false

        assertFalse(press(KeyEvent.KEYCODE_BUTTON_B))

        assertTrue(fired.isEmpty())
    }

    @Test
    fun `soltar START com o menu aberto e consumido sem fechar o menu de novo`() {
        input.shouldHandleStartButton = { true }

        assertTrue(press(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.ACTION_UP))

        assertTrue(fired.isEmpty())
    }

    @Test
    fun `soltar o botao de menu do controle e consumido sem abrir o menu de novo`() {
        input.shouldHandleGamepadMenuButton = { true }

        assertTrue(press(GAMEPAD_MENU_BUTTON, KeyEvent.ACTION_UP))

        assertTrue(fired.isEmpty())
    }

    @Test
    fun `um botao repetido sem soltar nao chega de novo ao jogo`() {
        menuOpen = false

        assertFalse(press(KeyEvent.KEYCODE_BUTTON_X))
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_X))
        assertFalse(press(KeyEvent.KEYCODE_BUTTON_X, KeyEvent.ACTION_UP))
        assertFalse(press(KeyEvent.KEYCODE_BUTTON_X))
    }

    private companion object {
        // An action that is neither DOWN nor UP (the value of the deprecated ACTION_MULTIPLE).
        const val OTHER_ACTION = 2

        // Vendor keycode some physical gamepads send for their menu button.
        const val GAMEPAD_MENU_BUTTON = -6
    }
}
