package com.vinaooo.revenger.ui.retromenu3

import android.graphics.Canvas
import android.graphics.Typeface
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [RetroEditText] had zero test coverage before this file. Text-buffer mutation and
 * cursor-position tracking now live in [RetroTextCursor] (delegated back onto this class
 * unchanged, covered in isolation by [RetroTextCursor_test]); this is a light integration test
 * confirming the delegation, the `invalidate()` wiring done in `init` (since it can't be a
 * constructor default, see [RetroTextCursor.onChange]), and that the real
 * `retro_rename_keyboard_dlg.xml` layout -- which uses the (Context, AttributeSet, Int)
 * `@JvmOverloads` constructor -- still inflates.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroEditText_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: Fragment

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()
    }

    @Test
    fun `getTextContent e setTextContent delegam para RetroTextCursor`() {
        val editText = RetroEditText(fragment.requireContext())

        editText.setTextContent("Slot 1")

        assertEquals("Slot 1", editText.getTextContent())
    }

    @Test
    fun `insertChar e deleteChar delegam e refletem no conteudo`() {
        val editText = RetroEditText(fragment.requireContext())
        editText.setTextContent("ac")
        editText.moveCursorLeft()

        editText.insertChar("b")
        assertEquals("abc", editText.getTextContent())

        editText.deleteChar()
        assertEquals("ac", editText.getTextContent())
    }

    @Test
    fun `layout real com o construtor de 3 args ainda infla via JvmOverloads`() {
        val view =
                LayoutInflater.from(fragment.requireContext())
                        .inflate(R.layout.retro_rename_keyboard_dlg, null)

        val editText = view.findViewById<RetroEditText>(R.id.rename_edit_text)

        editText.setTextContent("ok")
        assertEquals("ok", editText.getTextContent())
    }

    private fun draw(editText: RetroEditText): Canvas {
        val canvas = mockk<Canvas>(relaxed = true)
        val onDraw = RetroEditText::class.java.getDeclaredMethod("onDraw", Canvas::class.java)
        onDraw.isAccessible = true
        onDraw.invoke(editText, canvas)
        return canvas
    }

    @Test
    fun `usa a cor de texto do menu`() {
        val context = fragment.requireContext()

        val editText = RetroEditText(context)

        assertEquals(ContextCompat.getColor(context, R.color.rm_text_color), editText.currentTextColor)
    }

    @Test
    fun `hint, cor do hint e fonte pedem um redesenho`() {
        val editText = RetroEditText(fragment.requireContext())
        val shadow = shadowOf(editText)

        shadow.clearWasInvalidated()
        editText.setHintText("name")
        assertTrue(shadow.wasInvalidated())

        shadow.clearWasInvalidated()
        editText.setRetroHintColor(0)
        assertTrue(shadow.wasInvalidated())

        shadow.clearWasInvalidated()
        editText.applyTypeface(Typeface.MONOSPACE)
        assertTrue(shadow.wasInvalidated())
    }

    @Test
    fun `vazio com hint desenha o hint e o cursor`() {
        val editText = RetroEditText(fragment.requireContext())
        editText.setHintText("name")

        val canvas = draw(editText)

        verifyOrder {
            canvas.drawText("name", any(), any(), any())
            canvas.drawText("_", any(), any(), any())
        }
        verify(exactly = 2) { canvas.drawText(any<String>(), any(), any(), any()) }
    }

    @Test
    fun `com texto nao desenha o hint`() {
        val editText = RetroEditText(fragment.requireContext())
        editText.setHintText("name")
        editText.setTextContent("ab")

        val canvas = draw(editText)

        verify(exactly = 0) { canvas.drawText("name", any(), any(), any()) }
    }

    @Test
    fun `cursor no meio desenha o texto antes, o cursor depois dele e o resto`() {
        val editText = RetroEditText(fragment.requireContext())
        editText.setTextContent("abc")
        editText.moveCursorLeft()
        val start = editText.paddingStart.toFloat()
        val cursorX = start + editText.paint.measureText("ab")
        val afterX = cursorX + editText.paint.measureText("_")

        val canvas = draw(editText)

        verify(exactly = 1) { canvas.drawText("ab", start, any(), any()) }
        verify(exactly = 1) { canvas.drawText("_", cursorX, any(), any()) }
        verify(exactly = 1) { canvas.drawText("c", afterX, any(), any()) }
        verify(exactly = 3) { canvas.drawText(any<String>(), any(), any(), any()) }
    }

    @Test
    fun `cursor no inicio desenha o cursor e depois o texto todo`() {
        val editText = RetroEditText(fragment.requireContext())
        editText.setTextContent("abc")
        editText.moveCursorToStart()
        val start = editText.paddingStart.toFloat()

        val canvas = draw(editText)

        verify(exactly = 1) { canvas.drawText("_", start, any(), any()) }
        verify(exactly = 1) { canvas.drawText("abc", start + editText.paint.measureText("_"), any(), any()) }
        verify(exactly = 2) { canvas.drawText(any<String>(), any(), any(), any()) }
    }

    @Test
    fun `cursor no fim desenha o texto e o cursor depois dele`() {
        val editText = RetroEditText(fragment.requireContext())
        editText.setTextContent("abc")
        val start = editText.paddingStart.toFloat()

        val canvas = draw(editText)

        verify(exactly = 1) { canvas.drawText("abc", start, any(), any()) }
        verify(exactly = 1) { canvas.drawText("_", start + editText.paint.measureText("abc"), any(), any()) }
        verify(exactly = 2) { canvas.drawText(any<String>(), any(), any(), any()) }
    }
}
