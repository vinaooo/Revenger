package com.vinaooo.revenger.ui.splash

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the CRT boot view shows at each point of its animation, sampled at a few pixels with
 * Robolectric's native graphics. Under Robolectric a `ValueAnimator` jumps straight to its end on
 * the first frame, so the animation is stepped with `setCurrentFraction`, which runs the view's own
 * update listener. Only brightness is checked (lit, dark, black), not exact colors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CRTBootViewDrawing_test {

    private val view = CRTBootView(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, WIDTH, HEIGHT)
    }

    private fun animator(): ValueAnimator =
            CRTBootView::class.java.getDeclaredField("animator").apply { isAccessible = true }.get(view) as ValueAnimator

    /** Draws the view as it is at [fraction] of the running animation. */
    private fun drawAt(fraction: Float): Bitmap {
        animator().setCurrentFraction(fraction)
        return draw()
    }

    private fun draw(): Bitmap =
            Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }

    private fun Bitmap.brightness(x: Int, y: Int) = Color.red(getPixel(x, y))

    @Test
    fun `antes de comecar a tela e toda preta`() {
        val bitmap = draw()

        assertEquals(0, bitmap.brightness(CENTER_X, CENTER_Y))
        assertEquals(Color.BLACK, bitmap.getPixel(CENTER_X, CENTER_Y))
    }

    @Test
    fun `a animacao de ligar mostra o ponto, a linha e a tela abrindo com scanlines`() {
        view.startAnimation()

        val dot = drawAt(0.1f)
        assertTrue(dot.brightness(CENTER_X, CENTER_Y) > LIT)
        assertEquals(0, dot.brightness(LINE_X, CENTER_Y))

        val line = drawAt(0.45f)
        assertTrue(line.brightness(LINE_X, CENTER_Y) > LIT)
        assertTrue(line.brightness(LINE_X, CENTER_Y + 15) < DARK)
        assertEquals(0, line.brightness(CENTER_X, UPPER_Y))

        val expansion = drawAt(0.75f)
        assertTrue(expansion.brightness(CENTER_X, UPPER_Y) > 0)
        assertEquals(Color.BLACK, expansion.getPixel(2, 2)) // rounded corner stays black
        assertNotEquals(expansion.brightness(CENTER_X, UPPER_Y), expansion.brightness(CENTER_X, UPPER_Y + 1)) // scanlines
    }

    @Test
    fun `ligar termina apagando e desligar comeca com a tela acesa`() {
        view.startAnimation()
        val bootEnd = drawAt(0.95f)

        view.startReverseAnimation()
        val shutdownStart = drawAt(0.1f)

        assertTrue(bootEnd.brightness(CENTER_X, CENTER_Y) < DARK)
        assertTrue(shutdownStart.brightness(CENTER_X, CENTER_Y) > LIT)
    }

    @Test
    fun `cada passo da animacao redesenha a view`() {
        view.startAnimation()
        shadowOf(view).clearWasInvalidated()

        animator().setCurrentFraction(0.5f)

        assertTrue(shadowOf(view).wasInvalidated())
    }

    private companion object {
        const val WIDTH = 200
        const val HEIGHT = 120
        const val CENTER_X = WIDTH / 2
        const val CENTER_Y = HEIGHT / 2
        const val LINE_X = 160
        const val UPPER_Y = 30
        const val LIT = 0x80
        const val DARK = 0x40
    }
}
