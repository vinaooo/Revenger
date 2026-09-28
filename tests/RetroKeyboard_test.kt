package com.vinaooo.revenger.ui.retromenu3

import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertFalse
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
        keyboardView = view
    }

    private lateinit var keyboardView: android.view.View

    private fun tap(id: Int) = keyboardView.findViewById<android.view.View>(id).performClick()

    @Test
    fun `tocar nas teclas digita e apaga`() {
        keyboard.setText("a")

        tap(R.id.key_q)
        tap(R.id.key_7)
        assertEquals("aQ7", retroEditText.getTextContent())

        tap(R.id.key_backspace)
        assertEquals("aQ", retroEditText.getTextContent())
    }

    @Test
    fun `tocar em OK confirma o texto e em cancelar cancela`() {
        keyboard.setText("abc")

        tap(R.id.key_ok)
        tap(R.id.key_cancel)

        assertEquals("abc", confirmedText)
        assertEquals(1, cancelCalls)
    }

    @Test
    fun `navegacao via delegacao move o cursor e getCurrentRow reflete a nova linha`() {
        assertEquals(0, keyboard.getCurrentRow())

        keyboard.navigateDown()

        assertEquals(1, keyboard.getCurrentRow())
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

    @Test
    fun `pressCurrentKey numa tecla de caractere digita o caractere`() {
        keyboard.setText("a")

        keyboard.pressCurrentKey() // row 0, col 0 is "1"

        assertEquals("a1", retroEditText.getTextContent())
    }

    @Test
    fun `pressCurrentKey no backspace apaga o ultimo caractere`() {
        keyboard.setText("abc")
        // Row 3 ends with backspace, after 9 other keys.
        repeat(3) { keyboard.navigateDown() }
        repeat(9) { keyboard.navigateRight() }

        keyboard.pressCurrentKey()

        assertEquals("ab", retroEditText.getTextContent())
    }

    @Test
    fun `pressCurrentKey no cancelar chama onCancel`() {
        repeat(4) { keyboard.navigateDown() }

        keyboard.pressCurrentKey()

        assertEquals(1, cancelCalls)
        assertEquals(null, confirmedText)
    }

    @Test
    fun `tocar numa tecla esconde o destaque e navegar pelo gamepad traz de volta`() {
        val first = keyboardView.findViewById<android.view.View>(R.id.key_1)
        assertTrue(first.isSelected)

        tap(R.id.key_q)
        assertFalse(first.isSelected)
        keyboard.requestFocus()
        assertFalse(first.isSelected)

        keyboard.navigateRight()

        assertTrue(keyboardView.findViewById<android.view.View>(R.id.key_2).isSelected)
    }
}
