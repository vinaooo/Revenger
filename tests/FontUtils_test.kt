package com.vinaooo.revenger.utils

import android.content.Context
import android.content.res.Resources
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [FontUtils]' capitalization, for each `rm_text_capitalization` style. The build fixes one style
 * in resources, so the others are reached through a context whose resources report them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FontUtils_test {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private fun contextWithStyle(style: Int): Context {
        val resources = mockk<Resources>(relaxed = true)
        every { resources.getInteger(R.integer.rm_text_capitalization) } returns style
        every { resources.getString(R.string.save_success, 3) } returns "sAVED to slot 3"
        every { resources.getString(R.string.save_error) } returns "sAVE failed"
        return mockk<Context>(relaxed = true).also { every { it.resources } returns resources }
    }

    private fun capitalizedText(style: Int, text: String): String {
        val view = TextView(appContext).apply { this.text = text }
        FontUtils.applyTextCapitalization(contextWithStyle(style), view)
        return view.text.toString()
    }

    @Test
    fun `estilo 1 deixa so a primeira letra maiuscula`() {
        assertEquals("Load game", capitalizedText(1, "lOAD GAME"))
        assertEquals("Saved to slot 3", FontUtils.getCapitalizedString(contextWithStyle(1), R.string.save_success, 3))
    }

    @Test
    fun `estilo 2 deixa tudo maiusculo`() {
        assertEquals("LOAD GAME", capitalizedText(2, "lOAD GAME"))
        assertEquals("SAVE FAILED", FontUtils.getCapitalizedString(contextWithStyle(2), R.string.save_error))
    }

    @Test
    fun `outros estilos mantem o texto como esta`() {
        assertEquals("lOAD GAME", capitalizedText(0, "lOAD GAME"))
        assertEquals("sAVE failed", FontUtils.getCapitalizedString(contextWithStyle(0), R.string.save_error))
    }

    @Test
    fun `as variantes com varios TextViews aplicam a cada um`() {
        val context = contextWithStyle(2)
        val a = TextView(appContext).apply { text = "a" }
        val b = TextView(appContext).apply { text = "b" }
        val c = TextView(appContext).apply { text = "c" }

        FontUtils.applyTextCapitalization(context, a, b)
        FontUtils.applyTextCapitalization(context, listOf(c))

        assertEquals(listOf("A", "B", "C"), listOf(a, b, c).map { it.text.toString() })
    }
}
