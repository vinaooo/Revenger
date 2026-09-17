package com.vinaooo.revenger.ui.retromenu3

import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RetroKeyboard] had zero test coverage before this file. Grid navigation
 * (`navigateUp`/`Down`/`Left`/`Right`) and the selection-mode/highlight bookkeeping now live in
 * [KeyboardNavigationController] (delegated back onto this class unchanged, covered in isolation
 * by [KeyboardNavigationController_test]); this is a light integration test confirming the
 * delegation and the remaining character-entry/confirm/cancel behavior on the real
 * `retro_keyboard.xml` layout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroKeyboard_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: Fragment
    private lateinit var retroEditText: RetroEditText
    private var confirmedText: String? = null
    private var cancelCalls = 0
    private lateinit var keyboard: RetroKeyboard

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        retroEditText = RetroEditText(fragment.requireContext())
        keyboard =
                RetroKeyboard(
                        context = fragment.requireContext(),
                        retroEditText = retroEditText,
                        onConfirm = { confirmedText = it },
                        onCancel = { cancelCalls++ }
                )
        val view =
                LayoutInflater.from(fragment.requireContext())
                        .inflate(R.layout.retro_keyboard, null)
        keyboard.setupKeyboardInView(view)
    }

    @Test
    fun `navegacao via delegacao move o cursor e getCurrentRow reflete a nova linha`() {
        assertEquals(0, keyboard.getCurrentRow())

        keyboard.navigateDown()

        assertEquals(1, keyboard.getCurrentRow())
    }

    @Test
    fun `handleDpadEvent com DPAD_DOWN delega para navigateDown`() {
        val handled = keyboard.handleDpadEvent(KeyEvent.KEYCODE_DPAD_DOWN)

        assertTrue(handled)
        assertEquals(1, keyboard.getCurrentRow())
    }

    @Test
    fun `handleDpadEvent com BUTTON_A pressiona a tecla atualmente selecionada`() {
        // Row 0, col 0 is "1" on the real layout.
        keyboard.handleDpadEvent(KeyEvent.KEYCODE_BUTTON_A)

        assertEquals("1", retroEditText.getTextContent())
    }

    @Test
    fun `handleDpadEvent com BUTTON_B chama onCancel`() {
        keyboard.handleDpadEvent(KeyEvent.KEYCODE_BUTTON_B)

        assertEquals(1, cancelCalls)
    }

    @Test
    fun `setText define o conteudo inicial do RetroEditText`() {
        keyboard.setText("Slot 1")

        assertEquals("Slot 1", retroEditText.getTextContent())
    }

    @Test
    fun `requestFocus reseta a navegacao para a primeira linha`() {
        keyboard.navigateDown()

        keyboard.requestFocus()

        assertEquals(0, keyboard.getCurrentRow())
    }

    @Test
    fun `pressCurrentKey no botao OK chama onConfirm com o texto atual`() {
        keyboard.setText("abc")
        // Navigate to the OK key: row 4 is [CANCEL, SPACE, OK].
        repeat(4) { keyboard.navigateDown() }
        repeat(2) { keyboard.navigateRight() }

        keyboard.pressCurrentKey()

        assertEquals("abc", confirmedText)
    }
}
