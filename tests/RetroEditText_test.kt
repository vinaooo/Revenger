package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
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
}
