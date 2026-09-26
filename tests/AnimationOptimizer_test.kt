package com.vinaooo.revenger.utils

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [AnimationOptimizer.animateViewsBatchOptimized] is what the main menu's animate-in/out runs
 * (through [ViewUtils.animateMenuViewsBatchOptimized]); `animateMenuOut`'s callback is what removes
 * the menu, so "onEnd fires exactly once, after every view finished" is the contract pinned here.
 * The animations run on Robolectric's paused main looper: nothing has run right after the call,
 * and [advance] runs them to the end. (Mid-animation times aren't asserted; Robolectric's frame
 * clock doesn't stop exactly at the requested time.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnimationOptimizer_test {

    private lateinit var views: Array<View>
    private var endCalls = 0

    @Before
    fun setUp() {
        // Attached to a real window: ViewPropertyAnimator ends a detached view's animation at once.
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity).also(activity::setContentView)
        views = Array(3) { View(activity).also(parent::addView) }
        endCalls = 0
    }

    private fun advance(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun `todas as views chegam ao alvo e onEnd roda uma vez so no fim`() {
        AnimationOptimizer.animateViewsBatchOptimized(views, toAlpha = 0f, toScale = 0.8f, duration = 150) {
            endCalls++
        }

        assertEquals(0, endCalls)
        views.forEach { assertEquals(View.LAYER_TYPE_HARDWARE, it.layerType) }

        advance(500)
        assertEquals(1, endCalls)
        views.forEach { view ->
            assertEquals(0f, view.alpha)
            assertEquals(0.8f, view.scaleX)
            assertEquals(0.8f, view.scaleY)
            assertEquals(View.LAYER_TYPE_NONE, view.layerType)
        }
    }

    @Test
    fun `sem callback as animacoes terminam normalmente`() {
        views.forEach { it.alpha = 0f }

        AnimationOptimizer.animateViewsBatchOptimized(views, toAlpha = 1f, toScale = 1f)
        advance(500)

        views.forEach { view ->
            assertEquals(1f, view.alpha)
            assertEquals(View.LAYER_TYPE_NONE, view.layerType)
        }
    }

    @Test
    fun `cancelar uma view devolve a camada e ainda conta como fim`() {
        AnimationOptimizer.animateViewsBatchOptimized(views, toAlpha = 0f, toScale = 1f, duration = 150) {
            endCalls++
        }

        views[1].animate().cancel()
        assertEquals(View.LAYER_TYPE_NONE, views[1].layerType)
        assertEquals(0, endCalls)

        advance(500)
        assertEquals(1, endCalls)
    }

    @Test
    fun `ViewUtils delega a animacao em lote para o AnimationOptimizer`() {
        ViewUtils.animateMenuViewsBatchOptimized(arrayOf(views[0]), toAlpha = 0f, toScale = 0.5f) { endCalls++ }
        advance(500)

        assertEquals(1, endCalls)
        assertEquals(0f, views[0].alpha)
        assertEquals(0.5f, views[0].scaleX)
    }
}
