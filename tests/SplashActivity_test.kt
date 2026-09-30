package com.vinaooo.revenger.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import com.vinaooo.revenger.utils.OrientationManager
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import com.vinaooo.revenger.R
import java.time.Duration
import com.vinaooo.revenger.ui.splash.CRTBootView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SplashActivity_test {

    private val controller = Robolectric.buildActivity(SplashActivity::class.java).setup()
    private val activity = controller.get()
    private val crtView: CRTBootView = activity.findViewById(R.id.crt_boot_view)

    @Test
    fun `the end of the boot animation starts GameActivity and finishes the splash`() {
        crtView.onAnimationEndListener!!.invoke()

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(GameActivity::class.java.name, started.component?.className)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `nothing starts before the boot animation ends`() {
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(shadowOf(activity).nextStartedActivity)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `back is blocked during the splash`() {
        activity.onBackPressedDispatcher.onBackPressed()

        assertFalse(activity.isFinishing)
    }

    @Test
    fun `the boot animation end listener is wired`() {
        assertNotNull(crtView.onAnimationEndListener)
    }

    /** Regression: stopping the animation in onDestroy used to fire the end listener. */
    @Test
    fun `destroying mid-animation does not start GameActivity`() {
        crtView.startAnimation()

        controller.pause().stop().destroy()
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `a configuration change keeps the splash running`() {
        activity.onConfigurationChanged(activity.resources.configuration)

        assertFalse(activity.isFinishing)
    }

    private fun animationStarted(): Boolean =
            CRTBootView::class.java.getDeclaredField("isAnimationStarted").apply { isAccessible = true }
                    .getBoolean(crtView)

    @Test
    fun `the boot animation starts when the native splash leaves`() {
        controller.visible()
        assertFalse(animationStarted())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        activity.window.decorView.viewTreeObserver.dispatchOnPreDraw()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(animationStarted())
    }

    @Test
    fun `the window background is black`() {
        assertEquals(Color.BLACK, (activity.window.decorView.background as ColorDrawable).color)
    }

    @Test
    fun `the configured orientation is applied on create and after each configuration change`() {
        mockkObject(OrientationManager)
        try {
            val other = Robolectric.buildActivity(SplashActivity::class.java).setup().get()
            verify(exactly = 1) { OrientationManager.applyConfigOrientation(other, any()) }

            other.onConfigurationChanged(other.resources.configuration)

            verify(exactly = 2) { OrientationManager.applyConfigOrientation(other, any()) }
        } finally {
            unmockkObject(OrientationManager)
        }
    }

    @Test
    fun `destroying before the native splash is released cancels the pending release`() {
        controller.pause().stop().destroy()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        assertFalse(isReady())
    }

    private fun isReady(): Boolean =
            SplashActivity::class.java.getDeclaredField("isReady").apply { isAccessible = true }.getBoolean(activity)

    @Test
    fun `the native splash is released once the layout has had time to settle`() {
        assertFalse(isReady())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

        assertTrue(isReady())
    }
}
