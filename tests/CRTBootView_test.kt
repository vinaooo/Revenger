package com.vinaooo.revenger.ui.splash

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CRTBootView_test {

    private val view = CRTBootView(ApplicationProvider.getApplicationContext())
    private var endCount = 0

    init {
        view.onAnimationEndListener = { endCount++ }
    }

    private fun runAnimations() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(CRTBootView.ANIMATION_DURATION * 2))
    }

    @Test
    fun `boot animation fires the end listener once when it finishes`() {
        view.startAnimation()
        runAnimations()

        assertEquals(1, endCount)
    }

    @Test
    fun `stopping the animation does not fire the end listener`() {
        view.startAnimation()

        view.stopAnimation()
        runAnimations()

        assertEquals(0, endCount)
    }

    @Test
    fun `restarting fires the end listener only for the animation that finished`() {
        view.startReverseAnimation()
        view.startReverseAnimation()
        runAnimations()

        assertEquals(1, endCount)
    }

    @Test
    fun `shutdown animation fires the end listener once when it finishes`() {
        view.startReverseAnimation()
        runAnimations()

        assertEquals(1, endCount)
    }

    @Test
    fun `stopping without starting does nothing`() {
        view.stopAnimation()
        runAnimations()

        assertEquals(0, endCount)
    }

    @Test
    fun `detaching from the window stops the animation without firing`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setContentView(view)
        view.startAnimation()

        (view.parent as ViewGroup).removeView(view)
        runAnimations()

        assertEquals(0, endCount)
    }

    @Test
    fun `playShutdown shows the view and calls onComplete once`() {
        view.visibility = View.GONE
        var completed = 0

        view.playShutdown { completed++ }
        assertEquals(View.VISIBLE, view.visibility)
        runAnimations()

        assertEquals(1, completed)
        assertEquals("the previous listener is replaced", 0, endCount)
    }

    @Test
    fun `playShutdown twice completes only the second run`() {
        var first = 0
        var second = 0

        view.playShutdown { first++ }
        view.playShutdown { second++ }
        runAnimations()

        assertEquals(0, first)
        assertEquals(1, second)
    }

    /** Draws each phase, both directions, into a bitmap: the drawing code must not throw. */
    @Test
    fun `draws every phase in both directions`() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, WIDTH, HEIGHT)
        val canvas = Canvas(Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888))
        val looper = shadowOf(Looper.getMainLooper())

        view.draw(canvas) // not started: black only
        for (start in listOf({ view.startAnimation() }, { view.startReverseAnimation() })) {
            start()
            repeat(STEPS) {
                looper.idleFor(Duration.ofMillis(CRTBootView.ANIMATION_DURATION / STEPS))
                view.draw(canvas)
            }
            runAnimations()
        }

        assertEquals(2, endCount)
    }

    private companion object {
        const val WIDTH = 200
        const val HEIGHT = 120
        const val STEPS = 12
    }
}
