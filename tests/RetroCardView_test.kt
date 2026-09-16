package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [RetroCardView], the custom `LinearLayout` subclass that replaces MaterialCardView
 * for RetroMenu3 menu items. Had zero test coverage before this file. No other custom `View`
 * test exists in this codebase to mirror, so this follows a plain Robolectric
 * construction/state-mutation pattern.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetroCardView_test {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun backgroundColorOf(view: RetroCardView): Int =
            (view.background as ColorDrawable).color

    @Test
    fun `construcao basica define estado inicial NORMAL, clicavel, e useBackgroundColor true`() {
        val view = RetroCardView(context)

        assertEquals(RetroCardView.State.NORMAL, view.getState())
        assertTrue(view.isClickable)
        assertTrue(view.getUseBackgroundColor())
        assertEquals(Color.TRANSPARENT, backgroundColorOf(view))
    }

    @Test
    fun `setState para SELECTED com useBackgroundColor true aplica cor amarela`() {
        val view = RetroCardView(context)

        view.setState(RetroCardView.State.SELECTED)

        assertEquals(RetroCardView.State.SELECTED, view.getState())
        assertEquals(RetroCardView.COLOR_SELECTED, backgroundColorOf(view))
    }

    @Test
    fun `setState para SELECTED com useBackgroundColor false mantem fundo transparente`() {
        val view = RetroCardView(context)
        view.setUseBackgroundColor(false)

        view.setState(RetroCardView.State.SELECTED)

        assertEquals(RetroCardView.State.SELECTED, view.getState())
        assertEquals(Color.TRANSPARENT, backgroundColorOf(view))
    }

    @Test
    fun `setState para PRESSED com useBackgroundColor true aplica cor branca`() {
        val view = RetroCardView(context)

        view.setState(RetroCardView.State.PRESSED)

        assertEquals(RetroCardView.State.PRESSED, view.getState())
        assertEquals(RetroCardView.COLOR_PRESSED, backgroundColorOf(view))
    }

    @Test
    fun `setState para PRESSED com useBackgroundColor false mantem fundo transparente`() {
        val view = RetroCardView(context)
        view.setUseBackgroundColor(false)

        view.setState(RetroCardView.State.PRESSED)

        assertEquals(Color.TRANSPARENT, backgroundColorOf(view))
    }

    @Test
    fun `setState de volta para NORMAL restaura fundo transparente independente de useBackgroundColor`() {
        val view = RetroCardView(context)
        view.setState(RetroCardView.State.SELECTED)
        assertEquals(RetroCardView.COLOR_SELECTED, backgroundColorOf(view))

        view.setState(RetroCardView.State.NORMAL)

        assertEquals(RetroCardView.State.NORMAL, view.getState())
        assertEquals(Color.TRANSPARENT, backgroundColorOf(view))
    }

    @Test
    fun `setState com o mesmo estado atual e um no-op mas nao lanca excecao`() {
        val view = RetroCardView(context)
        view.setState(RetroCardView.State.SELECTED)

        try {
            view.setState(RetroCardView.State.SELECTED)
        } catch (e: Exception) {
            fail("Re-setting the same state should not throw: ${e.message}")
        }

        assertEquals(RetroCardView.State.SELECTED, view.getState())
        assertEquals(RetroCardView.COLOR_SELECTED, backgroundColorOf(view))
    }

    @Test
    fun `setUseBackgroundColor reaplica o visual imediatamente para o estado atual`() {
        val view = RetroCardView(context)
        view.setState(RetroCardView.State.SELECTED)
        assertEquals(RetroCardView.COLOR_SELECTED, backgroundColorOf(view))

        // Flip the flag while already SELECTED: visual must update immediately, without a
        // separate setState() call.
        view.setUseBackgroundColor(false)
        assertEquals(Color.TRANSPARENT, backgroundColorOf(view))

        view.setUseBackgroundColor(true)
        assertEquals(RetroCardView.COLOR_SELECTED, backgroundColorOf(view))
    }

    @Test
    fun `setPressed(true) move para o estado PRESSED e setPressed(false) volta para NORMAL`() {
        // Documents current coupling between the View framework's pressed-state callback and
        // RetroCardView's own State machine: a normal touch down+up sequence always resets a
        // SELECTED card back to NORMAL (transparent) via this override, since PRESSED->released
        // hardcodes NORMAL rather than restoring whatever state preceded the press.
        val view = RetroCardView(context)
        view.setState(RetroCardView.State.SELECTED)

        view.isPressed = true
        assertEquals(RetroCardView.State.PRESSED, view.getState())
        assertEquals(RetroCardView.COLOR_PRESSED, backgroundColorOf(view))

        view.isPressed = false
        assertEquals(RetroCardView.State.NORMAL, view.getState())
        assertEquals(Color.TRANSPARENT, backgroundColorOf(view))
    }

    @Test
    fun `addView usa generateDefaultLayoutParams quando nenhum LayoutParams e fornecido`() {
        // generateDefaultLayoutParams()/generateLayoutParams(LayoutParams) are `protected`
        // (inherited from ViewGroup), so they're exercised indirectly through addView() rather
        // than called directly.
        val view = RetroCardView(context)
        val child = android.view.View(context)

        view.addView(child)

        assertTrue(child.layoutParams is LinearLayout.LayoutParams)
        assertEquals(LinearLayout.LayoutParams.WRAP_CONTENT, child.layoutParams.width)
        assertEquals(LinearLayout.LayoutParams.WRAP_CONTENT, child.layoutParams.height)
    }

    @Test
    fun `addView converte LayoutParams de outro tipo de ViewGroup preservando largura e altura`() {
        val view = RetroCardView(context)
        val child = android.view.View(context)
        val foreignParams = android.widget.FrameLayout.LayoutParams(123, 456)

        // FrameLayout.LayoutParams fails LinearLayout's checkLayoutParams(), which forces
        // ViewGroup to call generateLayoutParams(ViewGroup.LayoutParams) to convert it.
        view.addView(child, foreignParams)

        val params = child.layoutParams
        assertTrue(params is LinearLayout.LayoutParams)
        assertEquals(123, params.width)
        assertEquals(456, params.height)
    }
}
