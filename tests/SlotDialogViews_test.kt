package com.vinaooo.revenger.ui.retromenu3

import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SlotDialogViews]' tree walk and selection highlight, on plain views. The dialogs built from the
 * real layouts are covered through [SlotDialogController_test] and the grid fragment tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SlotDialogViews_test {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `textViewsIn acha o proprio TextView e os que estao aninhados`() {
        val inner = TextView(context)
        val nested = LinearLayout(context).apply { addView(inner) }
        val first = TextView(context)
        val root = FrameLayout(context).apply {
            addView(first)
            addView(nested)
        }

        assertEquals(listOf(first, inner), SlotDialogViews.textViewsIn(root))
        assertEquals(listOf(first), SlotDialogViews.textViewsIn(first))
    }

    @Test
    fun `styleTexts sem textos nao faz nada`() {
        SlotDialogViews.styleTexts(emptyList())
    }

    @Test
    fun `markSelection ignora botoes sem seta e rotulo conhecidos`() {
        val unknown = RetroCardView(context).apply { id = android.view.View.generateViewId() }

        SlotDialogViews.markSelection(listOf(unknown), selectedIndex = 0)

        assertEquals(RetroCardView.State.NORMAL, unknown.getState())
    }
}
