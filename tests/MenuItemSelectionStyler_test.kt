package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
 * [MenuItemSelectionStyler] was extracted out of [MenuViewManager.updateSelectionVisual] (the
 * private `setItemSelected`/`setItemUnselected` pair), which had no direct coverage before
 * [MenuViewManager_test] exercised it indirectly through `updateSelectionVisual`. This file covers
 * the per-item styling in isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuItemSelectionStyler_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: Fragment
    private lateinit var styler: MenuItemSelectionStyler
    private lateinit var itemView: MenuItemView

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        val context = fragment.requireContext()
        val arrow =
                TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0)
                }
        itemView = MenuItemView(TextView(context), arrow, RetroCardView(context))
        styler = MenuItemSelectionStyler(fragment)
    }

    private fun selectedColor() =
            ContextCompat.getColor(activity, R.color.rm_selected_color)
    private fun normalColor() = ContextCompat.getColor(activity, R.color.rm_normal_color)

    @Test
    fun `markSelected aplica cor selecionada, mostra a seta e marca o card como SELECTED`() {
        styler.markSelected(itemView)

        assertEquals(selectedColor(), itemView.titleTextView.currentTextColor)
        assertEquals(selectedColor(), itemView.arrowTextView.currentTextColor)
        assertEquals(View.VISIBLE, itemView.arrowTextView.visibility)
        assertEquals(RetroCardView.State.SELECTED, itemView.cardView.getState())
    }

    @Test
    fun `markUnselected aplica cor normal, esconde a seta e marca o card como NORMAL`() {
        styler.markSelected(itemView)

        styler.markUnselected(itemView)

        assertEquals(normalColor(), itemView.titleTextView.currentTextColor)
        assertEquals(View.GONE, itemView.arrowTextView.visibility)
        assertEquals(RetroCardView.State.NORMAL, itemView.cardView.getState())
    }
}
