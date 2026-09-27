package com.vinaooo.revenger.input

import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [KeyEventRouter] (reached through [ControllerInput.processKeyEvent]): the B-button back
 * handling, DPAD menu navigation in every direction, and the menu-open block-all gate. The B
 * handling here differs on purpose from [GamePadButtonRouter]'s; the `pinned` test below keeps
 * this path's current behavior (any non-DOWN action releases B) so a change to either path
 * shows up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class KeyEventRouter_test {

    private lateinit var input: ControllerInput
    private lateinit var retroView: RetroView
    private lateinit var glRetroView: GLRetroView
    private var menuOpen = true
    private val fired = mutableListOf<String>()

    @Before
    fun setUp() {
        input = ControllerInput()
        input.shouldInterceptDpadForMenu = { menuOpen }
        input.menuBackCallback = { fired += "back" }
        input.menuNavigateUpCallback = { fired += "up" }
        input.menuNavigateDownCallback = { fired += "down" }
        input.menuNavigateLeftCallback = { fired += "left" }
        input.menuNavigateRightCallback = { fired += "right" }
        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns MutableLiveData(true)
    }

    private fun press(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN) =
            input.processKeyEvent(keyCode, KeyEvent(action, keyCode), retroView)

    private fun nothingSentToCore() = verify(exactly = 0) { glRetroView.sendKeyEvent(any(), any(), any()) }

    @Test
    fun `B com o menu aberto volta uma vez por toque e nunca chega ao jogo`() {
        assertEquals(true, press(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(true, press(KeyEvent.KEYCODE_BUTTON_B)) // held: auto-repeat DOWN
        assertEquals(true, press(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP))

        assertEquals(listOf("back"), fired)
        assertFalse(input.comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_B))
        nothingSentToCore()
    }

    @Test
    fun `B solto e pressionado de novo volta de novo`() {
        press(KeyEvent.KEYCODE_BUTTON_B)
        press(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP)
        Thread.sleep(MENU_DEBOUNCE_MS)
        press(KeyEvent.KEYCODE_BUTTON_B)

        assertEquals(listOf("back", "back"), fired)
    }

    @Test
    fun `pinned - neste caminho qualquer acao que nao seja DOWN solta o B`() {
        press(KeyEvent.KEYCODE_BUTTON_B)

        assertEquals(true, press(KeyEvent.KEYCODE_BUTTON_B, OTHER_ACTION))

        assertFalse(input.comboTracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_B))
    }

    @Test
    fun `B com o menu fechado vai para o jogo`() {
        menuOpen = false

        assertEquals(true, press(KeyEvent.KEYCODE_BUTTON_B))

        assertTrue(fired.isEmpty())
        verify(exactly = 1) { glRetroView.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_B, 0) }
    }

    @Test
    fun `cada direcao do DPAD navega o menu na direcao certa`() {
        press(KeyEvent.KEYCODE_DPAD_UP)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)

        assertEquals(listOf("up", "down", "left", "right"), fired)
        nothingSentToCore()
    }

    @Test
    fun `soltar o DPAD com o menu aberto e consumido sem navegar`() {
        assertEquals(true, press(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_UP))

        assertTrue(fired.isEmpty())
        nothingSentToCore()
    }

    @Test
    fun `com o bloqueio ligado outros botoes nao chegam ao jogo`() {
        menuOpen = false
        input.shouldBlockAllGamepadInput = { true }

        assertEquals(true, press(KeyEvent.KEYCODE_BUTTON_X))

        nothingSentToCore()
    }

    @Test
    fun `sem bloqueio outros botoes vao para o jogo pela porta 0`() {
        menuOpen = false

        press(KeyEvent.KEYCODE_BUTTON_X)

        verify(exactly = 1) { glRetroView.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_X, 0) }
    }

    private companion object {
        // An action that is neither DOWN nor UP (the value of the deprecated ACTION_MULTIPLE).
        const val OTHER_ACTION = 2

        // One past MenuCallbackDebouncer's 150 ms window, on the real clock it reads.
        const val MENU_DEBOUNCE_MS = 160L
    }
}
