package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.managers.SessionSlotTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [GlowAnimationController], extracted from [SaveStateGridFragment] to keep that
 * fragment's function count within detekt's `TooManyFunctions` threshold. Focuses on the
 * visibility/alpha bookkeeping this class owns directly (not animator-timing internals, which
 * depend on Robolectric's animation frame handling).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GlowAnimationController_test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        SessionSlotTracker.clearInstance()
    }

    @After
    fun cleanup() {
        SessionSlotTracker.clearInstance()
    }

    @Test
    fun `apply shows the glow view when last-used and not selected`() {
        val controller = GlowAnimationController()
        val glowView = View(context)

        controller.apply(glowView, isLastUsed = true, isSelected = false)

        assertEquals(View.VISIBLE, glowView.visibility)
    }

    @Test
    fun `apply hides the glow view when selected even if last-used`() {
        val controller = GlowAnimationController()
        val glowView = View(context)

        controller.apply(glowView, isLastUsed = true, isSelected = true)

        assertEquals(View.GONE, glowView.visibility)
        assertEquals(1.0f, glowView.alpha)
    }

    @Test
    fun `apply hides the glow view when not last-used`() {
        val controller = GlowAnimationController()
        val glowView = View(context)

        controller.apply(glowView, isLastUsed = false, isSelected = false)

        assertEquals(View.GONE, glowView.visibility)
        assertEquals(1.0f, glowView.alpha)
    }

    @Test
    fun `apply resets a previously glowing view's alpha to 1 when it stops being the last-used slot`() {
        val controller = GlowAnimationController()
        val glowView = View(context)
        controller.apply(glowView, isLastUsed = true, isSelected = false)
        glowView.alpha = 0.5f // simulate a mid-pulse animation frame

        controller.apply(glowView, isLastUsed = false, isSelected = false)

        assertEquals(1.0f, glowView.alpha)
        assertEquals(View.GONE, glowView.visibility)
    }

    @Test
    fun `apply resets the previous glowing view's alpha before animating a newly last-used view`() {
        val controller = GlowAnimationController()
        val previousGlowView = View(context)
        val newGlowView = View(context)
        controller.apply(previousGlowView, isLastUsed = true, isSelected = false)
        previousGlowView.alpha = 0.5f // simulate a mid-pulse animation frame

        controller.apply(newGlowView, isLastUsed = true, isSelected = false)

        assertEquals(1.0f, previousGlowView.alpha)
        assertEquals(View.VISIBLE, newGlowView.visibility)
    }

    @Test
    fun `stop does not throw when nothing is animating`() {
        val controller = GlowAnimationController()

        controller.stop()
    }

    @Test
    fun `refreshIfLastUsedSlotValid does not invoke callback when there is no last-used slot`() {
        val controller = GlowAnimationController()
        var invoked = false

        controller.refreshIfLastUsedSlotValid { invoked = true }

        assertFalse(invoked)
    }

    @Test
    fun `refreshIfLastUsedSlotValid invokes callback when the last-used slot is valid`() {
        SessionSlotTracker.getInstance().recordSave(1)
        val controller = GlowAnimationController()
        var invoked = false

        controller.refreshIfLastUsedSlotValid { invoked = true }

        assertTrue(invoked)
    }
}
